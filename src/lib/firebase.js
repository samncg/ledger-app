import firebase from 'firebase/compat/app';
import 'firebase/compat/auth';
import 'firebase/compat/firestore';
import { store } from './helpers';

/* ═══════════════════════════════════════════
   CLOUD SYNC — Firebase + Google Auth

   To enable sync between devices:
   1. Create a project at https://console.firebase.google.com
   2. Add a web app and paste its config into FIREBASE_CONFIG below.
   3. In Authentication → Sign-in method, enable "Google".
   4. In Authentication → Settings → Authorized domains, add the domain the
      app is served from (e.g. yourusername.github.io).
   5. In Firestore Database, create a database and add these security rules:
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            match /ledger/{uid} {
              allow read, write: if request.auth != null && request.auth.uid == uid;
            }
          }
        }
   6. Serve the app over http(s) — Google sign-in doesn't work on file:// URLs.
      Any static host works (Firebase Hosting, GitHub Pages, `npm run dev`).
   ═══════════════════════════════════════════ */
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const FIREBASE_CONFIG = {
  apiKey: "AIzaSyCyLhXoE5w599tFNd48129sKhKn9sSJHQ0",
  authDomain: "ledger-df5a2.firebaseapp.com",
  projectId: "ledger-df5a2",
  storageBucket: "ledger-df5a2.firebasestorage.app",
  messagingSenderId: "674114677747",
  appId: "1:674114677747:web:054ffcc2110b887a040086",
  measurementId: "G-SFKZ27MNPS"
};
export const FIREBASE_CONFIGURED=!Object.values(FIREBASE_CONFIG).some(v=>String(v).startsWith("PASTE_"));
let fbApp=null,fbAuth=null,fbFS=null;
export const fbInit=()=>{
  if(!FIREBASE_CONFIGURED)return false;
  if(fbApp)return true; // already initialized on a previous call
  if(typeof firebase==="undefined")return false;
  try{
    fbApp=firebase.initializeApp(FIREBASE_CONFIG);
    fbAuth=firebase.auth(fbApp);
    fbFS=firebase.firestore(fbApp);
    return true;
  }catch(e){console.warn("Firebase init failed:",e);return false}
};
export const getFBAuth=()=>fbAuth;
export const getFBFS=()=>fbFS;

/* Per-account timestamp of the last write this device pushed, so a doc that is
   older than what we already have isn't applied over newer local data.
   Uses a versioned key: older builds stored client-clock timestamps, which can
   be skewed (especially on phones) and would permanently block syncing. */
export const getLastSync=uid=>{
  const o=store.get("ledger-synclast2")||{};
  let t=o[uid]||0;
  if(t>Date.now()+60000){delete o[uid];store.set("ledger-synclast2",o);t=0} // impossible future value = stale skew
  return t;
};
export const setLastSync=(uid,t)=>{const o=store.get("ledger-synclast2")||{};o[uid]=t;store.set("ledger-synclast2",o)};
/* Wallpaper / card-panel / piggy-texture / piggy-sound files are base64 and can
   exceed Firestore's 1 MiB document limit, so they — and their settings (dim,
   blur, panel strength) — stay device-local and never sync. */
export const sanitizePrefs=p=>{const clean={...p};delete clean.wallpaper;delete clean.wallpaperDim;delete clean.wallBlur;delete clean.cardPanel;delete clean.cardPanelOpacity;delete clean.piggyTexture;delete clean.piggySoundCustom;return clean};
/* Strip undefined values (Firestore rejects them) — imported backups or older
   entries can carry undefined fields. */
export const firestoreSafe=o=>{
  if(Array.isArray(o))return o.map(firestoreSafe);
  if(o&&typeof o==='object'){
    const out={};
    for(const k of Object.keys(o)){
      const v=firestoreSafe(o[k]);
      if(v!==undefined)out[k]=v;
    }
    return out;
  }
  return o;
};
