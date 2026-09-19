import test from 'node:test';
import assert from 'node:assert/strict';
import {state,newDraft,accountDetail,getOrders,saveOrder,getInquiries,updateStatus} from '../js/store.js';
import {documentType,embassy,fin,otp} from '../js/views/identity.js';
import {accountSelection,details} from '../js/views/accounts.js';
import {review,payment,confirmation} from '../js/views/checkout.js';
import {documentHtml} from '../js/views/document.js';
import {dashboard,filteredRows} from '../js/views/dashboard.js';
import {transactionsFor} from '../js/services/transactions.js';

const storage=new Map();
Object.defineProperty(globalThis,'localStorage',{value:{getItem:k=>storage.get(k)??null,setItem:(k,v)=>storage.set(k,v)},configurable:true});
test('all wizard screens render for both document types',()=>{
  for(const type of ['statement','reference']){
    state.draft={...newDraft(),type,accounts:['visa-azn','account-eur'],reviewed:true};
    accountDetail('visa-azn');accountDetail('account-eur');
    state.order={...structuredClone(state.draft),id:'AR-2026-12345678',date:'2026-09-19T12:00:00Z',customer:'Aydan Əhədova',price:10,status:'pending'};
    for(const view of [documentType,embassy,fin,otp,accountSelection,details,review,payment,confirmation]){
      const html=view();assert.ok(html.length>100,view.name);assert.doesNotMatch(html,/\b(?:undefined|NaN)\b/,view.name);
      assert.doesNotMatch(html,/demo|prototip|sample|nümunə|ABC1234|\b123456\b|4242 4242|test kart|\p{Extended_Pictographic}|\p{Regional_Indicator}/iu,view.name);
    }
    const doc=documentHtml(state.order);assert.match(doc,/Account (?:statement|balance reference)/);assert.match(doc,/7575/);assert.match(doc,/6241/);
    if(type==='statement')assert.match(doc,/Card purchase/);else assert.doesNotMatch(doc,/Card purchase/);
  }
});
test('saved orders appear in dashboard and share status with customer orders',()=>{
  saveOrder({...state.order,destination:'embassy'});saveOrder(state.order);
  assert.equal(getOrders().length,1);state.embassy='italy';state.filter='pending';state.query='12345678';
  assert.equal(filteredRows().length,1);assert.equal(updateStatus(state.order.id,'completed'),true);
  assert.equal(filteredRows().length,0);assert.equal(getInquiries().find(o=>o.id===state.order.id).status,'completed');
  state.query='nothing matches';assert.match(dashboard(),/No inquiries found/);
  state.query='';state.filter='all';
});
test('document escapes free text and supports Azerbaijani',()=>{
  const doc=documentHtml({...state.order,language:'az',destination:'other',recipient:'<script>alert(1)</script>'});
  assert.match(doc,/Təqdim olunur/);assert.doesNotMatch(doc,/<script>/);assert.match(doc,/&lt;script&gt;/);
});
test('stored data is normalized and corrupted JSON does not crash',()=>{
  const saved=JSON.parse(storage.get('abb-bda-demo-orders-v1'));
  saved[0].id=saved[0].id.replace(/^AR-/,'DEMO-');
  storage.set('abb-bda-demo-statuses-v1',JSON.stringify({[saved[0].id]:'rejected'}));
  saved[0].details={'visa-azn':{period:'bad',equivalent:true,equivalentCurrency:'<img src=x>'}};
  storage.set('abb-bda-demo-orders-v1',JSON.stringify(saved));
  const order=getOrders()[0];assert.equal(order.details['visa-azn'].period,'1');
  assert.match(order.id,/^AR-/);
  assert.equal(getInquiries().find(o=>o.id===order.id).status,'rejected');
  saveOrder(saved[0]);assert.equal(getOrders().length,1);
  assert.doesNotMatch(documentHtml(order),/<img src=x>/);
  storage.set('abb-bda-demo-orders-v1','{broken');assert.doesNotThrow(getOrders);
});
test('date ranges and operation filters select actual sample transactions',()=>{
  const income=transactionsFor({period:'1',operation:'income'},'2026-09-19');
  assert.equal(income.length,1);assert.ok(income.every(t=>t.type==='income'));
  const custom=transactionsFor({period:'custom',start:'2026-09-13',end:'2026-09-19',operation:'all'},'2026-09-19');
  assert.equal(custom.length,1);assert.equal(custom[0].date,'2026-09-14');
});
