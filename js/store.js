import { accounts, seedInquiries, statuses, documents, embassies } from './data.js';
import {validRange} from './utils.js';

export const newDraft = () => ({type:'statement',embassy:'italy',destination:'embassy',recipient:'',language:'en',accounts:[],details:{},reviewed:false});
export const state = {route:'documents',step:1,substep:'embassy',maxStep:1,authenticated:false,draft:newDraft(),customer:null,showBalances:false,busy:false,error:'',otpDeadline:0,challenge:null,order:null,filter:'all',query:'',page:1,embassy:'italy'};
const ORDER_KEY='abb-bda-demo-orders-v1'; const STATUS_KEY='abb-bda-demo-statuses-v1';
let memoryOrders=[];let memoryStatuses={};let storageUnavailable=false;
export const storageFailed = () => storageUnavailable;
const read=(key,fallback)=>{let raw;try{raw=localStorage.getItem(key);}catch{storageUnavailable=true;return fallback;}try{return JSON.parse(raw)??fallback;}catch{return fallback;}};
const write=(key,value)=>{try{localStorage.setItem(key,JSON.stringify(value));storageUnavailable=false;return true;}catch{storageUnavailable=true;return false;}};
function safeOrder(o){
  return o&&/^(?:AR|DEMO)-\d{4}-[A-Z0-9-]{6,40}$/.test(o.id)&&o.customer==='Aydan Əhədova'&&documents[o.type]&&statuses[o.status]&&Array.isArray(o.accounts)&&o.accounts.length>0&&o.accounts.every(id=>accounts.some(a=>a.id===id&&!a.disabled))&&typeof o.date==='string'&&!Number.isNaN(Date.parse(o.date))&&embassies.some(e=>e.id===o.embassy)&&['az','en'].includes(o.language);
}
export function getOrders(){const saved=read(ORDER_KEY,memoryOrders);return Array.isArray(saved)?saved.filter(safeOrder).slice(0,100).map(o=>{
  const selected=[...new Set(o.accounts)],details={};
  for(const id of selected){const raw=o.details?.[id]||{};const d={language:['en','az'].includes(raw.language)?raw.language:o.language,equivalent:raw.equivalent===true,equivalentCurrency:['RUB','USD','EUR','GBP'].includes(raw.equivalentCurrency)?raw.equivalentCurrency:'RUB',period:['1','3','6','12','custom'].includes(raw.period)?raw.period:'1',operation:['all','income','expense'].includes(raw.operation)?raw.operation:'all',start:typeof raw.start==='string'?raw.start:'',end:typeof raw.end==='string'?raw.end:''};if(d.period==='custom'&&!validRange(d.start,d.end))d.period='1';details[id]=d;}
  return {id:o.id.replace(/^DEMO-/,'AR-'),type:o.type,customer:o.customer,status:o.status,date:o.date,embassy:o.embassy,language:o.language,destination:['embassy','personal','other'].includes(o.destination)?o.destination:'embassy',recipient:typeof o.recipient==='string'?o.recipient.slice(0,120):'',accounts:selected,details,price:documents[o.type].price,reviewed:true,paymentStatus:'paid',seed:false};
}):[];}
export function saveOrder(order){
  const orders=getOrders();if(orders.some(o=>o.id===order.id.replace(/^DEMO-/,'AR-')))return;
  memoryOrders=[order,...orders].slice(0,100);write(ORDER_KEY,memoryOrders);
}
export function getInquiries(){
  const raw=read(STATUS_KEY,memoryStatuses); const changes=raw&&typeof raw==='object'&&!Array.isArray(raw)?raw:{};
  return [...getOrders().filter(o=>o.destination==='embassy'),...seedInquiries].map(o=>{
    const status=changes[o.id]??changes[o.id.replace(/^AR-/,'DEMO-')];
    return {...o,status:statuses[status]?status:o.status};
  });
}
export function updateStatus(id,status){
  if(!statuses[status]||!getInquiries().some(o=>o.id===id))return false;
  const raw=read(STATUS_KEY,memoryStatuses);const prev=raw&&typeof raw==='object'&&!Array.isArray(raw)?raw:{};
  memoryStatuses={...prev,[id]:status};write(STATUS_KEY,memoryStatuses);return true;
}
export const selectedAccounts = () => accounts.filter(a=>state.draft.accounts.includes(a.id));
export function accountDetail(id){return state.draft.details[id]??={language:state.draft.language,equivalent:false,equivalentCurrency:'RUB',period:'1',operation:'all',start:'',end:''};}
export function invalidateReview(){state.draft.reviewed=false;state.maxStep=Math.min(state.maxStep,5);}
export function resetDraft(){state.draft=newDraft();state.step=1;state.maxStep=1;state.substep='embassy';state.order=null;state.error='';}
