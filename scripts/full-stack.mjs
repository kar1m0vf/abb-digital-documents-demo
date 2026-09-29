import {spawn,spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import net from 'node:net';
import {setTimeout as delay} from 'node:timers/promises';

const root=fileURLToPath(new URL('../',import.meta.url));
const backend=path.join(root,'backend');
const backendPort=Number(process.env.BACKEND_PORT||8080),frontendPort=Number(process.env.PORT||4173);
for(const port of [backendPort,frontendPort]){
  await new Promise((resolve,reject)=>{const probe=net.createServer();probe.once('error',()=>reject(new Error(`Port ${port} is already in use. Stop the previous dev server or set PORT / BACKEND_PORT.`)));probe.listen(port,'127.0.0.1',()=>probe.close(resolve));});
}
const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java'):'java';
const version=spawnSync(java,['-version'],{encoding:'utf8',windowsHide:true});
const major=Number((version.stderr||version.stdout||'').match(/version "(\d+)/)?.[1]);
if(version.error||!major||major<21){console.error('Java JDK 21 or newer is required. Set JAVA_HOME to the installed JDK.');process.exit(1);}
const env={...process.env,GRADLE_USER_HOME:path.join(backend,'.gradle-cache')};
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
const build=launch(java,['-jar','gradle/wrapper/gradle-wrapper.jar',`-PjavaVersion=${major}`,':application:bootJar','--console=plain']);
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
