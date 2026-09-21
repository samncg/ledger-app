/* ═══════════════════════════════════════════
   APP LOCK — PIN (salted hash) + optional WebAuthn
   The raw PIN is never stored; only a salted hash. If SubtleCrypto
   is unavailable a weak fallback hash keeps the PIN working.
   ═══════════════════════════════════════════ */
const LOCK_KEY='ledger-lock';

const toHex=buf=>Array.from(new Uint8Array(buf)).map(b=>b.toString(16).padStart(2,'0')).join('');
const fallbackHash=s=>{let h=5381;for(let i=0;i<s.length;i++)h=((h<<5)+h+s.charCodeAt(i))>>>0;return 'w'+h.toString(16)};
const randomBytes=n=>{
  try{if(typeof crypto!=='undefined'&&crypto.getRandomValues)return crypto.getRandomValues(new Uint8Array(n))}catch(e){}
  const a=new Uint8Array(n);for(let i=0;i<n;i++)a[i]=Math.floor(Math.random()*256);return a;
};

export const hashPin=async(pin,salt)=>{
  const data=salt+':'+pin;
  try{
    if(typeof crypto!=='undefined'&&crypto.subtle&&typeof TextEncoder!=='undefined'){
      const buf=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(data));
      return toHex(buf);
    }
  }catch(e){/* fall through to the weak hash */}
  return fallbackHash(data);
};

export const getLock=()=>{try{const v=localStorage.getItem(LOCK_KEY);return v?JSON.parse(v):null}catch(e){return null}};
export const clearLock=()=>{try{localStorage.removeItem(LOCK_KEY)}catch(e){}};

export const setLock=async pin=>{
  const salt=toHex(randomBytes(16));
  const hash=await hashPin(pin,salt);
  const prev=getLock()||{};
  const rec={salt,hash,biometric:prev.biometric||null};
  try{localStorage.setItem(LOCK_KEY,JSON.stringify(rec))}catch(e){}
  return rec;
};

export const verifyPin=async pin=>{
  const rec=getLock();
  if(!rec||!rec.salt||!rec.hash)return false;
  const hash=await hashPin(pin,rec.salt);
  return hash===rec.hash;
};

/* WebAuthn is best-effort — every call is wrapped so a missing/failed
   authenticator simply falls back to the PIN. */
export const biometricAvailable=()=>{
  try{return typeof window!=='undefined'&&!!window.PublicKeyCredential&&!!(navigator&&navigator.credentials)}catch(e){return false}
};
export const hasRegisteredBiometric=()=>{const r=getLock();return !!(r&&r.biometric)};

export const registerBiometric=async()=>{
  if(!biometricAvailable())return false;
  const challenge=randomBytes(32);
  const cred=await navigator.credentials.create({publicKey:{
    challenge,
    rp:{name:'Ledger'},
    user:{id:randomBytes(16),displayName:'Ledger user',name:'ledger'},
    pubKeyCredParams:[{type:'public-key',alg:-7},{type:'public-key',alg:-257}],
    authenticatorSelection:{authenticatorAttachment:'platform',userVerification:'preferred'},
    timeout:60000,
  }});
  if(!cred)return false;
  const id=btoa(String.fromCharCode(...new Uint8Array(cred.rawId)));
  const rec=getLock()||{};
  rec.biometric=id;
  try{localStorage.setItem(LOCK_KEY,JSON.stringify(rec))}catch(e){}
  return true;
};

export const unlockWithBiometric=async()=>{
  const rec=getLock();
  if(!rec||!rec.biometric||!biometricAvailable())return false;
  const raw=Uint8Array.from(atob(rec.biometric),c=>c.charCodeAt(0));
  const res=await navigator.credentials.get({publicKey:{
    challenge:randomBytes(32),
    allowCredentials:[{type:'public-key',id:raw}],
    userVerification:'preferred',
    timeout:60000,
  }});
  return !!res;
};
