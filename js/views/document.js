import { accounts, documents, embassies, demoCustomer, operations } from '../data.js';
import { escapeHtml as esc, money, dateLabel, periodRange } from '../utils.js';
import {transactionsFor} from '../services/transactions.js';
// All balances and exchange rates in this prototype are explicitly sample values.
const rate={AZN:1,USD:1.7,EUR:1.9};
function transactionTable(detail,currency,asOf,en){
  const rows=transactionsFor(detail,asOf);
  if(!rows.length)return `<p>${en?'No transactions in this period.':'Bu dövrdə əməliyyat yoxdur.'}</p>`;
  return `<table><thead><tr><th>${en?'Date':'Tarix'}</th><th>${en?'Description':'Təsvir'}</th><th>${en?'Incoming':'Mədaxil'}</th><th>${en?'Outgoing':'Məxaric'}</th></tr></thead><tbody>${rows.map(t=>`<tr><td>${dateLabel(t.date)}</td><td>${t[en?'en':'az']}</td><td>${t.type==='income'?money(t.amount,currency):'—'}</td><td>${t.type==='expense'?money(t.amount,currency):'—'}</td></tr>`).join('')}</tbody></table>`;
}
export function documentHtml(order){
  const english=order.language!=='az'; const type=documents[order.type]||documents.reference;
  const embassy=embassies.find(e=>e.id===order.embassy)||embassies[0];
  const recipient=order.destination==='personal'?(english?'For personal use':'Şəxsi istifadə üçün'):order.destination==='other'?esc(order.recipient):(english?embassy.en:embassy.name);
  const data=(order.accounts||[]).map(id=>accounts.find(a=>a.id===id)).filter(Boolean);
  return `<article class="paper"><img class="paper-logo" src="./assets/abb-logo.png" alt="ABB" width="110"><div class="paper-meta"><span>${esc(order.id||'—')}</span> <span>${dateLabel(order.date||new Date())}</span></div><h2>${english?(order.type==='statement'?'Account statement':'Account balance reference'):type.title}</h2><p>${english?'To':'Təqdim olunur'}: <strong>${recipient}</strong></p><p>${english?'Account information for':'Seçilmiş hesablar üzrə məlumat'} <strong>${esc(order.customer||demoCustomer.name)}</strong>.</p>${data.map(a=>{const d=order.details?.[a.id]||{language:order.language,period:'1',operation:'all'};const en=(d.language||order.language)!=='az';const range=periodRange(d,(order.date||new Date().toISOString()).slice(0,10));return `<h3>${esc(a.name)} · ${a.last4}</h3><table><tbody><tr><th>${en?'Account number':'Hesab nömrəsi'}</th><td>${a.number}</td></tr><tr><th>${en?'Currency':'Valyuta'}</th><td>${a.currency}</td></tr><tr><th>${en?'Balance':'Qalıq'}</th><td>${money(a.balance,a.currency)}</td></tr>${d.equivalent?`<tr><th>${en?'Equivalent':'Ekvivalent'}</th><td>${money(a.balance*rate[a.currency]/rate[d.equivalentCurrency||'EUR'],d.equivalentCurrency||'EUR')}</td></tr>`:''}${order.type==='statement'?`<tr><th>${en?'Period':'Müddət'}</th><td>${dateLabel(range[0])} — ${dateLabel(range[1])}</td></tr><tr><th>${en?'Operation type':'Əməliyyat növü'}</th><td>${en?({all:'All',income:'Incoming',expense:'Outgoing'}[d.operation||'all']):operations[d.operation||'all']}</td></tr>`:''}</tbody></table>${order.type==='statement'?transactionTable(d,a.currency,(order.date||new Date().toISOString()).slice(0,10),en):''}`;}).join('')}</article>`;
}
