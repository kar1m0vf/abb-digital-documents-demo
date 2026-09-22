/** Demo adapter. Replace these methods with calls to your authenticated backend.
 * No real FIN, OTP, card details or bank requests are sent by this prototype.
 * Payment accepts a demo token, never raw card data.
 */
import { demoCustomer, documents, MAX_SELECTED_ACCOUNTS } from '../data.js';
import { validFin, validOtp } from '../utils.js';
const delay = ms => new Promise(resolve=>setTimeout(resolve,ms));
const challenges=new Map();const submissions=new Map();
export const DEMO = Object.freeze({fin:'ABC1234',otp:'123456',card:'4242 4242 4242 4242',declinedCard:'4000 0000 0000 0002',expiry:'12/30',cvv:'123'});
export async function requestOtp(fin){
  await delay(450);
  if(!validFin(fin))throw new Error('FİN kodu hərf və rəqəmlərdən ibarət 7 simvollu olmalıdır.');
  if(fin!==DEMO.fin)throw new Error('FİN kodu üzrə məlumat tapılmadı. Kodu yoxlayın.');
  const id=crypto.randomUUID();challenges.set(id,{attempts:0,expires:Date.now()+300000});
  return {id,phone:demoCustomer.phone,resendAt:Date.now()+60000};
}
export async function verifyOtp(id,code){
  await delay(450);const c=challenges.get(id);
  if(!c||c.expires<Date.now())throw new Error('Kodun müddəti bitib. Yeni kod tələb edin.');
  if(c.attempts>=5)throw new Error('Cəhd limiti dolub. Yeni kod tələb edin.');
  c.attempts++;
  if(!validOtp(code)||code!==DEMO.otp)throw new Error(`Kod düzgün deyil. ${5-c.attempts} cəhd qalıb.`);
  challenges.delete(id);return {...demoCustomer};
}
export async function submitOrder(draft,paymentToken,idempotencyKey){
  if(submissions.has(idempotencyKey))return submissions.get(idempotencyKey);
  const snapshot=structuredClone(draft);
  const pending=(async()=>{
    await delay(800);
    if(paymentToken==='demo-declined')throw new Error('Ödəniş rədd edildi. Başqa kartla yenidən cəhd edin.');
    if(paymentToken!=='demo-success'||!snapshot.reviewed||!snapshot.accounts.length||snapshot.accounts.length>MAX_SELECTED_ACCOUNTS)throw new Error('Sifariş məlumatlarını yoxlayın.');
    return {...snapshot,id:`AR-${new Date().getFullYear()}-${crypto.randomUUID().slice(0,8).toUpperCase()}`,customer:demoCustomer.name,date:new Date().toISOString(),price:documents[snapshot.type].price,status:'pending',paymentStatus:'paid',seed:false};
  })();
  submissions.set(idempotencyKey,pending);
  try{return await pending;}catch(error){submissions.delete(idempotencyKey);throw error;}
}
