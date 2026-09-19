export const escapeHtml = (value='') => String(value).replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const icon = (name, cls='') => `<svg class="icon ${cls}" aria-hidden="true" focusable="false"><use href="./assets/icons.svg#${name}"></use></svg>`;
export const money = (amount,currency='AZN') => `${new Intl.NumberFormat('az-AZ',{minimumFractionDigits:2,maximumFractionDigits:2}).format(amount)} ${currency}`;
export const dateLabel = date => new Intl.DateTimeFormat('az-AZ',{day:'2-digit',month:'2-digit',year:'numeric'}).format(new Date(date));
export const today = () => { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
export const cleanCode = (value,numeric=false) => value.toUpperCase().replace(numeric ? /[^0-9]/g : /[^A-Z0-9]/g,'');
export const validFin = value => /^[A-Z0-9]{7}$/.test(value);
export const validOtp = value => /^\d{6}$/.test(value);
export function validCard(value) {
  const n=value.replace(/\s/g,''); if (!/^\d{16}$/.test(n)) return false;
  let sum=0; for(let i=n.length-1,alt=false;i>=0;i--,alt=!alt){let x=+n[i];if(alt){x*=2;if(x>9)x-=9;}sum+=x;} return sum%10===0;
}
export function validExpiry(value, now=new Date()) {
  if(!/^(0[1-9]|1[0-2])\/\d{2}$/.test(value)) return false;
  const [m,y]=value.split('/').map(Number); return new Date(2000+y,m,1)>now;
}
export function validRange(start,end,max=today()) {
  const real = d => /^\d{4}-\d{2}-\d{2}$/.test(d) && !Number.isNaN(new Date(d).valueOf()) && new Date(d).toISOString().slice(0,10)===d;
  return real(start)&&real(end)&&start<=end&&end<=max;
}
export function periodRange(detail, now=today()) {
  if(detail.period==='custom') return [detail.start,detail.end];
  const end=new Date(now+'T12:00:00');const start=new Date(end); const day=start.getDate();
  start.setDate(1);start.setMonth(start.getMonth()-Number(detail.period||1));
  start.setDate(Math.min(day,new Date(start.getFullYear(),start.getMonth()+1,0).getDate()));
  const fmt=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  return [fmt(start),fmt(end)];
}
