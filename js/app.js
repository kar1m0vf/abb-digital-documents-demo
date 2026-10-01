import { documents, accounts, steps, embassies, MAX_SELECTED_ACCOUNTS } from './data.js';
import { state, getOrders, getInquiries, saveOrder, updateStatus, accountDetail, selectedAccounts, invalidateReview, resetDraft, storageFailed } from './store.js';
import { renderHeader, shell, stepper, notice, button } from './components.js';
import { documentType, embassy, fin, otp } from './views/identity.js';
import { accountSelection, details } from './views/accounts.js';
import { review, payment, confirmation } from './views/checkout.js';
import { dashboard, stats, inquiryTable, filteredRows, portalLogin } from './views/dashboard.js';
import { ordersView, productsView } from './views/orders.js';
import { openModal, closeModal, showDocument, toast, finHelp, phoneHelp } from './modal.js';
import { requestOtp, verifyOtp, submitOrder, refreshOrders, logout, DEMO, portalLogin as signInPortal, loadPortal, portalDocumentDetail, notificationList, notificationRead, orderDetail } from './services/api-client.js';
import { backendEnabled, portalState } from './services/backend-state.js';
import { cleanCode, validFin, validOtp, validRange, validCard, validExpiry, generateUUID, escapeHtml as esc, icon, dateLabel } from './utils.js';

import { clearMotion, revealEquivalent, showCardBack, animateConfirmation } from './motion.js';
import { closeSelect, installSelects } from './select.js';

const main=document.querySelector('#main');const header=document.querySelector('#header');
document.documentElement.dataset.inputModality='pointer';
document.addEventListener('pointerdown',()=>{document.documentElement.dataset.inputModality='pointer';},true);
document.addEventListener('keydown',event=>{
  if(['Tab','Enter',' ','ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Escape'].includes(event.key))document.documentElement.dataset.inputModality='keyboard';
},true);

let timer=null;let lastFin='';let paymentKey=generateUUID();
const routes=new Set(['documents','orders','payments','accounts','cards','embassy']);

function render({focus=false}={}){
  const active=document.activeElement;
  const focusSelector=active?.id?`#${CSS.escape(active.id)}`:active?.name?`[name="${CSS.escape(active.name)}"]${active.type==='radio'?`[value="${CSS.escape(active.value)}"]`:''}`:active?.dataset.action?`[data-action="${CSS.escape(active.dataset.action)}"]`:null;
  closeSelect();
  clearMotion();
  clearInterval(timer);document.body.classList.toggle('embassy-mode',state.route==='embassy');
  header.innerHTML=renderHeader();document.documentElement.lang=state.route==='embassy'?'en':'az';
  document.title=state.route==='embassy'?'Embassy Inquiries':'Rəqəmsal Sənəd Sifarişi';
  if(state.route==='embassy')main.innerHTML=backendEnabled&&!portalState()?portalLogin():dashboard();
  else if(state.route==='orders')main.innerHTML=shell(ordersView(),{wizard:false,title:'Sifarişlərim',description:'Sənədlərinizi açın və sifarişlərin statusunu izləyin.'});
  else if(state.route==='payments')main.innerHTML=shell(ordersView(true),{wizard:false,title:'Ödənişlər',description:'Sənəd sifarişləri üzrə ödəniş tarixçəsi.'});
  else if(['accounts','cards'].includes(state.route))main.innerHTML=shell(productsView(state.route==='accounts'?'account':'card'),{wizard:false,title:state.route==='accounts'?'Hesablar':'Kartlar'});
  else{
    if(state.step>=3&&!state.authenticated){state.step=2;state.substep='fin';}
    if(state.step===7&&state.order)state.order=getInquiries().find(o=>o.id===state.order.id)||getOrders().find(o=>o.id===state.order.id)||state.order;
    const content=state.step===1?documentType():state.step===2?(state.substep==='fin'?fin():state.substep==='otp'?otp():embassy()):state.step===3?accountSelection():state.step===4?(state.substep==='destination'?embassy():details()):state.step===5?review():state.step===6?payment():confirmation();
    main.innerHTML=shell(content);
    if(state.step===2&&state.substep==='otp'){updateCountdown();timer=setInterval(updateCountdown,1000);}
  }
  animateConfirmation(main);
  if(focus){main.querySelector('[data-view-heading]')?.focus({preventScroll:true});window.scrollTo({top:0,behavior:'instant'});}
  else if(focusSelector)document.querySelector(focusSelector)?.focus({preventScroll:true});
}
function route(){const value=location.hash.replace(/^#\//,'');const nextRoute=routes.has(value)?value:'documents';if(nextRoute!==state.route)state.showBalances=false;state.route=nextRoute;state.error='';render({focus:true});if(backendEnabled)syncOrders(true);if(state.route==='embassy')syncPortal();}
let syncingOrders=false;
async function syncOrders(reportError=false){
  if(syncingOrders||state.busy||document.querySelector('[role="combobox"][aria-expanded="true"]'))return;
  syncingOrders=true;
  const before=JSON.stringify(getOrders());
  try{await refreshOrders();if(!state.busy&&before!==JSON.stringify(getOrders())){if(state.route==='embassy')refreshDashboard();else if(['orders','payments'].includes(state.route))render();}}
  catch(error){if(reportError)toast(error.message);}
  finally{syncingOrders=false;}
}
let syncingPortal=false;
async function syncPortal(reportError=false){
  if(!backendEnabled||state.route!=='embassy'||!portalState()||syncingPortal||state.busy)return;
  syncingPortal=true;
  try{await loadPortal();refreshDashboard();}
  catch(error){if(reportError)toast(error.message);}
  finally{syncingPortal=false;}
}
window.addEventListener('hashchange',route);
window.addEventListener('storage',e=>{if(e.key?.startsWith('abb-bda-demo-')&&!state.busy){if(state.route==='embassy')refreshDashboard();else if(['orders','payments'].includes(state.route))render();}});
function navigate(to){if(state.route===to){render({focus:true});}else location.hash=`/${to}`;}
function updateCountdown(){const el=document.querySelector('#countdown'),btn=document.querySelector('#resend-button');if(!el||!btn)return;const secs=Math.max(0,Math.ceil((state.otpDeadline-Date.now())/1000));el.textContent=secs?`(${Math.floor(secs/60)}:${String(secs%60).padStart(2,'0')})`:'';btn.disabled=secs>0||state.busy;}
function setBusy(busy){state.busy=busy;main.setAttribute('aria-busy',String(busy));
  for(const el of document.querySelectorAll('button,input,select')){
    if(busy&&!el.disabled){el.dataset.busyDisabled='true';el.disabled=true;}
    else if(!busy&&el.dataset.busyDisabled){el.disabled=false;delete el.dataset.busyDisabled;}
  }
  const primary=main.querySelector('.form-actions .btn-primary');if(primary){if(busy){primary.dataset.oldLabel=primary.innerHTML;primary.innerHTML='<span class="spinner" aria-hidden="true"></span> Gözləyin…';}else if(primary.dataset.oldLabel){primary.innerHTML=primary.dataset.oldLabel;delete primary.dataset.oldLabel;}}
  updateCountdown();
}
function setError(message,field){state.error=message;const el=document.querySelector('#form-error');if(el){el.textContent=message;el.hidden=!message;}if(field){field.setAttribute('aria-invalid','true');field.focus();}main.querySelector('.code-fields')?.classList.toggle('invalid',Boolean(message));}
function clearError(){setError('');main.querySelectorAll('[aria-invalid=true]').forEach(el=>el.removeAttribute('aria-invalid'));}
function transition(step,substep='embassy'){state.error='';state.step=step;state.substep=substep;state.maxStep=Math.max(state.maxStep,step);render({focus:true});}
function validateDetails(){for(const a of selectedAccounts()){const d=accountDetail(a.id);if(state.draft.type==='statement'&&d.period==='custom'&&!validRange(d.start,d.end)){setError(`${a.name} · ${a.last4}: düzgün tarix aralığı seçin. Son tarix bu gündən sonra ola bilməz.`,document.querySelector(`#start-${a.id}`));return false;}}return true;}
function next(){
  if(state.step===1){if(state.draft.type==='statement')transition(state.authenticated?3:2,'fin');else transition(2);return;}
  if(state.step===2){
    if(state.substep==='fin')return handleFin();if(state.substep==='otp')return handleOtp();
    if(state.draft.destination==='other'&&!state.draft.recipient.trim()){setError('Qurumun adını daxil edin.',document.querySelector('#recipient'));return;}
    transition(state.authenticated?3:2,state.authenticated?'embassy':'fin');return;
  }
  if(state.step===3){if(!state.draft.accounts.length){setError('Ən azı bir hesab və ya kart seçin.');return;}if(state.draft.accounts.length>MAX_SELECTED_ACCOUNTS){setError(`Ən çox ${MAX_SELECTED_ACCOUNTS} kart və ya hesab seçə bilərsiniz.`);return;}selectedAccounts().forEach(a=>{const d=accountDetail(a.id);if(d.equivalentCurrency===a.currency)d.equivalentCurrency=a.currency==='EUR'?'USD':'EUR';});transition(4);return;}
  if(state.step===4){
    if(state.substep==='destination'){
      if(state.draft.destination==='other'&&!state.draft.recipient.trim()){setError('Qurumun adını daxil edin.',document.querySelector('#recipient'));return;}
      transition(5);
    }else if(validateDetails()){if(state.draft.type==='statement')transition(4,'destination');else transition(5);}
    return;
  }
  if(state.step===5){if(!state.draft.reviewed){setError('Sənəddəki məlumatları təsdiqləyin.');return;}paymentKey=generateUUID();transition(6);}
}
function back(){
  if(state.step===2&&state.substep==='otp'){transition(2,'fin');return;}
  if(state.draft.type==='statement'){
    if(state.step===2||state.step===3){transition(1);return;}
    if(state.step===4&&state.substep==='destination'){transition(4);return;}
    if(state.step===5){transition(4,'destination');return;}
  }
  if(state.step===2&&state.substep==='fin'){transition(2);return;}
  if(state.step>1&&state.step<7)transition(state.step-1);
}
const codeValue=name=>Array.from(main.querySelectorAll(`[data-code="${name}"] input`)).map(el=>el.value).join('');
async function handleFin(){const value=codeValue('fin');if(!validFin(value)){setError('FİN kodunu tam daxil edin: 7 simvol (hərf və rəqəm).',main.querySelector('[data-code] input'));return;}clearError();setBusy(true);
  try{const challenge=await requestOtp(value);lastFin=value;state.challenge=challenge.id;state.otpDeadline=challenge.resendAt;state.otpPhone=challenge.phone;setBusy(false);transition(2,'otp');main.querySelector('[data-code] input')?.focus();}
  catch(e){setBusy(false);setError(e.message,main.querySelector('[data-code] input'));}
}
async function handleOtp(){const value=codeValue('otp');if(!validOtp(value)){setError('6 rəqəmli təsdiqləmə kodunu tam daxil edin.',main.querySelector('[data-code] input'));return;}clearError();setBusy(true);
  try{state.customer=await verifyOtp(state.challenge,value);state.authenticated=true;state.challenge=null;lastFin='';setBusy(false);transition(3);}
  catch(e){setBusy(false);setError(e.message,main.querySelector('[data-code] input'));}
}
async function resend(){if(state.busy||Date.now()<state.otpDeadline)return;clearError();setBusy(true);try{const c=await requestOtp(lastFin);state.challenge=c.id;state.otpDeadline=c.resendAt;main.querySelectorAll('[data-code] input').forEach(el=>el.value='');setBusy(false);updateCountdown();toast('Yeni kod tələb edildi.');main.querySelector('[data-code] input')?.focus();}catch(e){setBusy(false);setError(e.message);}}
async function pay(){
  if(state.step!==6||!state.authenticated||!state.draft.reviewed)return;
  const number=document.querySelector('#card-number'),expiry=document.querySelector('#card-expiry'),cvv=document.querySelector('#card-cvv');
  clearError();if(!validCard(number.value)){setError('Düzgün 16 rəqəmli kart nömrəsi daxil edin.',number);return;}
  if(!validExpiry(expiry.value)){setError('Son istifadə tarixini AA/İİ formatında daxil edin.',expiry);return;}
  if(!/^\d{3}$/.test(cvv.value)){setError('3 rəqəmli CVV kodunu daxil edin.',cvv);return;}
  const value=number.value.replace(/\s/g,'');
  if(![DEMO.card,DEMO.declinedCard].map(v=>v.replace(/\s/g,'')).includes(value)){setError('Bu kartla ödəniş mümkün deyil. Digər kartdan istifadə edin.',number);return;}
  const token=value.endsWith('0002')?'demo-declined':'demo-success';cvv.value='';updateCardPreview();setBusy(true);
  try{const order=await submitOrder(state.draft,token,paymentKey);const detail=order.orderId?await orderDetail(order.orderId).catch(()=>null):null;saveOrder(detail?{...order,timeline:detail.timeline}:order);state.order=detail?{...order,timeline:detail.timeline}:order;setBusy(false);transition(7);if(storageFailed())toast('Sifariş bu sessiyada saxlanıldı. Brauzerin daimi yaddaşı əlçatan deyil.');}
  catch(e){setBusy(false);setError(e.message);}
}
function refreshDashboard(){const statCards=document.querySelector('#dashboard-stats'),panel=document.querySelector('#inquiries-panel');if(!statCards||!panel)return;statCards.innerHTML=stats();panel.innerHTML=inquiryTable();}
async function handlePortalLogin(){
  const username=document.querySelector('#portal-username')?.value.trim()||'';
  const password=document.querySelector('#portal-password')?.value||'';
  if(!username||!password){setError('Username and password are required.',document.querySelector('#portal-username'));return;}
  clearError();setBusy(true);
  try{await signInPortal(username,password);state.embassy=portalState().embassyId;setBusy(false);await syncPortal();render({focus:true});}
  catch(error){setBusy(false);setError(error.message);}
}
async function showNotifications(){
  if(backendEnabled&&state.authenticated){
    try{
      const result=await notificationList();
      const items=result?.notifications||[];
      openModal('Bildirişlər',items.length?`<div class="stack">${items.map(item=>`<div class="row"><div><strong>${esc(item.title)}</strong><p class="small">${esc(item.body)}</p><p class="small muted">${dateLabel(String(item.createdAt||'').slice(0,10))}</p>${item.read?'':`<button class="text-link" type="button" data-action="notification-read" data-id="${esc(item.notificationId)}">Oxundu işarələ</button>`}</div></div>`).join('')}</div>`:`<p class="muted">Hələ yeni bildiriş yoxdur.</p>`,{footer:items.length?button('Sifarişlərə bax','notification-orders'):''});
      return;
    }catch(error){toast(error.message);return;}
  }
  const orders=getOrders();
  openModal('Bildirişlər',orders.length?`<p>${orders.length} sifarişiniz var. Sənədlər və son statuslar «Sifarişlərim» bölməsindədir.</p>`:'<p class="muted">Hələ yeni bildiriş yoxdur.</p>',{footer:orders.length?button('Sifarişlərə bax','notification-orders'):''});
}
async function markNotification(id){
  if(!id)return;
  try{await notificationRead(id);toast('Bildiriş oxunmuş kimi işarələndi.');await showNotifications();}
  catch(error){toast(error.message);}
}
async function viewInquiry(id){
  const portalRow=backendEnabled&&portalState()?.documents.find(doc=>doc.id===id);
  if(portalRow){
    setBusy(true);
    try{const detail=await portalDocumentDetail(id);setBusy(false);if(detail)showDocument(detail);}
    catch(error){setBusy(false);toast(error.message);}
    return;
  }
  const order=getInquiries().find(o=>o.id===id)||getOrders().find(o=>o.id===id);
  if(order)showDocument(order);
}
function beginNewOrder(){resetDraft();paymentKey=generateUUID();navigate('documents');}
function startNewOrder(){if(state.step>1&&state.step<7&&state.draft.accounts.length){openModal('Yeni sifariş başlasın?', '<p>Cari sifarişin seçimləri silinəcək. Əvvəl yaradılmış sifarişləriniz saxlanılacaq.</p>',{footer:button('Davam et','confirm-new')+button('Geri','close-modal','secondary')});}else beginNewOrder();}

document.addEventListener('click',async event=>{
  if(event.target.closest('.skip-link')){event.preventDefault();main.focus();return;}
  const actionEl=event.target.closest('[data-action]');const nav=event.target.closest('a[href^="#/"]');
  if(state.busy&&(actionEl||nav)){event.preventDefault();return;}
  if(!actionEl)return;event.preventDefault();if(actionEl.disabled)return;
  const action=actionEl.dataset.action;
  switch(action){
    case 'next':next();break;case 'back':back();break;case 'verify-fin':await handleFin();break;case 'verify-otp':await handleOtp();break;case 'resend':await resend();break;case 'pay':await pay();break;
    case 'jump':{const step=Number(actionEl.dataset.step);if(step>=1&&step<=state.step&&state.step<7)transition(step,actionEl.dataset.substep||'embassy');break;}
    case 'edit-details':invalidateReview();transition(4);break;
    case 'menu':{const nav=document.querySelector('#main-nav');const open=nav.classList.toggle('open');actionEl.setAttribute('aria-expanded',String(open));break;}
    case 'fin-help':finHelp();break;case 'phone-help':phoneHelp();break;case 'close-modal':closeModal();break;
    case 'preview':showDocument(state.draft);break;case 'preview-order':showDocument(state.order);break;
    case 'balances':state.showBalances=!state.showBalances;render();break;
    case 'orders':navigate('orders');break;case 'new-order':startNewOrder();break;case 'confirm-new':closeModal();beginNewOrder();break;
    case 'copy-order':try{await navigator.clipboard.writeText(state.order.id);toast('Sənəd nömrəsi kopyalandı.');}catch{openModal('Sənəd nömrəsi',`<p>${esc(state.order.id)}</p>`);}break;
    case 'notifications':await showNotifications();break;
    case 'notification-orders':closeModal();navigate('orders');break;
    case 'notification-read':closeModal();await markNotification(actionEl.dataset.id);break;
    case 'profile':openModal('Profil',`<div class="row"><span class="avatar">${state.customer.initials}</span><div><strong>${esc(state.customer.name)}</strong><p class="muted small">Şəxsi hesab</p></div></div>`,{footer:button('Hesabdan çıx','logout','secondary')});break;
    case 'logout':closeModal();logout();state.authenticated=false;state.customer=null;state.challenge=null;state.otpPhone='';lastFin='';resetDraft();navigate('documents');toast('Hesabdan çıxdınız.');break;
    case 'filter':state.filter=actionEl.dataset.filter;state.page=1;refreshDashboard();break;
    case 'clear-filter':state.filter='all';state.query='';state.page=1;document.querySelector('#inquiry-search').value='';refreshDashboard();break;
    case 'page':state.page=Math.max(1,Math.min(Math.ceil(filteredRows().length/10),Number(actionEl.dataset.page)));refreshDashboard();break;
    case 'portal-login':await handlePortalLogin();break;
    case 'view-inquiry':await viewInquiry(actionEl.dataset.id);break;
  }
});
document.addEventListener('submit',event=>{event.preventDefault();if(state.busy)return;const id=event.target.id;if(id==='fin-form')handleFin();else if(id==='otp-form')handleOtp();else if(id==='payment-form')pay();else if(id==='portal-login-form')handlePortalLogin();else next();});

main.addEventListener('input',event=>{
  const el=event.target;if(state.busy)return;
  if(el.closest('[data-code]')){const group=el.closest('[data-code]');const inputs=[...group.querySelectorAll('input')];const index=inputs.indexOf(el);const chars=cleanCode(el.value,group.dataset.code==='otp');
    if(chars.length>1){const start=chars.length>=inputs.length?0:index;for(let i=start;i<inputs.length;i++)inputs[i].value=chars[i-start]||'';inputs[Math.min(start+chars.length,inputs.length-1)].focus();}
    else{el.value=chars.slice(0,1);if(chars&&index<inputs.length-1)inputs[index+1].focus();}
    clearError();return;
  }
  if(el.id==='inquiry-search'){state.query=el.value;state.page=1;refreshDashboard();return;}
  if(el.id==='recipient'){state.draft.recipient=el.value;invalidateReview();clearError();}
  if(el.id==='card-number'){el.value=el.value.replace(/\D/g,'').slice(0,16).replace(/(.{4})/g,'$1 ').trim();updateCardPreview();clearError();}
  if(el.id==='card-expiry'){const value=el.value.replace(/\D/g,'').slice(0,4);el.value=value.length>2?value.slice(0,2)+'/'+value.slice(2):value;updateCardPreview();clearError();}
  if(el.id==='card-cvv'){el.value=el.value.replace(/\D/g,'').slice(0,3);updateCardPreview();clearError();}
});
function updateCardPreview(){
  const number=main.querySelector('#card-number'),expiry=main.querySelector('#card-expiry'),cvv=main.querySelector('#card-cvv');
  if(!number||!expiry||!cvv)return;
  main.querySelector('.payment-card-number').textContent=number.value;
  main.querySelector('.payment-card-expiry').textContent=expiry.value;
  main.querySelector('.payment-card-cvv').textContent=cvv.value;
}
main.addEventListener('focusin',event=>{if(['card-number','card-expiry','card-cvv'].includes(event.target.id))showCardBack(event.target.id==='card-cvv');});
main.addEventListener('paste',event=>{const el=event.target;const group=el.closest('[data-code]');if(!group)return;event.preventDefault();const inputs=[...group.querySelectorAll('input')];const chars=cleanCode(event.clipboardData.getData('text'),group.dataset.code==='otp');const start=chars.length>=inputs.length?0:inputs.indexOf(el);for(let i=start;i<inputs.length;i++)inputs[i].value=chars[i-start]||'';inputs[Math.min(start+chars.length,inputs.length-1)].focus();clearError();});
main.addEventListener('keydown',event=>{const el=event.target;
  if(event.key==='Enter'&&el.tagName==='INPUT'&&el.closest('form')&&!['checkbox','radio','date'].includes(el.type)){
    event.preventDefault();if(state.busy)return;const id=el.closest('form').id;
    if(id==='fin-form')handleFin();else if(id==='otp-form')handleOtp();else if(id==='payment-form')pay();else if(id==='portal-login-form')handlePortalLogin();else next();return;
  }
  const group=el.closest('[data-code]');if(!group)return;const inputs=[...group.querySelectorAll('input')];const i=inputs.indexOf(el);if(event.key==='Backspace'&&!el.value&&i>0){event.preventDefault();inputs[i-1].value='';inputs[i-1].focus();}else if(event.key==='ArrowLeft'&&i>0){event.preventDefault();inputs[i-1].focus();}else if(event.key==='ArrowRight'&&i<inputs.length-1){event.preventDefault();inputs[i+1].focus();}
});
document.addEventListener('keydown',event=>{if(event.key==='Escape'){document.querySelector('#main-nav')?.classList.remove('open');document.querySelector('[data-action="menu"]')?.setAttribute('aria-expanded','false');}});
main.addEventListener('change',event=>{
  const el=event.target;if(state.busy)return;
  if(el.name==='document-type'){state.draft.type=el.value;if(el.value==='reference')state.draft.destination='embassy';invalidateReview();const nextStepper=document.createElement('template');nextStepper.innerHTML=stepper();document.querySelector('.stepper').replaceWith(nextStepper.content.querySelector('.stepper'));document.querySelector('.wizard').dataset.document=el.value;}
  else if(el.name==='destination'){state.draft.destination=el.value;invalidateReview();render();}
  else if(el.name==='language'){state.draft.language=el.value;for(const detail of Object.values(state.draft.details))detail.language=el.value;invalidateReview();}
  else if(el.name==='account'){
    const a=accounts.find(a=>a.id===el.value);if(!a||a.disabled)return;
    if(el.checked&&!state.draft.accounts.includes(a.id)&&state.draft.accounts.length>=MAX_SELECTED_ACCOUNTS){el.checked=false;setError(`Ən çox ${MAX_SELECTED_ACCOUNTS} kart və ya hesab seçə bilərsiniz.`);return;}
    state.draft.accounts=el.checked?[...new Set([...state.draft.accounts,a.id])]:state.draft.accounts.filter(id=>id!==a.id);
    accountDetail(a.id);invalidateReview();document.querySelector('#selection-summary').textContent=`${state.draft.accounts.length}/${MAX_SELECTED_ACCOUNTS} məhsul seçilib`;document.querySelector('.form-actions .btn-primary').disabled=!state.draft.accounts.length;
    const limitReached=state.draft.accounts.length>=MAX_SELECTED_ACCOUNTS;
    main.querySelectorAll('input[name="account"]').forEach(input=>{if(!accounts.find(account=>account.id===input.value)?.disabled)input.disabled=limitReached&&!input.checked;});
    clearError();
  }
  else if(el.name==='reviewed'){state.draft.reviewed=el.checked;clearError();if(state.step===5&&el.checked)next();else document.querySelector('.form-actions .btn-primary').disabled=true;}
  else if(el.closest('[data-account-id]')){
    const card=el.closest('[data-account-id]');const d=accountDetail(card.dataset.accountId);
    if(el.dataset.detail){d[el.dataset.detail]=el.type==='checkbox'?el.checked:el.value;if(el.dataset.detail==='equivalent')revealEquivalent(card.querySelector('.equivalent-fields'),el.checked);}
    else if(el.name.startsWith('detail-language-'))d.language=el.value;
    else if(el.name.startsWith('period-')){d.period=el.value;card.querySelector('.custom-dates').hidden=el.value!=='custom';}
    else if(el.name.startsWith('operation-'))d.operation=el.value;
    invalidateReview();clearError();
  }
});
main.addEventListener('figma-select-change', async event => {
  if(state.busy)return;
  const {id,context,value}=event.detail;
  if(id==='embassy'){state.draft.embassy=value;invalidateReview();}
  else if(id==='dashboard-embassy'){if(portalState())return;state.embassy=value;state.page=1;state.filter='all';state.query='';render();}
  else if(id.startsWith('status-')&&context){setBusy(true);try{await updateStatus(context,value);refreshDashboard();toast('Status updated.');}catch(error){refreshDashboard();toast(error.message);}finally{setBusy(false);}}
});
installSelects();
route();
if(backendEnabled){
  setInterval(()=>{if(document.hidden)return;if(['orders','payments','embassy'].includes(state.route))syncOrders();if(state.route==='embassy')syncPortal();},5000);
  window.addEventListener('focus',()=>{syncOrders();syncPortal();});
}
