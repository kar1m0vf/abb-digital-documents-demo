import {periodRange,today} from '../utils.js';
// Synthetic relative dates keep the demo useful without any bank connection.
const samples=[
  [5,'expense',42.50,'Kartla ödəniş','Card purchase'],
  [12,'income',1500,'Əməkhaqqı','Salary payment'],
  [22,'expense',85,'Kommunal ödəniş','Utility payment'],
  [45,'income',300,'Hesaba köçürmə','Incoming transfer'],
  [80,'expense',124.90,'Kartla ödəniş','Card purchase'],
  [140,'income',500,'Hesaba köçürmə','Incoming transfer'],
  [220,'expense',65,'Xidmət ödənişi','Service payment'],
  [320,'income',200,'Hesaba köçürmə','Incoming transfer']
];
export function transactionsFor(detail,asOf=today()){
  const [start,end]=periodRange(detail,asOf);
  return samples.map(([days,type,amount,az,en])=>{const d=new Date(asOf+'T12:00:00');d.setDate(d.getDate()-days);const date=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;return {date,type,amount,az,en};})
    .filter(t=>t.date>=start&&t.date<=end&&(!detail.operation||detail.operation==='all'||t.type===detail.operation))
    .sort((a,b)=>a.date.localeCompare(b.date));
}
