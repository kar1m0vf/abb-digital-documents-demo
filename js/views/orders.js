import { documents, accounts, embassies } from '../data.js';
import { state, getOrders, getInquiries } from '../store.js';
import { button, statusBadge, accountRow } from '../components.js';
import { icon, escapeHtml as esc, dateLabel, money } from '../utils.js';
export function ordersView(paymentOnly=false){
  const all=getInquiries();const orders=getOrders().map(o=>all.find(r=>r.id===o.id)||o);
  if(!orders.length)return `<section class="panel empty-state">${icon(paymentOnly?'credit-card':'inbox')}<h2>${paymentOnly?'Hələ ödəniş yoxdur':'Hələ sifarişiniz yoxdur'}</h2><p>İlk sənədinizi sifariş etdikdən sonra məlumatlar burada görünəcək.</p>${button('Sənəd sifariş et','new-order')}</section>`;
  return `<div class="order-list">${orders.map(o=>`<article class="order-card"><div><h3>${documents[o.type].title}</h3><div class="meta"><span>${esc(o.id)}</span><span>${dateLabel(o.date)}</span><span>${money(o.price)}</span></div><p class="small muted" style="margin-top:10px">${o.destination==='embassy'?embassies.find(e=>e.id===o.embassy)?.name:o.destination==='personal'?'Şəxsi istifadə':esc(o.recipient)}</p></div><div class="order-actions">${paymentOnly?'<span class="status-badge completed">Ödəniş uğurludur</span>':statusBadge(o.status)}<button class="text-link" type="button" data-action="view-inquiry" data-id="${esc(o.id)}">${icon('eye')} Sənədə bax</button></div></article>`).join('')}</div>`;
}
export function productsView(type){return `<section class="panel products-column"><div class="product-toolbar"><h3>${type==='card'?'Kartlarınız':'Hesablarınız'}</h3><button type="button" class="text-link" data-action="balances">${icon(state.showBalances?'eye-off':'eye')} Balansı ${state.showBalances?'gizlət':'göstər'}</button></div><div class="account-list">${accounts.filter(a=>a.type===type).map(a=>accountRow(a,{selectable:false,show:state.showBalances})).join('')}</div><div class="center-actions" style="margin-top:24px">${button('Sənəd sifariş et','new-order')}</div></section>`;}
