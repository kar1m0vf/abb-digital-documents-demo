import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {createAppServer} from '../server.mjs';
async function listen(server,t){await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));});return `http://127.0.0.1:${server.address().port}`;}
test('standalone server serves the UI but hides backend sources and private files',async t=>{
  const url=await listen(createAppServer(),t);
  assert.equal((await fetch(url)).status,200);
  assert.match(await (await fetch(url+'/js/config.js')).text(),/:"demo";/);
  for(const file of ['/backend/application/build.gradle.kts','/.git/config','/js/../backend/data/presentation.mv.db','/package.json'])assert.equal((await fetch(url+file)).status,404,file);
  assert.equal((await fetch(url+'/api/v1/demo/orders')).status,404);
});
test('same-origin proxy forwards method, body and bearer token',async t=>{
  let received;
  const upstream=await listen(http.createServer(async(req,res)=>{let body='';for await(const chunk of req)body+=chunk;received={method:req.method,url:req.url,authorization:req.headers.authorization,body};res.writeHead(201,{'Content-Type':'application/json'});res.end('{"id":"test"}');}),t);
  const url=await listen(createAppServer({backendUrl:upstream}),t);
  assert.match(await (await fetch(url+'/js/config.js')).text(),/:"backend";/);
  const response=await fetch(url+'/api/v1/demo/orders',{method:'POST',headers:{Authorization:'Bearer sample','Content-Type':'application/json'},body:'{"test":true}'});
  assert.equal(response.status,201);assert.deepEqual(await response.json(),{id:'test'});
  assert.deepEqual(received,{method:'POST',url:'/api/v1/demo/orders',authorization:'Bearer sample',body:'{"test":true}'});
});
test('unavailable backend produces an error, not a demo success',async t=>{
  const url=await listen(createAppServer({backendUrl:'http://127.0.0.1:1'}),t);
  const response=await fetch(url+'/api/v1/demo/orders');assert.equal(response.status,502);assert.equal((await response.json()).code,'BACKEND_UNAVAILABLE');
});
