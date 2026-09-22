import { icon, artwork, escapeHtml as esc } from './utils.js';
import { button } from './components.js';
import { documentHtml } from './views/document.js';
const dialog=document.querySelector('#modal');let returnFocus;let toastTimer;
export function toast(message){const t=document.querySelector('#toast');clearTimeout(toastTimer);t.textContent=message;t.classList.add('visible');toastTimer=setTimeout(()=>t.classList.remove('visible'),3800);}
export function openModal(title,body,{wide=false,footer=''}={}){
  returnFocus=document.activeElement;
  dialog.className=wide?'modal-document':'';
  dialog.innerHTML=`<div class="modal-heading"><h2 id="modal-title" tabindex="-1">${title}</h2>${wide?'':`<button type="button" class="icon-button" data-action="close-modal" aria-label="Bağla">${icon('x')}</button>`}</div><div class="modal-body">${body}</div>${footer?`<div class="modal-footer">${footer}</div>`:''}`;
  dialog.showModal();dialog.querySelector('#modal-title').focus({preventScroll:true});
}
export function closeModal(){dialog.close();if(returnFocus?.isConnected)returnFocus.focus();}
dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeModal();}});
export function showDocument(order){openModal('Sənədə baxış',documentHtml(structuredClone(order)),{wide:true,footer:button('Close','close-modal','secondary')});}
export function finHelp(){openModal('<span class="help-dot">?</span> FİN kodu nədir?',`<p>FİN hər Azərbaycan vətəndaşına verilən, hərf və rəqəmlərdən ibarət 7 simvollu unikal koddur. Şəxsiyyət vəsiqəsinin üzündə, my.gov.az, emas.az portallarında istifadə olunur.</p><p class="fin-caption">FİN — Fərdi İdentifikasiya Nömrəsi</p><div class="fin-guide-pair"><img src="./assets/figma/fin-front.png" width="223" height="158" alt="Yeni şəxsiyyət vəsiqəsində FİN kodunun yerləşməsi"><img src="./assets/figma/fin-back.png" width="223" height="158" alt="Köhnə şəxsiyyət vəsiqəsində FİN kodunun yerləşməsi"></div>`);dialog.classList.add('fin-modal');}
export function phoneHelp(){openModal('Nömrəni necə yeniləyə bilərəm?',`<p>Təhlükəsizlik səbəbindən OTP təsdiqi üçün istifadə olunan mobil nömrə yalnız bank filialında, şəxsiyyət vəsiqənizi təqdim etməklə dəyişdirilə bilər. Bu, hesabınızın icazəsiz şəxslər tərəfindən ələ keçirilməsinin qarşısını alır.</p><ol class="help-list"><li>Ən yaxın ABB filialına şəxsiyyət vəsiqənizlə gedin</li><li>Əlaqə məlumatlarının yenilənməsi formasını doldurun</li><li>Yenilənmə təsdiqləndikdən sonra sifarişə davam edin</li></ol>`,{footer:button('Anladım','close-modal')});dialog.classList.add('phone-modal');dialog.insertAdjacentHTML('afterbegin',artwork('phone-attention.svg','phone-attention',64));}
