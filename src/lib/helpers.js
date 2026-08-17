import { CURRENCIES } from './constants';

/* ═══════════════════════════════════════════
   HELPERS
   ═══════════════════════════════════════════ */
export const fmt=(n,cur)=>{
  const c=CURRENCIES[cur]||CURRENCIES.MYR;
  const sign=n<0?"-":"";
  const abs=Math.abs(n);
  const decimals=c.code==="JPY"?0:2;
  return `${c.symbol} ${sign}${abs.toLocaleString(c.locale,{minimumFractionDigits:decimals,maximumFractionDigits:decimals})}`;
};
export const pad2=n=>String(n).padStart(2,"0");
export const todayKey=(d=new Date())=>`${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}`;
export const addDays=(dateStr,n)=>{const d=new Date(dateStr+"T00:00:00");d.setDate(d.getDate()+n);return todayKey(d)};
export const daysInMonth=(d=new Date())=>new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
export const firstOfMonthKey=(d=new Date())=>`${d.getFullYear()}-${pad2(d.getMonth()+1)}-01`;
export const dayDiff=(a,b)=>{const da=new Date(a+"T00:00:00");const db=new Date(b+"T00:00:00");return Math.round((db-da)/86400000)};
export const relativeDate=(dateStr,today)=>{
  const diff=dayDiff(today,dateStr);
  if(diff===0)return"Today";
  if(diff===-1)return"Yesterday";
  if(diff===1)return"Tomorrow";
  if(diff>-7&&diff<0)return`${Math.abs(diff)}d ago`;
  return dateStr.slice(5).replace("-","/");
};
export const groupLabel=(dateStr,today)=>{
  const diff=dayDiff(today,dateStr);
  if(diff===0)return"Today";
  if(diff===-1)return"Yesterday";
  if(diff>=-6&&diff<0)return"This week";
  const d=new Date(dateStr+"T00:00:00");
  return d.toLocaleDateString(undefined,{month:'long',year:'numeric'});
};
export const uid=()=>Date.now()+Math.random().toString(36).slice(2,6);
/* Effective category list for an expense — new entries carry a `categories`
   array, old synced ones only have a single `category`. */
export const expCats=e=>Array.isArray(e.categories)&&e.categories.length?e.categories:(e.category?[e.category]:[]);
export const store={
  get(k){try{const v=localStorage.getItem(k);return v?JSON.parse(v):null}catch(e){return null}},
  set(k,v){try{localStorage.setItem(k,JSON.stringify(v));return true}catch(e){return false}}
};

export const heatColorVal=c=>!c||c==='transparent'||c==='none'?'#1b1f24':c;

/* Custom Google Fonts — loads the font's stylesheet on demand */
export const loadGoogleFont=(family)=>{
  const linkId=`gf-${family.replace(/\s+/g,'-').toLowerCase()}`;
  if(document.getElementById(linkId))return;
  const link=document.createElement('link');
  link.id=linkId;
  link.rel='stylesheet';
  link.href=`https://fonts.googleapis.com/css2?family=${encodeURIComponent(family).replace(/%20/g,'+')}:wght@300;400;500;600;700;800&display=swap`;
  document.head.appendChild(link);
};
export const customFontStack=name=>`'${name}',system-ui,-apple-system,BlinkMacSystemFont,sans-serif`;

/* Advance a Date by a frequency — used by automations. */
export const advanceDate=(d,freq)=>{
  const nd=new Date(d);
  if(freq==='weekly')nd.setDate(nd.getDate()+7);
  else if(freq==='monthly'){
    const day=nd.getDate();
    nd.setMonth(nd.getMonth()+1);
    if(nd.getDate()<day)nd.setDate(0); // clamp (e.g. 31st → last day of month)
  }else nd.setDate(nd.getDate()+1);
  return nd;
};
