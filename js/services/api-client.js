import * as demo from './api.js';
import { createBackendApi } from './backend-api.js';
import { config } from '../config.js';
import { accounts, embassies } from '../data.js';
import { backendEnabled, rememberRemoteOrder, replaceRemoteOrders, portalState, setPortalState } from './backend-state.js';

export const DEMO=demo.DEMO;
const productIds={'visa-azn':101,'master-azn':102,'visa-usd':103,credit:104,'account-azn':105,'account-usd':106,'account-eur':107};
const presentationAccounts=accounts.map(account=>({...account}));
const last4=value=>String(value??'').replace(/\D/g,'').slice(-4).padStart(4,'0');
const maskedAccount=value=>`AZ•• •••• •••• •••• •••• ${last4(value)}`;
export function useCustomerProducts(products) {
  const presentation=presentationAccounts.map(account=>products.find(item=>item.id===productIds[account.id]));
  if(presentation.every(Boolean)) {
    const updates=presentation.map((product,index)=>{
      if(product.currency!==presentationAccounts[index].currency||!Number.isFinite(Number(product.balance)))throw new Error('Hesab məlumatları uyğun deyil. Yenidən cəhd edin.');
      return Number(product.balance);
    });
    accounts.splice(0,accounts.length,...presentationAccounts.map((account,index)=>({...account,balance:updates[index]})));
    return;
  }

  const next=products.flatMap(product=>{
    const currency=product.currency;
    const balance=Number(product.balance);
    if(!Number.isFinite(balance)||!['AZN','USD','EUR'].includes(currency))throw new Error('Hesab məlumatları uyğun deyil. Yenidən cəhd edin.');
    const cards=Array.isArray(product.cards)?product.cards.map(card=>({
      id:`card-${card.id}`,
      name:card.brand==='MASTERCARD'?'Tam Mastercard':'Tam Visa',
      type:'card',currency,last4:last4(card.maskedNumber),number:card.maskedNumber,
      balance,image:card.brand==='MASTERCARD'?'card-mastercard.jpeg':'card-visa.jpeg'
    })):[];
    return [...cards,{
      id:`account-${product.id}`,
      name:product.type==='SAVING'?'Əmanət hesabı':'Cari hesab',
      type:'account',currency,last4:last4(product.accountNumber),number:maskedAccount(product.accountNumber),balance
    }];
  });
  if(!next.length)throw new Error('Hesab məlumatları tapılmadı.');
  accounts.splice(0,accounts.length,...next);
}
const backend=createBackendApi({baseUrl:config.apiBase,onOrder:rememberRemoteOrder,onOrders:replaceRemoteOrders,onAccounts:products=>{
  // Keep the approved presentation fixtures intact; map upstream customers into the same components.
  useCustomerProducts(products);
}});
const adapter=backendEnabled?backend:demo;
export const requestOtp=(...args)=>adapter.requestOtp(...args);
export const verifyOtp=(...args)=>adapter.verifyOtp(...args);
export const submitOrder=(...args)=>adapter.submitOrder(...args);
export const refreshOrders=()=>backendEnabled?backend.refreshOrders():Promise.resolve();
export const updateRemoteStatus=(id,status)=>backend.updateStatus(id,status);
export const logout=()=>backend.logout();

// --- Embassy portal (real endpoints) ---
const periodMap={'1M':'1','3M':'3','6M':'6','1Y':'12'};
const typeMap={ACCOUNT_STATEMENT:'statement',EMBASSY_CERTIFICATE:'reference'};
const statusMap={COMPLETED:'completed',REJECTED:'rejected'};
const portalStatusToView={COMPLETED:'COMPLETED',REJECTED:'REJECTED'};
const embassyIdFor=name=>embassies.find(e=>e.id===name)?.id||embassies.find(e=>e.en.toLowerCase()===String(name).toLowerCase())?.id||embassies[0].id;
function portalRow(doc,embassyId){
  return {id:doc.documentNumber,customer:doc.customerName,type:typeMap[doc.documentType]||'statement',
    status:statusMap[doc.status]||'pending',date:String(doc.date||'').slice(0,10),embassy:embassyId,language:'en',
    destination:'embassy',recipient:'',accounts:[],details:{},price:5,portal:true,documentNumber:doc.documentNumber};
}
export function portalDetailOrder(detail,embassyId){
  const items=detail.items||[];
  const resolved=items.map(item=>{
    const digits=last4(item.accountNumber);
    const match=accounts.find(a=>a.last4===digits&&a.currency===item.currency)||accounts.find(a=>a.last4===digits);
    const fallback=match||accounts.find(a=>a.currency===item.currency&&!a.disabled)||accounts[0];
    return {...fallback,number:maskedAccount(item.accountNumber),last4:digits,currency:item.currency||fallback.currency};
  });
  const row=portalRow({documentNumber:detail.documentNumber,customerName:detail.customer?.fullName||'',
    documentType:detail.documentType,status:detail.status,date:detail.createdAt},embassyId);
  const details={};
  resolved.forEach((account,index)=>{details[account.id]=row.details[account.id]??{language:'en',equivalent:false,
    equivalentCurrency:account.currency==='EUR'?'USD':'EUR',period:periodMap[items[index]?.period]||'1',operation:'all',start:'',end:''};});
  return {...row,accounts:resolved.map(a=>a.id),resolvedAccounts:resolved,details,
    timeline:(detail.timeline||[]).map(entry=>({step:entry.step,description:entry.description,timestamp:entry.timestamp}))};
}
export async function portalLogin(username,password){
  if(!backendEnabled)return null;
  const result=await backend.portalLogin(username,password);
  const state={user:result,embassyId:embassyIdFor(result.embassyName),embassyName:result.embassyName,stats:null,documents:[]};
  setPortalState(state);
  return state;
}
export async function loadPortal({search='',status='ALL',page=0,size=100}={}){
  const current=portalState();
  if(!backendEnabled||!current)return null;
  const [stats,page1]=await Promise.all([backend.portalStats(),backend.portalDocuments({search,status,page,size})]);
  const next={...current,stats,documents:(page1.documents||[]).map(doc=>portalRow(doc,current.embassyId)),
    totalElements:page1.totalElements??(page1.documents||[]).length,totalPages:page1.totalPages??1};
  setPortalState(next);
  return next;
}
export async function portalDocumentDetail(documentNumber){
  const current=portalState();
  if(!backendEnabled||!current)return null;
  return portalDetailOrder(await backend.portalDocument(documentNumber),current.embassyId);
}
export async function portalUpdateStatus(documentNumber,status){
  if(!backendEnabled||!portalState())return false;
  await backend.portalUpdateStatus(documentNumber,portalStatusToView[status]||'COMPLETED');
  await loadPortal();return true;
}
export function portalLogout(){backend.portalLogout();setPortalState(null);}
export function notificationList(){return backendEnabled?backend.listNotifications():Promise.resolve(null);}
export function notificationRead(id){return backendEnabled?backend.readNotification(id):Promise.resolve(null);}
export function orderDetail(id){return backendEnabled?backend.getOrder(id):Promise.resolve(null);}
