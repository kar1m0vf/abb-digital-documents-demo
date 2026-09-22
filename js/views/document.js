import { accounts, documents, embassies, demoCustomer, operations } from '../data.js';
import { escapeHtml as esc, money, dateLabel, periodRange } from '../utils.js';
import { transactionsFor } from '../services/transactions.js';

// These values belong to the presentation prototype, not to a live exchange-rate feed.
const rate = { AZN: 1, USD: 1.7, EUR: 1.9, RUB: 0.019, GBP: 2.2 };
const amount = (value, currency) => `${currency} ${Number(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function transactionTable(detail, currency, asOf, english) {
  const rows = transactionsFor(detail, asOf);
  if (!rows.length) return `<p>${english ? 'No transactions in this period.' : 'Bu dövrdə əməliyyat yoxdur.'}</p>`;
  return `<table class="paper-transactions"><thead><tr><th>${english ? 'Date' : 'Tarix'}</th><th>${english ? 'Description' : 'Təsvir'}</th><th>${english ? 'Incoming' : 'Mədaxil'}</th><th>${english ? 'Outgoing' : 'Məxaric'}</th></tr></thead><tbody>${rows.map(row => `<tr><td>${dateLabel(row.date)}</td><td>${row[english ? 'en' : 'az']}</td><td>${row.type === 'income' ? money(row.amount, currency) : '—'}</td><td>${row.type === 'expense' ? money(row.amount, currency) : '—'}</td></tr>`).join('')}</tbody></table>`;
}

function accountTable(account, detail, order, asOf) {
  const english = (detail.language || order.language) !== 'az';
  const equivalentCurrency = rate[detail.equivalentCurrency] ? detail.equivalentCurrency : 'EUR';
  const equivalent = detail.equivalent
    ? amount(account.balance * rate[account.currency] / rate[equivalentCurrency], equivalentCurrency)
    : '—';
  const range = periodRange(detail, asOf);
  const row = (label, value) => `<tr><th scope="row">${label}</th><td>${value}</td></tr>`;
  const lines = [
    row(english ? 'Account type' : 'Hesab növü', english ? (account.type === 'card' ? 'Card' : 'Account') : (account.type === 'card' ? 'Kart' : 'Hesab')),
    row(english ? 'Account No' : 'Hesab №', esc(account.number)),
    ...(account.type === 'card' ? [row(english ? 'Card No' : 'Kart №', `•••• •••• •••• ${esc(account.last4)}`)] : []),
    row(english ? 'Balance' : 'Qalıq', amount(account.balance, account.currency)),
    row(english ? 'Balance equivalent' : 'Qalıq ekvivalenti', equivalent),
  ];
  if (order.type === 'statement') {
    lines.push(row(english ? 'Period' : 'Müddət', `${dateLabel(range[0])} — ${dateLabel(range[1])}`));
    lines.push(row(english ? 'Operation type' : 'Əməliyyat növü', english ? ({ all: 'All', income: 'Incoming', expense: 'Outgoing' }[detail.operation || 'all']) : operations[detail.operation || 'all']));
  }
  return `<table class="letter-account-table"><tbody>${lines.join('')}</tbody></table>${order.type === 'statement' ? transactionTable(detail, account.currency, asOf, english) : ''}`;
}

export function documentHtml(order) {
  const english = order.language !== 'az';
  const type = documents[order.type] || documents.reference;
  const embassy = embassies.find(item => item.id === order.embassy) || embassies[0];
  const recipient = order.destination === 'personal'
    ? (english ? 'For personal use' : 'Şəxsi istifadə üçün')
    : order.destination === 'other' ? esc(order.recipient || '') : esc(english ? embassy.en : embassy.name);
  const customer = esc(order.customer || demoCustomer.name);
  const asOf = (order.date || new Date().toISOString()).slice(0, 10);
  const date = dateLabel(asOf);
  const selected = (order.accounts || []).map(id => accounts.find(account => account.id === id)).filter(Boolean);
  const title = english ? (order.type === 'statement' ? 'Account statement' : 'Account balance reference') : type.title;
  const introduction = english
    ? `By this letter “ABB” OJSC, Branch Network and Sales Support Department hereby confirms that ${customer} holds the following accounts with “ABB” OJSC as of ${date}.`
    : `“ABB” ASC ${date} tarixinə ${customer} adına aşağıdakı hesabların mövcudluğunu təsdiq edir.`;
  const exchange = english
    ? `Illustrative exchange rates used for this document as of ${date}: 1 EUR = 1.9 AZN, 1 GBP = 2.2 AZN, 1 USD = 1.7 AZN.`
    : `Bu sənəddə istifadə edilən şərti məzənnələr (${date}): 1 EUR = 1.9 AZN, 1 GBP = 2.2 AZN, 1 USD = 1.7 AZN.`;
  const privacy = english
    ? 'Under current legislation, the information in this letter is protected by banking secrecy and cannot be disclosed to third parties.'
    : 'Qanunvericiliyə əsasən, bu məktubdakı məlumatlar bank sirrini təşkil edir və digər şəxslərə açıqlana bilməz.';
  const recipientLabel = english ? (['personal', 'other'].includes(order.destination) ? recipient : `To the ${recipient}`) : `Təqdim olunur: ${recipient}`;
  return `<article class="paper paper-letter"><h2 class="sr-only">${title}</h2><p class="letter-recipient">${recipientLabel}</p><p class="letter-intro">${introduction}</p>${selected.map(account => accountTable(account, order.details?.[account.id] || { language: order.language, period: '1', operation: 'all' }, order, asOf)).join('')}<p class="letter-exchange">${exchange}</p><p class="letter-privacy">${privacy}</p><div class="letter-signoff"><strong>${english ? 'Sincerely yours,<br>Department Director' : 'Hörmətlə,<br>Sənəd xidməti'}</strong><strong>ABB Document Service</strong></div></article>`;
}
