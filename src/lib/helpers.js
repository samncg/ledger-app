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
/* String-safe id tie-breaker — ids are strings, so b.id-a.id would be NaN. */
export const cmpId=(a,b)=>a.id<b.id?-1:a.id>b.id?1:0;

/* ─── Daily spend streak ───
   Consecutive days with at least one logged expense. `current` stays alive until a
   whole day is missed (so today not being logged yet doesn't break it); `best` is the
   longest run ever. `grace` forgives that many missed days inside a single run
   (a "streak grace day"). */
export const spendStreaks=(expenses,today,grace=0)=>{
  const skip=Math.max(0,Math.floor(grace)||0);
  const days=new Set();
  for(const e of expenses||[]){if(e&&e.date)days.add(e.date)}
  const loggedToday=days.has(today);
  let current=0;
  let d=loggedToday?today:addDays(today,-1);
  let misses=0;
  while(true){
    if(days.has(d)){current++;d=addDays(d,-1)}
    else if(misses<skip){misses++;d=addDays(d,-1)}
    else break;
  }
  let best=0;
  for(const day of days){
    if(days.has(addDays(day,1)))continue; // only measure from the end of a run
    let n=0,cur=day,m=0;
    while(true){
      if(days.has(cur)){n++;cur=addDays(cur,-1)}
      else if(m<skip){m++;cur=addDays(cur,-1)}
      else break;
    }
    if(n>best)best=n;
  }
  return {current,best,loggedToday};
};

/* Cards renamed since a stored layout was saved, mapped to their current id so the
   card keeps the position the user put it in. */
const LEGACY_CARD_IDS={trophies:'streak'};
/* Merge a stored card order with the known cards: keep the user's order, drop unknown
   ids, and append cards added since — so shipping a new card never resets a layout. */
export const mergeCardOrder=(stored,known)=>{
  const list=(Array.isArray(stored)?stored:[]).map(id=>LEGACY_CARD_IDS[id]||id);
  const kept=list.filter((id,i)=>known.includes(id)&&list.indexOf(id)===i);
  return [...kept,...known.filter(id=>!kept.includes(id))];
};

/* Tags are free-form labels on a spend (max 8 per entry). A leading '#' is optional and
   stripped, and duplicates are dropped so the same tag can't be added twice. */
export const cleanTags = tags => Array.isArray(tags)
  ? [...new Set(tags.map(t => String(t).trim().replace(/^#+/, '')).filter(Boolean))].slice(0, 8)
  : [];
/* Enforce single-category selection across all expenses */
export const normalizeExpense = e => {
  if (!e) return e;
  const cat = (Array.isArray(e.categories) && e.categories.length ? e.categories[0] : e.category) || 'food';
  return { ...e, category: cat, categories: [cat], tags: cleanTags(e.tags) };
};
/* Drop falsy / malformed entries so a bad import or cloud copy can never crash
   the renderer (e.g. spentByDay reading amount off null). */
export const normalizeExpenses = list => Array.isArray(list)
  ? list.filter(e=>e&&typeof e==='object'&&Number.isFinite(e.amount)&&typeof e.date==='string'&&e.date.trim()!=='').map(normalizeExpense)
  : [];
export const expCats = e => {
  const cat = (Array.isArray(e.categories) && e.categories.length ? e.categories[0] : e.category) || 'food';
  return [cat];
};
/* ─── Smart category suggestions ───
   Keyword heuristics seeded from the user's real data patterns. Extend freely:
   first matching category wins, keyed by category id. ─── */
export const CATEGORY_KEYWORDS={
  food:["rice","latte","milk","bread","coffee","matcha","curry","poke","subway","candy","water","chocolate"],
  transport:["erl","mrt","grab","ride","lrt","bus","toll","petrol"],
  shopping:["cable","usb","mr diy","stationary","paper","print","pen","drive"],
  other:["laundry","deepseek","subscription","bill"],
};
export const suggestCategory=(text,cats,memory)=>{
  const t=(text||'').toLowerCase().trim();
  if(!t)return null;
  const ids=cats&&cats.length?cats.map(c=>c.id):Object.keys(CATEGORY_KEYWORDS);
  const ok=id=>ids.includes(id);
  // 1. A note you've logged (exactly) before wins.
  const exact=memory&&memory.notes&&memory.notes[t];
  if(exact&&ok(exact))return exact;
  // 2. Personal vocabulary learned from your own notes.
  if(memory&&memory.tokens){
    const hits={};
    for(const tok of tokenizeNote(t)){
      const c=memory.tokens[tok];
      if(c&&ok(c))hits[c]=(hits[c]||0)+1;
    }
    let best=null;
    for(const[c,n]of Object.entries(hits))if(best===null||n>hits[best])best=c;
    if(best)return best;
  }
  // 3. Built-in keyword fallback.
  for(const[id,words]of Object.entries(CATEGORY_KEYWORDS)){
    if(!ok(id))continue;
    if(words.some(w=>t.includes(w)))return id;
  }
  return null;
};

/* ─── History-aware category memory ───
   Learns note→category (exact) and token→category from the user's own expenses,
   so personal vocabulary works without hand-editing CATEGORY_KEYWORDS. */
const tokenizeNote=n=>n.split(/[^a-z0-9]+/).filter(Boolean);
const voteInto=(map,key,cat,date)=>{
  if(!key||!cat)return;
  const slot=map[key]||(map[key]={});
  const v=slot[cat]||(slot[cat]={count:0,last:''});
  v.count++;
  if(date&&date>v.last)v.last=date;
};
const bestVote=slot=>{
  let best=null;
  for(const[cat,v]of Object.entries(slot)){
    if(!best||v.count>best.count||(v.count===best.count&&v.last>best.last))best={cat,count:v.count,last:v.last};
  }
  return best?best.cat:null;
};
export const buildCategoryMemory=expenses=>{
  const notes={},tokens={};
  for(const e of(expenses||[])){
    if(!e)continue;
    const cat=(Array.isArray(e.categories)&&e.categories.length?e.categories[0]:e.category)||'';
    const n=(e.note||'').trim().toLowerCase().replace(/\s+/g,' ');
    if(!n||!cat)continue;
    voteInto(notes,n,cat,e.date);
    for(const t of tokenizeNote(n))voteInto(tokens,t,cat,e.date);
  }
  const resolve=m=>{const out={};for(const[k,slot]of Object.entries(m)){const c=bestVote(slot);if(c)out[k]=c;}return out;};
  return{notes:resolve(notes),tokens:resolve(tokens)};
};

/* Downscale an image File to a small JPEG data URL (receipt photos). */
export const downscaleImage=(file,maxDim=600,quality=0.6)=>new Promise((resolve,reject)=>{
  const reader=new FileReader();
  reader.onerror=()=>reject(new Error('Could not read file'));
  reader.onload=()=>{
    const img=new Image();
    img.onerror=()=>reject(new Error('Could not decode image'));
    img.onload=()=>{
      try{
        const longest=Math.max(img.width||maxDim,img.height||maxDim);
        const scale=Math.min(1,maxDim/Math.max(1,longest));
        const w=Math.max(1,Math.round((img.width||maxDim)*scale));
        const h=Math.max(1,Math.round((img.height||maxDim)*scale));
        const canvas=document.createElement('canvas');
        canvas.width=w;canvas.height=h;
        const ctx=canvas.getContext('2d');
        ctx.drawImage(img,0,0,w,h);
        resolve(canvas.toDataURL('image/jpeg',quality));
      }catch(e){reject(e)}
    };
    img.src=reader.result;
  };
  reader.readAsDataURL(file);
});

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
  // Parse bare yyyy-MM-dd strings as LOCAL time — new Date('yyyy-MM-dd') is UTC
  // and can land a day early in UTC-negative zones, re-adding a recurring entry.
  const nd=new Date(typeof d==='string'?d+"T00:00:00":d);
  if(freq==='weekly')nd.setDate(nd.getDate()+7);
  else if(freq==='monthly'){
    const day=nd.getDate();
    nd.setMonth(nd.getMonth()+1);
    if(nd.getDate()<day)nd.setDate(0); // clamp (e.g. 31st → last day of month)
  }else nd.setDate(nd.getDate()+1);
  return nd;
};
