export const documents = {
  statement: { title: 'Hesabdan Çıxarış', price: 5, icon: 'document', description: 'Seçdiyiniz hesabdan dövri çıxarış səfirliyə, digər qurumlara və ya şəxsi istifadə üçün' },
  reference: { title: 'Səfirliyə arayış', price: 10, icon: 'plane', description: 'Seçdiyiniz hesabdan dövri çıxarış səfirliyə, digər qurumlara və ya şəxsi istifadə üçün' }
};
export const embassies = [
  { id: 'italy', name: 'İtalya səfirliyi', en: 'Embassy of Italy' },
  { id: 'france', name: 'Fransa səfirliyi', en: 'Embassy of France' },
  { id: 'usa', name: 'ABŞ səfirliyi', en: 'Embassy of the United States' },
  { id: 'germany', name: 'Almaniya səfirliyi', en: 'Embassy of Germany' },
  { id: 'spain', name: 'İspaniya səfirliyi', en: 'Embassy of Spain' },
  { id: 'uk', name: 'Böyük Britaniya səfirliyi', en: 'British Embassy' }
];
export const demoCustomer = { name: 'Aydan Əhədova', initials: 'AƏ', phone: '+994 50 *** ** 00' };
export const MAX_SELECTED_ACCOUNTS = 3;
export const accounts = [
  { id:'visa-azn', name:'Tam Visa', type:'card', currency:'AZN', last4:'7575', number:'AZ•• •••• •••• •••• •••• 7575', balance:2450.80, image:'card-visa.jpeg' },
  { id:'master-azn', name:'Tam Mastercard', type:'card', currency:'AZN', last4:'4581', number:'AZ•• •••• •••• •••• •••• 4581', balance:680.25, image:'card-mastercard.jpeg' },
  { id:'visa-usd', name:'Visa USD', type:'card', currency:'USD', last4:'9032', number:'AZ•• •••• •••• •••• •••• 9032', balance:1200, image:'card-visa.jpeg' },
  { id:'credit', name:'Kredit kartı', type:'card', currency:'AZN', last4:'1084', number:'AZ•• •••• •••• •••• •••• 1084', balance:-350, image:'card-mastercard.jpeg', disabled:true },
  { id:'account-azn', name:'Cari hesab', type:'account', currency:'AZN', last4:'2156', number:'AZ•• •••• •••• •••• •••• 2156', balance:5230.50 },
  { id:'account-usd', name:'Cari hesab', type:'account', currency:'USD', last4:'3860', number:'AZ•• •••• •••• •••• •••• 3860', balance:3400 },
  { id:'account-eur', name:'Cari hesab', type:'account', currency:'EUR', last4:'6241', number:'AZ•• •••• •••• •••• •••• 6241', balance:1850 }
];
export const steps = ['Sənəd növü', 'Səfirlik məlumatları', 'Hesab seçimi', 'Çıxarış detalları', 'Yoxlama', 'Ödəniş', 'Təsdiq'];
export const periods = {1:'Son 1 ay',3:'Son 3 ay',6:'Son 6 ay',12:'Son 1 il',custom:'Tarix seçimi'};
export const operations = {all:'Hər ikisi',income:'Mədaxil',expense:'Məxaric'};
export const statuses = {pending:{az:'Gözləyir',en:'Pending',icon:'clock'},completed:{az:'Tamamlandı',en:'Completed',icon:'circle-check'},rejected:{az:'İmtina edildi',en:'Rejected',icon:'circle-x'}};
export const seedInquiries = [
  ['000512','Aydan Əhədova','reference','completed','2024-05-31'],
  ['000489','Elvin Məmmədov','statement','pending','2024-05-28'],
  ['000471','Tural Hüseynov','reference','pending','2024-05-25'],
  ['000125','Nigar Əliyeva','reference','completed','2024-05-28'],
  ['000124','Rəşad Quliyev','statement','rejected','2024-05-27'],
  ['000118','Leyla Həsənova','reference','completed','2024-05-26'],
  ['000103','Samir Nəsirov','statement','pending','2024-05-24'],
  ['000098','Fidan Abbasova','reference','completed','2024-05-23'],
  ['000091','Orxan Qəhrəmanov','reference','rejected','2024-05-22'],
  ['000087','Günay Məmmədli','reference','pending','2024-05-21']
].map(([id,customer,type,status,date])=>({id:`AR-2024-${id}`,customer,type,status,date,embassy:'italy',language:'en',accounts:['visa-azn'],details:{},price:documents[type].price,seed:true}));
