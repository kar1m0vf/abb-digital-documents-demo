import { documentHtml } from '../views/document.js';

/** Local, offline PDF export. Canvas renders Azerbaijani glyphs reliably without
 * shipping unlicensed design fonts. Pages are embedded at 2x A4 resolution.
 * pdf-lib is lazy-loaded only when the user requests a download.
 */
export async function downloadPdf(order){
  const { PDFDocument }=await import('../../vendor/pdf-lib.esm.min.js');
  const pdf=await PDFDocument.create();pdf.setTitle('ABB — Account document');pdf.setSubject('Account information');
  const wrapper=document.createElement('div');wrapper.innerHTML=documentHtml(order);const paper=wrapper.querySelector('.paper');
  const WIDTH=1190,HEIGHT=1684,MARGIN=85;let canvas,ctx,y;const pages=[];
  function newPage(){canvas=document.createElement('canvas');canvas.width=WIDTH;canvas.height=HEIGHT;ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,WIDTH,HEIGHT);y=MARGIN;pages.push(canvas);}
  newPage();
  function ensure(height){if(y+height>HEIGHT-95)newPage();}
  function lines(text,width,size=21,bold=false){ctx.font=`${bold?'600':'400'} ${size}px "Segoe UI", Arial, sans-serif`;const result=[];let row='';for(const word of text.split(/\s+/)){const next=row?row+' '+word:word;if(ctx.measureText(next).width>width&&row){result.push(row);row=word;}else row=next;
    while(ctx.measureText(row).width>width){let n=row.length-1;while(n>1&&ctx.measureText(row.slice(0,n)).width>width)n--;result.push(row.slice(0,n));row=row.slice(n);}
  }if(row)result.push(row);return result;}
  function textBlock(text,{size=21,bold=false,color='#1d2c46',gap=18,width=WIDTH-MARGIN*2}={}){const rows=lines(text,width,size,bold);for(const row of rows){ensure(size*1.6);ctx.font=`${bold?'600':'400'} ${size}px "Segoe UI", Arial, sans-serif`;ctx.fillStyle=color;ctx.fillText(row,MARGIN,y+size);y+=size*1.5;}y+=gap;}
  const logo=new Image();logo.src=new URL('../../assets/abb-logo.png',import.meta.url).href;await logo.decode();ctx.drawImage(logo,MARGIN,y,180,88);y+=110;
  for(const el of paper.children){
    if(el.tagName==='IMG')continue;
    if(el.tagName==='TABLE'){
      for(const row of el.querySelectorAll('tr')){const cells=[...row.children];const total=WIDTH-MARGIN*2;const widths=cells.length===2?[350,total-350]:[180,total-580,200,200];const wrapped=cells.map((c,i)=>lines(c.textContent,widths[i]-28,19,c.tagName==='TH'));const h=Math.max(...wrapped.map(a=>a.length))*29+26;ensure(h);let x=MARGIN;
        cells.forEach((cell,i)=>{ctx.fillStyle=cell.tagName==='TH'?'#f3f6fa':'#fff';ctx.fillRect(x,y,widths[i],h);ctx.strokeStyle='#d5deec';ctx.lineWidth=1;ctx.strokeRect(x,y,widths[i],h);ctx.fillStyle='#1d2c46';ctx.font=`${cell.tagName==='TH'?'600':'400'} 19px "Segoe UI", Arial, sans-serif`;wrapped[i].forEach((t,j)=>ctx.fillText(t,x+14,y+31+j*29));x+=widths[i];});y+=h;
      }y+=20;continue;
    }
    textBlock(el.textContent,{size:el.tagName==='H2'?30:el.tagName==='H3'?24:el.classList.contains('paper-meta')?18:21,bold:['H2','H3'].includes(el.tagName),color:'#1d2c46'});
  }
  for(let i=0;i<pages.length;i++){const c=pages[i],cctx=c.getContext('2d');cctx.font='17px Arial';cctx.fillStyle='#7a879b';cctx.fillText(`ABB · ${i+1} / ${pages.length}`,MARGIN,HEIGHT-50);const png=await pdf.embedPng(c.toDataURL('image/png'));pdf.addPage([595,842]).drawImage(png,{x:0,y:0,width:595,height:842});}
  const blob=new Blob([await pdf.save()],{type:'application/pdf'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`${order.id||'ABB-document'}.pdf`;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);
}
