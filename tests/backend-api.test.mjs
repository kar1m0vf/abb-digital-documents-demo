import test from 'node:test';
import assert from 'node:assert/strict';
import {createBackendApi} from '../js/services/backend-api.js';

const customer={customerId:101,fullName:'Aydan Əhədova',phoneMasked:'+994 50 *** ** 00'};
const reply=(data,status=200)=>({ok:status<400,status,json:async()=>data});
function fixture(handler=()=>reply({accounts:[]})){
  const calls=[];
  const api=createBackendApi({fetchImpl:async(url,options)=>{
    calls.push({url,...options});
    if(url.endsWith('/fin/verify'))return reply(customer);
    if(url.endsWith('/otp/send'))return reply({sentTo:customer.phoneMasked,expiresInSeconds:60});
    if(url.endsWith('/otp/validate'))return reply({accessToken:'test-token'});
    return handler(url,options);
  }});
  return {api,calls};
}
async function login(api){const challenge=await api.requestOtp('ABC1234');return api.verifyOtp(challenge.id,'123456');}
test('account retry does not consume the same OTP a second time',async()=>{
  let count=0;
  const {api,calls}=fixture(()=>{if(count++===0)throw new TypeError('offline');return reply({accounts:[]});});
  const challenge=await api.requestOtp('ABC1234');
  await assert.rejects(api.verifyOtp(challenge.id,'123456'),/Serverə/);
  assert.equal((await api.verifyOtp(challenge.id,'123456')).name,customer.fullName);
  assert.equal(calls.filter(c=>c.url.endsWith('/otp/validate')).length,1);
  assert.equal(calls.at(-1).headers.Authorization,'Bearer test-token');
  await assert.rejects(api.verifyOtp(challenge.id,'123456'),/müddəti/);
});
test('concurrent payments use one request; a rejected payment can be retried',async()=>{
  let payments=0;
  const {api,calls}=fixture((url,options)=>{
    if(url.endsWith('/accounts'))return reply({accounts:[]});
    payments++;
    return JSON.parse(options.body).paymentToken==='demo-declined'?reply({code:'PAYMENT_FAILED'},400):reply({id:'AR-2026-000001'});
  });
  await login(api);
  await assert.rejects(api.submitOrder({reviewed:true},'demo-declined','same-key'),/rədd/);
  const [a,b]=await Promise.all([api.submitOrder({reviewed:true},'demo-success','same-key'),api.submitOrder({reviewed:true},'demo-success','same-key')]);
  assert.deepEqual(a,b);assert.equal(payments,2);
  assert.doesNotMatch(calls.at(-1).body,/cvv|4242|cardNumber/);
  api.logout();await assert.rejects(api.submitOrder({},'demo-success','next-key'),/Sessiya/);
});
test('server outage never creates a successful local payment',async()=>{
  const {api}=fixture(url=>{if(url.endsWith('/accounts'))return reply({accounts:[]});throw new TypeError('offline');});
  await login(api);await assert.rejects(api.submitOrder({},'demo-success','key'),/Serverə/);
});
test('a stale list response cannot overwrite a newer status',async()=>{
  let finishList,loaded=false,updated=false;
  const api=createBackendApi({onOrders:()=>{loaded=true;},onOrder:()=>{updated=true;},fetchImpl:async(url,options)=>options.method==='PUT'?reply({id:'AR-2026-000001',status:'completed'}):new Promise(resolve=>{finishList=()=>resolve(reply([]));})});
  const list=api.refreshOrders();await api.updateStatus('AR-2026-000001','completed');finishList();await list;
  assert.equal(updated,true);assert.equal(loaded,false);
});
