import { icon, escapeHtml as esc } from './utils.js';
import { button } from './components.js';
import { documentHtml } from './views/document.js';
const dialog=document.querySelector('#modal');let returnFocus;let toastTimer;let currentDocument=null;
export function toast(message){const t=document.querySelector('#toast');clearTimeout(toastTimer);t.textContent=message;t.classList.add('visible');toastTimer=setTimeout(()=>t.classList.remove('visible'),3800);}
export function openModal(title,body,{wide=false,footer=''}={}){
  returnFocus=document.activeElement;
  dialog.className=wide?'modal-document':'';
  dialog.innerHTML=`<div class="modal-heading"><h2 id="modal-title" tabindex="-1">${title}</h2><button type="button" class="icon-button" data-action="close-modal" aria-label="Bağla">${icon('x')}</button></div><div class="modal-body">${body}</div>${footer?`<div class="modal-footer">${footer}</div>`:''}`;
  dialog.showModal();dialog.querySelector('#modal-title').focus({preventScroll:true});
}
export function closeModal(){dialog.close();if(returnFocus?.isConnected)returnFocus.focus();}
dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeModal();}});
export function showDocument(order){currentDocument=structuredClone(order);openModal('Sənədə baxış',documentHtml(currentDocument),{wide:true,footer:button(`${icon('download')} PDF yüklə`,'download-pdf')+button(`${icon('printer')} Çap et`,'print','secondary')});}
export const getCurrentDocument=()=>currentDocument;
export function finHelp(){openModal('FİN kod nədir?',`<p>FİN — Fərdi İdentifikasiya Nömrəsidir. Şəxsiyyət vəsiqəsində göstərilən 7 simvollu unikal koddur.</p><img class="fin-guide" src="./assets/fin-guide.jpeg" alt="Yeni və köhnə şəxsiyyət vəsiqələrində FİN kodun yerləşməsi">`);}
export function phoneHelp(){openModal('Nömrəni necə yeniləyə bilərəm?',`<p>Qeydiyyat nömrəsinə çıxışınız yoxdursa, nömrənin yenilənməsi üçün ABB ilə əlaqə saxlayın.</p><ol class="help-list"><li>Şəxsiyyət vəsiqənizi hazırlayın.</li><li>Əlaqə məlumatlarının yenilənməsi üçün bankın dəstəyinə və ya filiala müraciət edin.</li><li>Yenilənmə təsdiqləndikdən sonra sifarişə davam edin.</li></ol>`,{footer:button('Anladım','close-modal')});}
