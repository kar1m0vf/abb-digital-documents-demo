import {spawn,spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {existsSync,readdirSync} from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import {setTimeout as delay} from 'node:timers/promises';

const root=fileURLToPath(new URL('../',import.meta.url));
const backend=path.join(root,'backend');
const backendPort=Number(process.env.BACKEND_PORT||8080),frontendPort=Number(process.env.PORT||4173);
for(const port of [backendPort,frontendPort]){
  await new Promise((resolve,reject)=>{const probe=net.createServer();probe.once('error',()=>reject(new Error(`Port ${port} is already in use. Stop the previous dev server or set PORT / BACKEND_PORT.`)));probe.listen(port,'127.0.0.1',()=>probe.close(resolve));});
}
const javaBin=process.platform==='win32'?'java.exe':'java',javacBin=process.platform==='win32'?'javac.exe':'javac';
// Keep in sync with the toolchain fallback in backend/build.gradle.kts.
const toolchainVersion=21;
function javaVersion(exe){const r=spawnSync(exe,['-version'],{encoding:'utf8',windowsHide:true});return r.error?'':(r.stderr||r.stdout||'').match(/version "([\d._]+)/)?.[1]||'';}
function javaMajor(exe){return Number(javaVersion(exe).split('.')[0]||0);}
// Gradle needs a real JDK (bin/javac) to satisfy the toolchain, so a JRE on PATH is not enough.
function jdkHomes(){
  if(process.env.JAVA_HOME)return [process.env.JAVA_HOME];
  const parents=process.platform==='win32'?[]
    :['/usr/lib/jvm','/usr/java','/Library/Java/JavaVirtualMachines',path.join(process.env.HOME||'', '.jdks'),path.join(process.env.HOME||'', '.local/jvm')];
  const homes=[];
  for(const parent of parents){try{for(const entry of readdirSync(parent))homes.push(path.join(parent,entry));}catch{}}
  return homes;
}
const candidates=jdkHomes()
  .filter(home=>existsSync(path.join(home,'bin',javacBin)))
  .map(home=>({home,version:javaVersion(path.join(home,'bin',javaBin))}))
  .filter(c=>Number(c.version.split('.')[0]||0)>=toolchainVersion)
  // Gradle resolves the toolchain by exact major version, so prefer a match, and
  // within it the newest patch release. Newer JDKs only as a last resort.
  .sort((a,b)=>((a.version.split('.')[0]===String(toolchainVersion)?0:1)-(b.version.split('.')[0]===String(toolchainVersion)?0:1))
    ||(b.version.localeCompare(a.version,undefined,{numeric:true})));
const jdkHome=candidates[0]?.home;
if(!jdkHome){
  const current=process.env.JAVA_HOME??'java (on PATH)';
  console.error(`Java JDK ${toolchainVersion}+ is required, but no JDK was found (checked ${current}).
A Java runtime is not enough: Gradle needs bin/javac for the toolchain.
Install a JDK and try again, e.g.:
  sudo apt install openjdk-${toolchainVersion}-jdk
or point JAVA_HOME at an existing JDK ${toolchainVersion} installation.`);
  process.exit(1);
}
const java=path.join(jdkHome,'bin',javaBin);
console.log(`Using JDK ${javaMajor(java)} at ${jdkHome}`);
const env={...process.env,JAVA_HOME:jdkHome,GRADLE_USER_HOME:path.join(backend,'.gradle-cache')};
const children=new Set();let stopping=false;
function launch(command,args,options={}){
  const child=spawn(command,args,{cwd:backend,env,stdio:'inherit',windowsHide:true,...options});children.add(child);child.once('exit',()=>children.delete(child));child.once('error',error=>{console.error(error.message);stop(1);});return child;
}
function stopChild(child){
  if(!child.pid||child.exitCode!==null)return;
  if(process.platform==='win32'){
    const taskkill=process.env.SystemRoot?path.join(process.env.SystemRoot,'System32','taskkill.exe'):'taskkill.exe';
    const result=spawnSync(taskkill,['/pid',String(child.pid),'/T','/F'],{stdio:'ignore',windowsHide:true});
    if(result.status!==0)child.kill();
    return;
  }
  child.kill('SIGTERM');
}
function stop(code=0){if(stopping)return;stopping=true;for(const child of children)stopChild(child);process.exitCode=code;}
process.on('SIGINT',()=>stop());process.on('SIGTERM',()=>stop());
console.log('Building the Spring backend…');
const build=launch(java,['-jar','gradle/wrapper/gradle-wrapper.jar',':application:bootJar','--console=plain']);
const buildCode=await new Promise(resolve=>{build.once('exit',resolve);build.once('error',()=>resolve(1));});
if(buildCode!==0||stopping){stop(1);}else{
  const port=backendPort;
  const backendUrl=`http://127.0.0.1:${port}`;
  const app=launch(java,['-jar','application/build/libs/application-0.0.1-SNAPSHOT.jar','--spring.profiles.active=presentation','--debug=false',`--server.port=${port}`]);
  app.once('exit',()=>{if(!stopping)stop(1);});
  let ready=false;
  for(let attempt=0;attempt<120&&!stopping;attempt++){
    try{const result=await fetch(`${backendUrl}/api/v1/demo/health`,{signal:AbortSignal.timeout(1000)});if(result.ok&&(await result.json()).mode==='presentation'){ready=true;break;}}catch{}
    await delay(500);
  }
  if(!ready){console.error('Backend did not start. Check the Java output above.');stop(1);}else if(!stopping){
    const frontend=launch(process.execPath,['server.mjs'],{cwd:root,env:{...process.env,BACKEND_URL:backendUrl}});
    frontend.once('exit',code=>{if(!stopping)stop(code||0);});
  }
}
