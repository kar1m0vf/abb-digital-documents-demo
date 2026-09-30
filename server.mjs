import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('.', import.meta.url));
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpeg': 'image/jpeg', '.json': 'application/json', '.woff2': 'font/woff2' };
export function createAppServer({backendUrl=''}={}) {
  const backend=backendUrl?new URL(backendUrl):null;
  if(backend&&!['http:','https:'].includes(backend.protocol))throw new Error('BACKEND_URL must use HTTP or HTTPS.');
  return http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    const pathname = decodeURIComponent(url.pathname);
    if(pathname==='/js/config.js'){
      const mode=backend?'backend':'demo';
      res.writeHead(200,{'Content-Type':'text/javascript; charset=utf-8','Cache-Control':'no-store'});
      res.end(`const mode=new URLSearchParams(globalThis.location?.search||'').get('mode')==='demo'?'demo':${JSON.stringify(mode)};export const config=Object.freeze({mode,apiBase:'/api/v1'});`);return;
    }
    if(pathname.startsWith('/api/')){
      if(!backend||!pathname.startsWith('/api/v1/')||!['GET','POST','PUT','PATCH'].includes(req.method)){res.writeHead(404,{'Content-Type':'application/json'});res.end(JSON.stringify({message:'API is not enabled.'}));return;}
      const chunks=[];let size=0;
      for await(const chunk of req){size+=chunk.length;if(size>65536){res.writeHead(413,{'Content-Type':'application/json'});res.end(JSON.stringify({message:'Request too large.'}));return;}chunks.push(chunk);}
      try{
        const upstream=await fetch(new URL(url.pathname+url.search,backend),{method:req.method,headers:{'Content-Type':'application/json',Accept:'application/json',...(req.headers.authorization?{Authorization:req.headers.authorization}:{}),'Accept-Language':req.headers['accept-language']||'az'},...(chunks.length?{body:Buffer.concat(chunks)}:{}),signal:AbortSignal.timeout(10000),redirect:'error'});
        res.writeHead(upstream.status,{'Content-Type':upstream.headers.get('content-type')||'application/json','Cache-Control':'no-store'});res.end(Buffer.from(await upstream.arrayBuffer()));
      }catch{res.writeHead(502,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(JSON.stringify({code:'BACKEND_UNAVAILABLE',message:'Serverə qoşulmaq mümkün olmadı. Yenidən cəhd edin.'}));}
      return;
    }
    // Only browser assets are public. Never serve Java sources, databases or build caches.
    if(!['GET','HEAD'].includes(req.method)||!(pathname==='/'||pathname==='/index.html'||/^\/(?:js|css|assets|vendor)\//.test(pathname))||pathname.split(/[\\/]/).some(part=>part.startsWith('.')))throw new Error('Not found');
    const filename = path.resolve(root, '.' + (pathname === '/' ? '/index.html' : pathname));
    const relative=path.relative(root,filename);
    if (relative.startsWith('..')||path.isAbsolute(relative)||!(await stat(filename)).isFile()) throw new Error('Not found');
    const data = await readFile(filename);
    res.writeHead(200, { 'Content-Type': types[path.extname(filename)] || 'application/octet-stream', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'X-Robots-Tag': 'noindex, nofollow' });
    res.end(req.method==='HEAD'?undefined:data);
  } catch { res.writeHead(404, { 'Content-Type': 'text/plain' }); res.end('Not found'); }
  });
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const port=Number(process.env.PORT||4173);
  createAppServer({backendUrl:process.env.BACKEND_URL||''}).listen(port,process.env.HOST||'127.0.0.1',()=>console.log(`ABB prototype: http://localhost:${port} (${process.env.BACKEND_URL?'backend':'standalone demo'})`));
}
