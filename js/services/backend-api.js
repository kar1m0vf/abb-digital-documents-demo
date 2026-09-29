/** Adapter for the local Spring presentation profile. No markup or card data crosses this boundary. */
export function createBackendApi({baseUrl='/api/v1',fetchImpl=(...args)=>fetch(...args),onOrder=()=>{},onOrders=()=>{},onAccounts=()=>{},timeout=10000}={}) {
  let token='', revision=0;
  const challenges=new Map(), submissions=new Map();
  async function request(path,{method='GET',body,authorized=false}={}) {
    const controller=new AbortController();
    const timer=setTimeout(()=>controller.abort(),timeout);
    try {
      const response=await fetchImpl(`${baseUrl}${path}`,{method,signal:controller.signal,cache:'no-store',headers:{Accept:'application/json','Accept-Language':'az',...(body?{'Content-Type':'application/json'}:{}),...(authorized&&token?{Authorization:`Bearer ${token}`}:{})},...(body?{body:JSON.stringify(body)}:{})});
      let data;try{data=await response.json();}catch{throw new Error('Server cavabı oxunmadı. Yenidən cəhd edin.');}
      if(!response.ok) {
        if(response.status===401)token='';
        const messages={PAYMENT_FAILED:'Ödəniş rədd edildi. Başqa kartla yenidən cəhd edin.',INVALID_OTP:'Kod düzgün deyil və ya müddəti bitib. Yenidən cəhd edin.',CUSTOMER_NOT_FOUND:'FİN kodu üzrə məlumat tapılmadı. Kodu yoxlayın.',UNAUTHORIZED:'Sessiya bitib. FİN kodunu yenidən təsdiqləyin.',CONFLICT:'Sifariş məlumatları dəyişib. Yeni sifariş yaradın.'};
        throw new Error(messages[data.code]||data.message||'Sorğu yerinə yetirilmədi. Yenidən cəhd edin.');
      }
      return data;
    } catch(error) {
      if(error.name==='AbortError'||error instanceof TypeError)throw new Error('Serverə qoşulmaq mümkün olmadı. Yenidən cəhd edin.');
      throw error;
    } finally {clearTimeout(timer);}
  }
  async function requestOtp(fin) {
    const customer=await request('/auth/fin/verify',{method:'POST',body:{fin}});
    const result=await request('/auth/otp/send',{method:'POST',body:{customerId:customer.customerId}});
    const id=crypto.randomUUID();challenges.clear();challenges.set(id,customer);
    return {id,phone:result.sentTo,resendAt:Date.now()+result.expiresInSeconds*1000};
  }
  async function verifyOtp(id,code) {
    const customer=challenges.get(id);
    if(!customer)throw new Error('Kodun müddəti bitib. Yeni kod tələb edin.');
    // Keep the verified token during an account-fetch retry; OTPs are one-use.
    if(!customer.verifiedToken) {
      const auth=await request('/auth/otp/validate',{method:'POST',body:{customerId:customer.customerId,otp:code}});
      customer.verifiedToken=auth.accessToken;
    }
    token=customer.verifiedToken;
    const products=await request(`/customers/${customer.customerId}/accounts`,{authorized:true});
    if(!Array.isArray(products.accounts))throw new Error('Hesab məlumatları oxunmadı. Yenidən cəhd edin.');
    onAccounts(products.accounts);
    challenges.delete(id);
    return {name:customer.fullName,phone:customer.phoneMasked,initials:customer.fullName.split(/\s+/).filter(Boolean).slice(0,2).map(part=>part[0]).join('')};
  }
  async function refreshOrders() {
    const startedAt=revision;
    const result=await request('/demo/orders');
    if(!Array.isArray(result))throw new Error('Sifariş məlumatları oxunmadı. Yenidən cəhd edin.');
    if(startedAt===revision)onOrders(result);return result;
  }
  async function submitOrder(draft,paymentToken,idempotencyKey) {
    if(!token)throw new Error('Sessiya bitib. FİN kodunu yenidən təsdiqləyin.');
    if(submissions.has(idempotencyKey))return submissions.get(idempotencyKey);
    const pending=request('/demo/orders',{method:'POST',authorized:true,body:{draft:structuredClone(draft),paymentToken,idempotencyKey}}).then(order=>{revision++;onOrder(order);return order;});
    submissions.set(idempotencyKey,pending);
    try{return await pending;}catch(error){submissions.delete(idempotencyKey);throw error;}
  }
  async function updateStatus(id,status) {
    const result=await request(`/demo/orders/${encodeURIComponent(id)}/status`,{method:'PUT',body:{status}});
    revision++;onOrder(result);return true;
  }
  function logout(){token='';challenges.clear();submissions.clear();}
  return {requestOtp,verifyOtp,submitOrder,refreshOrders,updateStatus,logout};
}
