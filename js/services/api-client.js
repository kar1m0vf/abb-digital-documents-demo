import * as demo from './api.js';
import { createBackendApi } from './backend-api.js';
import { config } from '../config.js';
import { accounts } from '../data.js';
import { backendEnabled, rememberRemoteOrder, replaceRemoteOrders } from './backend-state.js';

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
