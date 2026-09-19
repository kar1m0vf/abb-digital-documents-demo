import test from 'node:test';
import assert from 'node:assert/strict';
import {validCard,validExpiry,validRange,periodRange,cleanCode} from '../js/utils.js';
import {requestOtp,verifyOtp,submitOrder} from '../js/services/api.js';

test('card and calendar boundaries',()=>{
  assert.equal(validCard('4242 4242 4242 4242'),true);
  assert.equal(validCard('4242 4242 4242 4241'),false);
  assert.equal(validExpiry('09/26',new Date('2026-09-30T12:00:00Z')),true);
  assert.equal(validExpiry('09/26',new Date('2026-10-01T12:00:00Z')),false);
  assert.equal(validRange('2026-09-01','2026-09-19','2026-09-19'),true);
  assert.equal(validRange('2026-09-20','2026-09-19','2026-09-19'),false);
  assert.equal(validRange('2026-02-30','2026-03-01','2026-09-19'),false);
  assert.deepEqual(periodRange({period:'1'},'2026-03-31'),['2026-02-28','2026-03-31']);
  assert.equal(cleanCode('ab c-1234'),'ABC1234');
});
test('OTP rejects wrong code, accepts demo code, and cannot be replayed',async()=>{
  const c=await requestOtp('ABC1234');
  await assert.rejects(()=>verifyOtp(c.id,'999999'),/Kod düzgün deyil/);
  assert.equal((await verifyOtp(c.id,'123456')).name,'Aydan Əhədova');
  await assert.rejects(()=>verifyOtp(c.id,'123456'),/Kodun müddəti bitib/);
});
test('concurrent submissions are idempotent and declined payments can be retried',async()=>{
  const draft={type:'reference',accounts:['visa-azn'],reviewed:true};
  const key=crypto.randomUUID();
  const [a,b]=await Promise.all([submitOrder(draft,'demo-success',key),submitOrder(draft,'demo-success',key)]);
  assert.equal(a.id,b.id);assert.equal(a.price,10);
  const retry=crypto.randomUUID();
  await assert.rejects(()=>submitOrder(draft,'demo-declined',retry),/rədd edildi/);
  assert.equal((await submitOrder(draft,'demo-success',retry)).paymentStatus,'paid');
});
