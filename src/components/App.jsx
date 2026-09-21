import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import firebase from 'firebase/compat/app';

import { I } from '../lib/icons';
import {
  DEFAULT_CATS, DEFAULT_THEME, DEFAULT_CARD_ORDER, FONT_OPTIONS,
  HEAT_DEFAULT_COLORS, PREF_DEFAULTS, PRESETS,
} from '../lib/constants';
import {
  addDays, advanceDate, buildCategoryMemory, cleanTags, cmpId, customFontStack, dayDiff, daysInMonth, downscaleImage, expCats,
  firstOfMonthKey, fmt, groupLabel, loadGoogleFont, mergeCardOrder, normalizeExpense, normalizeExpenses,
  relativeDate, spendStreaks, store, suggestCategory, todayKey, uid,
} from '../lib/helpers';
import {
  FIREBASE_CONFIGURED, fbInit, firestoreSafe, getFBAuth, getFBFS,
  getLastSync, sanitizePrefs, setLastSync,
} from '../lib/firebase';
import { setLang, t } from '../lib/i18n';
import { fetchRate, rateDecimals } from '../lib/fx';
import { tiltDisable, tiltEnable } from '../lib/tilt';
import { confettiBurst, playCelebrate, playPiggySound } from '../lib/sound';
import {
  biometricAvailable, clearLock, getLock, registerBiometric, setLock, unlockWithBiometric, verifyPin,
} from '../lib/lock';

import TopBar from './TopBar';
import Hero from './Hero';
import TravelHome from './TravelHome';
import SetupCard from './SetupCard';
import BudgetDrawer from './BudgetDrawer';
import MoneyDrawer from './MoneyDrawer';
import CustomizeDrawer from './CustomizeDrawer';
import CommandPalette from './CommandPalette';
import Confirm from './Confirm';
import Toast from './Toast';
import LockScreen from './LockScreen';
import WeatherEffect from './WeatherEffect';
import LogCard from './cards/LogCard';
import BreakdownCard from './cards/BreakdownCard';
import TrendCard from './cards/TrendCard';
import HistoryCard from './cards/HistoryCard';
import PiggyCard from './cards/PiggyCard';
import AutoCard from './cards/AutoCard';
import BackupCard from './cards/BackupCard';
import InsightsCard from './cards/InsightsCard';
import StreakCard from './cards/StreakCard';

/* ═══════════════════════════════════════════
   APP
   ═══════════════════════════════════════════ */
/* Shape-checks shared by cloud-apply and backup-import paths. */
const isPlainObj=o=>!!o&&typeof o==='object'&&!Array.isArray(o);
const validSettings=s=>isPlainObj(s)&&typeof s.monthlyBudget==='number'&&s.monthlyBudget>=0&&Number.isInteger(s.periodDays)&&s.periodDays>=1&&typeof s.startDate==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(s.startDate)&&!isNaN(new Date(s.startDate+'T00:00:00').getTime());
const validExpenses=a=>Array.isArray(a)&&a.every(e=>e&&typeof e==='object'&&Number.isFinite(e.amount)&&typeof e.date==='string'&&e.date.trim()!=='');
const validTopUps=a=>Array.isArray(a)&&a.every(t=>t&&typeof t.amount==='number'&&typeof t.date==='string');
const validRecurring=a=>Array.isArray(a)&&a.every(r=>r&&typeof r.amount==='number'&&typeof r.type==='string');
const validCats=a=>Array.isArray(a)&&a.every(c=>c&&typeof c.id==='string'&&typeof c.label==='string');
const validPiggies=a=>Array.isArray(a)&&a.every(p=>p&&typeof p==='object'&&typeof p.id==='string');

export default function App(){
  const[theme,setTheme]=useState(()=>({...DEFAULT_THEME,...(store.get("ledger-theme")||{})}));
  const[prefs,setPrefs]=useState(()=>({...PREF_DEFAULTS,...(store.get("ledger-prefs")||{})}));
  const[settings,setSettings]=useState(()=>store.get("ledger-settings"));
  const[expenses,setExpenses]=useState(()=>{
    const stored=store.get("ledger-expenses")||[];
    const normalized=normalizeExpenses(stored);
    if(JSON.stringify(stored)!==JSON.stringify(normalized)){
      store.set("ledger-expenses",normalized);
    }
    return normalized;
  });
  const[categories,setCategories]=useState(()=>store.get("ledger-cats")||DEFAULT_CATS);
  const[catBudgets,setCatBudgets]=useState(()=>store.get("ledger-catbudgets")||{});
  const[topUps,setTopUps]=useState(()=>store.get("ledger-topups")||[]);
  const[balance,setBalance]=useState(()=>store.get("ledger-balance")||{start:0});
  const[piggies,setPiggies]=useState(()=>{
    const list=store.get("ledger-piggies");
    if(Array.isArray(list)&&list.length)return list;
    const old=store.get("ledger-piggy");
    if(old&&typeof old==='object'&&typeof old.saved==='number'){
      return[{id:uid(),name:'Piggy bank',target:old.target||0,saved:old.saved||0,texture:null,soundId:'coin',soundCustom:null}];
    }
    return[{id:uid(),name:'Piggy bank',target:0,saved:0,texture:null,soundId:'coin',soundCustom:null}];
  });
  const[activePiggyId,setActivePiggyId]=useState(()=>piggies[0]?.id||'default');
  const[recurring,setRecurring]=useState(()=>store.get("ledger-recurring")||[]);
  const[showSetup,setShowSetup]=useState(false);
  const[showTopUp,setShowTopUp]=useState(false);
  const[moveMode,setMoveMode]=useState("budget");
  const[topUpAmount,setTopUpAmount]=useState("");
  const[topUpNote,setTopUpNote]=useState("");
  const[autoType,setAutoType]=useState("expense");
  const[autoAmount,setAutoAmount]=useState("");
  const[autoCat,setAutoCat]=useState("food");
  const[autoNote,setAutoNote]=useState("");
  const[autoFreq,setAutoFreq]=useState("monthly");
  const[autoStart,setAutoStart]=useState(()=>todayKey());
  const[showDrawer,setShowDrawer]=useState(false);
  const[drawerTab,setDrawerTab]=useState("theme");
  const[showCmd,setShowCmd]=useState(false);
  const[toast,setToast]=useState(null);
  const[confirm,setConfirm]=useState(null);
  const[scrolled,setScrolled]=useState(false);
  const[authUser,setAuthUser]=useState(null);
  const[syncError,setSyncError]=useState(false);
  const[syncErrorMsg,setSyncErrorMsg]=useState("");
  const[lastSyncedAt,setLastSyncedAt]=useState(0);

  const prefsRef=useRef(prefs);useEffect(()=>{prefsRef.current=prefs},[prefs]);
  const pushTimerRef=useRef(null);
  const syncedRef=useRef(null);

  const[amount,setAmount]=useState("");
  const[selCats,setSelCats]=useState(["food"]);
  const category=selCats[0]||"food";
  const toggleSelCat=id=>setSelCats([id]); // single-select — new choice replaces the previous one
  const[note,setNote]=useState("");
  const[entryDate,setEntryDate]=useState(()=>todayKey());
  const[editingId,setEditingId]=useState(null);
  const fileInputRef=useRef(null);
  const addFormRef=useRef(null);
  const[receipt,setReceipt]=useState(null);
  const[tags,setTags]=useState([]);
  const[receiptView,setReceiptView]=useState(null);

  /* App lock — gated by the prefs toggle; a stored salted PIN hash lives in
     localStorage (never the raw PIN). */
  const[lockRec,setLockRec]=useState(()=>getLock());
  const[locked,setLocked]=useState(()=>{const l=getLock();return !!(prefs.appLock&&l&&l.hash)});
  const[lockSetup,setLockSetup]=useState(false);
  const lockAwayRef=useRef(0);

  const[draftBudget,setDraftBudget]=useState("");
  const[draftDays,setDraftDays]=useState(()=>String(daysInMonth()));
  const[draftStartDate,setDraftStartDate]=useState(()=>firstOfMonthKey());
  const[draftCurrency,setDraftCurrency]=useState("MYR");
  const[draftBalance,setDraftBalance]=useState("");

  const[newCatName,setNewCatName]=useState("");
  const[newCatGlyph,setNewCatGlyph]=useState("★");
  const[lastAction,setLastAction]=useState(null); // for undo
  const[draftFontName,setDraftFontName]=useState("");
  const[catBudgetEdit,setCatBudgetEdit]=useState(false);
  const[catBudgetDraft,setCatBudgetDraft]=useState({});

  const today=todayKey();
  /* Keep the i18n module's active language in step with the pref before any child
     renders — children call t() directly rather than taking it as a prop. */
  setLang(prefs.lang||'en');
  const cur=prefs.currency||"MYR";
  const balancesOn=prefs.balancesEnabled!==false;
  const heroMode=balancesOn?(prefs.heroMode==='balance'?'balance':'daily'):'daily';
  const MYR=useCallback(n=>fmt(n,cur),[cur]);

  const cats=useMemo(()=>categories.map(c=>({...c,color:theme.catColors[c.id]||'#7c8896'})),[categories,theme]);

  /* Learn note→category from your own history so the auto-pick improves as you log. */
  const catMemory=useMemo(()=>buildCategoryMemory(expenses),[expenses]);

  /* Auto-pick the category from the note as you type (history-aware). */
  const onNoteChange=v=>{
    setNote(v);
    if(editingId)return;
    const s=suggestCategory(v,cats,catMemory);
    if(s&&s!==selCats[0])setSelCats([s]);
  };

  useEffect(()=>{document.body.style.background=theme.bg;document.body.style.color=theme.text},[theme]);
  useEffect(()=>{
    const onScroll=()=>setScrolled(window.scrollY>16);
    window.addEventListener('scroll',onScroll,{passive:true});
    return()=>window.removeEventListener('scroll',onScroll);
  },[]);

  const showToast=useCallback((msg,type='info',action=null)=>{
    const id=Date.now();
    setToast({msg,type,id,action});
    setTimeout(()=>setToast(t=>t&&t.id===id?null:t),action?6000:3500);
  },[]);

  const persistExpenses=useCallback(n=>{setExpenses(n);store.set("ledger-expenses",n)},[]);
  const persistSettings=useCallback(n=>{setSettings(n);store.set("ledger-settings",n)},[]);

  /* Auto-sync the budget period to the real length of the current month. */
  useEffect(()=>{
    if(!settings)return;
    const t=new Date(today+"T00:00:00");
    const realDays=daysInMonth(t);
    const firstToday=firstOfMonthKey(t);
    const firstStart=firstOfMonthKey(new Date(settings.startDate+"T00:00:00"));
    const needRealign=firstStart!==firstToday;
    if(needRealign||settings.periodDays!==realDays){
      persistSettings({...settings,periodDays:realDays,startDate:needRealign?firstToday:settings.startDate})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  },[today]);
  const persistTheme=useCallback(n=>{setTheme(n);store.set("ledger-theme",n)},[]);
  const[savedTheme,setSavedTheme]=useState(()=>store.get("ledger-theme-saved")||null);
  const persistSavedTheme=useCallback(n=>{setSavedTheme(n);store.set("ledger-theme-saved",n)},[]);
  const persistCats=useCallback(n=>{setCategories(n);store.set("ledger-cats",n)},[]);
  const persistPrefs=useCallback(n=>{
    setPrefs(n);
    const ok=store.set("ledger-prefs",n);
    if(!ok)showToast(t('toast.storageLimit'),"error");
  },[showToast]);
  const persistCatBudgets=useCallback(n=>{setCatBudgets(n);store.set("ledger-catbudgets",n)},[]);
  const persistTopUps=useCallback(n=>{setTopUps(n);store.set("ledger-topups",n)},[]);
  const persistBalance=useCallback(n=>{setBalance(n);store.set("ledger-balance",n)},[]);
  const persistPiggies=useCallback(n=>{
    setPiggies(n);
    store.set("ledger-piggies",n);
    if(n&&n.length)store.set("ledger-piggy",{target:n[0].target||0,saved:n[0].saved||0});
  },[]);
  const persistRecurring=useCallback(n=>{setRecurring(n);store.set("ledger-recurring",n)},[]);

  /* ─── Cloud sync (Firebase + Google Auth) ─── */
  const stateRef=useRef(null);
  useEffect(()=>{stateRef.current={expenses,topUps,balance,piggies,recurring,settings,categories,catBudgets,prefs,theme}},[expenses,topUps,balance,piggies,recurring,settings,categories,catBudgets,prefs,theme]);
  const savedThemeRef=useRef(savedTheme);useEffect(()=>{savedThemeRef.current=savedTheme},[savedTheme]);
  const skipNextPushRef=useRef(false);

  /* Apply a cloud copy to local state (and localStorage, so it survives offline).
     Every field is shape-checked so a stale or malformed cloud doc can never
     crash the renderer or clobber the local setup with defaults. */
  const applyRemote=useCallback(data=>{
    try{
      if(Array.isArray(data.expenses)&&data.expenses.every(e=>e&&typeof e.amount==='number'&&typeof e.date==='string')){
        const norm=normalizeExpenses(data.expenses);
        setExpenses(norm);store.set("ledger-expenses",norm);
      }
      if(Array.isArray(data.topUps)&&data.topUps.every(t=>t&&typeof t.amount==='number'&&typeof t.date==='string')){setTopUps(data.topUps);store.set("ledger-topups",data.topUps)}
      if(data.balance&&typeof data.balance==='object'&&!Array.isArray(data.balance)&&typeof data.balance.start==='number'){setBalance(data.balance);store.set("ledger-balance",data.balance)}
      if(Array.isArray(data.piggies)&&data.piggies.length){
        // Pushes strip device-local fields (texture / sound), so merge them back
        // by id instead of letting the incoming copy wipe them.
        const local=stateRef.current&&Array.isArray(stateRef.current.piggies)?stateRef.current.piggies:[];
        const merged=data.piggies.map(p=>{
          const prev=local.find(x=>x.id===p.id);
          return{...p,texture:prev?prev.texture:(p.texture??null),soundId:prev?prev.soundId:(p.soundId??'coin'),soundCustom:prev?prev.soundCustom:(p.soundCustom??null)};
        });
        setPiggies(merged);store.set("ledger-piggies",merged);
      }
      else if(data.piggy&&typeof data.piggy==='object'&&!Array.isArray(data.piggy)&&typeof data.piggy.target==='number'&&typeof data.piggy.saved==='number'){
        const p=[{id:'p1',name:'Piggy bank',target:data.piggy.target||0,saved:data.piggy.saved||0,texture:null,soundId:'coin',soundCustom:null}];
        setPiggies(p);store.set("ledger-piggies",p);
      }
      if(Array.isArray(data.recurring)&&data.recurring.every(r=>r&&typeof r.amount==='number'&&typeof r.type==='string')){setRecurring(data.recurring);store.set("ledger-recurring",data.recurring)}
      if(data.settings&&typeof data.settings==='object'&&!Array.isArray(data.settings)&&typeof data.settings.monthlyBudget==='number'&&typeof data.settings.periodDays==='number'&&typeof data.settings.startDate==='string'){setSettings(data.settings);store.set("ledger-settings",data.settings)}
      if(Array.isArray(data.cats)&&data.cats.every(c=>c&&typeof c.id==='string'&&typeof c.label==='string')){setCategories(data.cats);store.set("ledger-cats",data.cats)}
      if(data.catBudgets&&typeof data.catBudgets==='object'&&!Array.isArray(data.catBudgets)){setCatBudgets(data.catBudgets);store.set("ledger-catbudgets",data.catBudgets)}
      if(data.prefs&&typeof data.prefs==='object'&&!Array.isArray(data.prefs)){
        const next={...PREF_DEFAULTS,...prefsRef.current};
        for(const k of Object.keys(data.prefs)){
          if(k==='customFonts'||k==='cardOrder'){if(Array.isArray(data.prefs[k]))next[k]=data.prefs[k]}
          else if(k==='compact'||k==='groupHistory'||k==='catEnabled')next[k]=!!data.prefs[k]
          else if(k==='heroMode')next[k]=data.prefs[k]==='balance'?'balance':'daily'
          else if(k==='balancesEnabled')next[k]=!!data.prefs[k]
          else if(k==='piggySound')next[k]=!!data.prefs[k]
          else if(k==='wallpaper'||k==='wallpaperDim'||k==='wallBlur'||k==='cardPanel'||k==='cardPanelOpacity'||k==='piggyTexture'||k==='piggySoundCustom'||k==='appLock'){/* device-local — never apply from the cloud */}
          else next[k]=data.prefs[k];
        }
        for(const k of ['pieThickness','pieGap','uiBlur','uiOpacity','wallBlur','cardPanelOpacity','wallpaperDim','weatherSpeed']){
          const n=parseFloat(next[k]);if(isFinite(n))next[k]=n;
        }
        setPrefs(next);store.set("ledger-prefs",next);
        (next.customFonts||[]).forEach(f=>{if(f&&f.name)loadGoogleFont(f.name)});
      }
      if(data.theme&&typeof data.theme==='object'&&!Array.isArray(data.theme)){
        const next={...DEFAULT_THEME,...data.theme};
        if(typeof next.bg==='string'&&typeof next.surface==='string'&&typeof next.text==='string'){setTheme(next);store.set("ledger-theme",next)}
      }
      if(data.savedTheme!==undefined&&(data.savedTheme===null||(typeof data.savedTheme==='object'&&!Array.isArray(data.savedTheme)))){setSavedTheme(data.savedTheme);store.set("ledger-theme-saved",data.savedTheme)}
    }catch(e){console.warn("applyRemote failed:",e)}
  },[]);

  /* Push only when the synced state actually differs from the last successful
     push — a byte-level comparison means no-op echoes and re-renders never
     write to Firestore. */
  const lastPushedJsonRef=useRef(null);
  const pushSync=useCallback(uid=>{
    try{
      if(!FIREBASE_CONFIGURED||!getFBFS()||!uid)return;
      const s=stateRef.current||{};
      const payload={
        expenses:firestoreSafe(s.expenses||[]),
        topUps:firestoreSafe(s.topUps||[]),
        balance:firestoreSafe(s.balance||{start:0}),
        piggy:firestoreSafe(s.piggies?.[0]?{target:s.piggies[0].target||0,saved:s.piggies[0].saved||0}:{target:0,saved:0}),
        piggies:firestoreSafe((s.piggies||[]).map(p=>({id:p.id,name:p.name,target:p.target||0,saved:p.saved||0}))),
        recurring:firestoreSafe(s.recurring||[]),
        settings:firestoreSafe(s.settings||null),
        cats:firestoreSafe(s.categories||DEFAULT_CATS),
        catBudgets:firestoreSafe(s.catBudgets||{}),
        prefs:firestoreSafe(sanitizePrefs(s.prefs||{})),
        theme:firestoreSafe(s.theme||{}),
        savedTheme:firestoreSafe(savedThemeRef.current),
      };
      const json=JSON.stringify(payload);
      if(json===lastPushedJsonRef.current)return; // nothing changed since the last write — skip
      // Server timestamps keep ordering consistent across devices (client clocks can drift).
      // NOTE: never pass this sentinel through firestoreSafe — it would be
      // flattened into a plain object and stored as garbage.
      payload.updatedAt=firebase.firestore.FieldValue.serverTimestamp();
      getFBFS().doc('ledger/'+uid).set(payload)
        .then(()=>{lastPushedJsonRef.current=json;setSyncError(false);setSyncErrorMsg("");setLastSyncedAt(Date.now())})
        .catch(e=>{lastPushedJsonRef.current=null;console.warn("Sync push failed:",e);setSyncError(true);setSyncErrorMsg(String(e&&e.message||e))});
    }catch(e){console.warn("Sync push failed:",e);setSyncError(true);setSyncErrorMsg(String(e&&e.message||e))}
  },[]);

  /* Snapshot-driven pushes (repair/seed paths) go through a cooldown so that no
     feedback loop between devices can ever firehose Firestore writes. */
  const lastSyncPushRef=useRef(0);
  const safePushSync=useCallback(uid=>{
    const now=Date.now();
    if(now-lastSyncPushRef.current<3000)return;
    lastSyncPushRef.current=now;
    pushSync(uid);
  },[pushSync]);

  /* Listen for auth changes — sign-in/out drives the whole sync loop. */
  useEffect(()=>{
    if(!FIREBASE_CONFIGURED)return;
    if(!fbInit())return;
    const unsub=getFBAuth().onAuthStateChanged(user=>{
      setAuthUser(user?{uid:user.uid,name:user.displayName,email:user.email,photo:user.photoURL}:null);
      if(!user){setSyncError(false);setSyncErrorMsg("")}
    });
    return ()=>unsub();
  },[]);

  /* Watch the cloud copy: newer remote data wins; missing/older docs get seeded from local. */
  useEffect(()=>{
    if(!FIREBASE_CONFIGURED||!authUser||!fbInit())return;
    const uid=authUser.uid;
    let unsub=null;
    try{
      unsub=getFBFS().doc('ledger/'+uid).onSnapshot(snap=>{
        if(snap.exists){ // `exists` is a boolean property on DocumentSnapshot (v9+), not a method
          const data=snap.data();
          const last=getLastSync(uid);
          const local=stateRef.current||{};
          const localHasData=!!local.settings||(Array.isArray(local.expenses)&&local.expenses.length>0)||(Array.isArray(local.topUps)&&local.topUps.length>0);
          const cloudLooksDefault=!(Array.isArray(data.expenses)&&data.expenses.length>0)&&!(Array.isArray(data.topUps)&&data.topUps.length>0)&&!data.settings;
          const at=data.updatedAt;
          // Support both server timestamps and the old numeric format.
          const remoteAt=at&&typeof at.toMillis==='function'?at.toMillis():(typeof at==='number'?at:0);
          if(remoteAt>last){
            if(last===0&&localHasData&&!cloudLooksDefault){
              // First time linking this device with real data on both sides —
              // never silently clobber this device's copy. Keep local and push it.
              pushSync(uid);
              setLastSync(uid,remoteAt); // remember so we don't repeat the toast
              showToast(t('toast.keptDeviceData'),"success");
            }else if(cloudLooksDefault&&localHasData){
              // The cloud copy is an empty/default state (e.g. seeded before setup).
              // Never let it replace real local data — push the real copy up instead.
              pushSync(uid);
            }else{
              skipNextPushRef.current=true; // don't echo what we just applied back to the cloud
              applyRemote(data);
              setLastSync(uid,remoteAt);
              setLastSyncedAt(remoteAt);
              if(last===0)showToast(t('toast.syncedFromCloud'),"success"); // announce the first pull only, not our own echoes
            }
          }else if(remoteAt===0){
            // Unreadable timestamp — docs written by an earlier buggy build stored
            // a plain object instead of a server timestamp. Preserve whatever data
            // exists: pull real cloud data if this device is fresh, otherwise
            // re-save this device's copy with a proper timestamp.
            if(cloudLooksDefault||localHasData){safePushSync(uid)}
            else{skipNextPushRef.current=true;applyRemote(data)}
          }else if(remoteAt<last){
            safePushSync(uid); // cloud copy is older — restore it from this device
          }
        }else{
          safePushSync(uid); // first time on this account — seed the cloud with local data
        }
        syncedRef.current=uid;
      },e=>{console.warn("Sync listener error:",e);setSyncError(true);setSyncErrorMsg(String(e&&e.message||e))});
    }catch(e){console.warn("Sync subscribe failed:",e);setSyncError(true);setSyncErrorMsg(String(e&&e.message||e))}
    return ()=>{if(unsub)unsub()};
  },[authUser,applyRemote,pushSync,safePushSync,showToast]);

  /* Debounced push whenever any synced slice changes locally. */
  useEffect(()=>{
    if(skipNextPushRef.current){skipNextPushRef.current=false;return}
    if(!FIREBASE_CONFIGURED||!authUser||syncedRef.current!==authUser.uid)return;
    if(pushTimerRef.current)clearTimeout(pushTimerRef.current);
    pushTimerRef.current=setTimeout(()=>pushSync(authUser.uid),1500);
    return ()=>clearTimeout(pushTimerRef.current);
  },[expenses,topUps,balance,piggies,recurring,settings,categories,catBudgets,prefs,theme,authUser,pushSync]);

  const signInGoogle=async()=>{
    if(!FIREBASE_CONFIGURED){showToast(t('toast.syncNotConfigured'),"error");return}
    if(!fbInit()){showToast(t('toast.firebaseLoadFailed'),"error");return}
    try{
      const provider=new firebase.auth.GoogleAuthProvider();
      await getFBAuth().signInWithPopup(provider);
      showToast(t('toast.signedIn'),"success");
    }catch(err){
      const code=err&&err.code;
      if(code==='auth/popup-closed-by-user'){
        showToast(t('toast.signInCancelled'),"info");
      }else if(code==='auth/popup-blocked'||code==='auth/cancelled-popup-request'){
        showToast(t('toast.popupBlocked'),"error");
      }else if(code==='auth/unauthorized-domain'){
        showToast(t('toast.domainUnauthorized'),"error");
      }else if(code==='auth/operation-not-supported-in-this-environment'){
        showToast(t('toast.needsHosting'),"error");
      }else{
        showToast(err&&err.message?err.message:t('toast.signInFailed'),"error");
      }
    }
  };
  const signOutGoogle=async()=>{
    try{await getFBAuth().signOut();showToast(t('toast.signedOut'),"info")}
    catch(e){showToast(t('toast.signOutFailed'),"error")}
  };

  const requestNotifyPermission=async()=>{
    if(typeof Notification==='undefined'){showToast(t('toast.noNotificationSupport'),"error");return}
    try{
      const perm=await Notification.requestPermission();
      showToast(perm==='granted'?t('toast.notificationsEnabled'):t('toast.notificationsNotEnabled'),perm==='granted'?"success":"info");
    }catch(e){showToast(t('toast.notificationFailed'),"error")}
  };

  /* ─── App lock handlers ─── */
  const handleUnlock=async pin=>{
    const ok=await verifyPin(pin);
    if(ok){setLocked(false);showToast(t('toast.unlocked'),"success")}
    return ok;
  };
  const handleSetPin=async pin=>{
    const rec=await setLock(pin);
    setLockRec(rec);setLockSetup(false);setLocked(false);
    persistPrefs({...prefs,appLock:true});
    showToast(t('toast.appLockEnabled'),"success");
  };
  const handleToggleAppLock=()=>{
    if(prefs.appLock){persistPrefs({...prefs,appLock:false});setLocked(false);return}
    if(getLock()&&getLock().hash){persistPrefs({...prefs,appLock:true});return} // takes effect on next load/resume
    setLockSetup(true); // no PIN yet — create one before enabling
  };
  const handleBiometric=async()=>{
    try{
      const rec=getLock();
      if(rec&&rec.biometric)await unlockWithBiometric();
      else await registerBiometric();
      setLockRec(getLock());setLocked(false);showToast(t('toast.unlocked'),"success");
      return true;
    }catch(e){showToast(t('toast.deviceUnlockUnavailable'),"error");return false}
  };
  const handleSetupBiometric=async()=>{
    try{await registerBiometric();setLockRec(getLock());showToast(t('toast.deviceUnlockEnabled'),"success")}
    catch(e){showToast(t('toast.deviceUnlockSetupFailed'),"error")}
  };
  const handleForgotPin=()=>{
    clearLock();setLockRec(null);setLocked(false);setLockSetup(false);
    persistPrefs({...prefs,appLock:false});
    showToast(t('toast.appLockRemoved'),"info");
  };

  /* ─── Setup ─── */
  const saveSetup=()=>{
    const budget=parseFloat(draftBudget);
    const days=parseInt(draftDays,10);
    if(!isFinite(budget)||budget<0||!days||days<=0){showToast(t('toast.invalidBudget'),"error");return}
    let bal=null;
    if(balancesOn){
      bal=draftBalance.trim()===''?null:parseFloat(draftBalance);
      if(bal!==null&&(!isFinite(bal)||bal<0)){showToast(t('toast.invalidBalance'),"error");return}
    }
    persistSettings({monthlyBudget:budget,periodDays:days,startDate:draftStartDate||firstOfMonthKey()});
    if(!settings)persistPrefs({...prefs,currency:draftCurrency});
    if(balancesOn){
      if(bal!==null)persistBalance({...balance,start:bal});
      else if(!settings)persistBalance({...balance,start:0});
    }
    setShowSetup(false);
    showToast(settings?t('toast.budgetUpdated'):t('toast.budgetSaved'),"success");
  };

  /* ─── Move money & balance ─── */
  const topUpTotal=useMemo(()=>topUps.reduce((s,t)=>s+t.amount,0),[topUps]);
  const effectiveMonthlyBudget=settings?settings.monthlyBudget+topUpTotal:0;
  const dailyBudget=settings?effectiveMonthlyBudget/settings.periodDays:0;

  /* ─── Derived data ─── */
  const spentByDay=useMemo(()=>{
    const m={};for(const e of expenses)m[e.date]=(m[e.date]||0)+e.amount;return m;
  },[expenses]);

  const{dayCells,elapsedDays,runningBalance}=useMemo(()=>{
    if(!settings)return{dayCells:[],elapsedDays:0,runningBalance:0};
    const elapsed=Math.min(Math.max(dayDiff(settings.startDate,today)+1,1),settings.periodDays);
    let running=0;const cells=[];
    for(let i=0;i<settings.periodDays;i++){
      const date=addDays(settings.startDate,i);
      const isFuture=i>=elapsed;
      const spent=spentByDay[date]||0;
      const delta=isFuture?0:dailyBudget-spent;
      if(!isFuture)running+=delta;
      cells.push({date,spent,delta,isFuture,isToday:date===today});
    }
    return{dayCells:cells,elapsedDays:elapsed,runningBalance:running};
  },[settings,spentByDay,today,dailyBudget]);

  const todaySpent=spentByDay[today]||0;
  const todayRemaining=dailyBudget-todaySpent;

  /* Bank balance = starting money, plus the leftover allowance banked at the end
     of each day, minus money moved over to the monthly budget. Leftovers use the
     allowance that was in effect on each day (top-ups dated after a day don't
     retroactively change it), so returning money to the balance feels exact. */
  const bankedSoFar=useMemo(()=>{
    if(!settings)return 0;
    let banked=0;
    for(const c of dayCells){
      if(c.isFuture)break;
      // The allowance in effect on that day: top-ups dated after it don't
      // retroactively rewrite what was already saved.
      const topped=topUps.reduce((s,t)=>s+(t.date<=c.date?t.amount:0),0);
      const allowance=(settings.monthlyBudget+topped)/settings.periodDays;
      const left=allowance-c.spent;
      // Overspends either drain the bank balance (bank the negative leftover) or come
      // out of the total budget (bank only the positive leftover), per prefs.
      banked+=prefs.overspendFromBalance?left:Math.max(0,left);
    }
    return banked;
  },[settings,dayCells,topUps,prefs.overspendFromBalance]);
  const bankBalance=useMemo(()=>(balance?.start||0)-topUpTotal+bankedSoFar,[balance,topUpTotal,bankedSoFar]);
  const todaySaved=Math.max(0,todayRemaining);
  const heroLabel=heroMode==='balance'?t('hero.labelBalance'):t('hero.labelAvailable');
  const heroValue=heroMode==='balance'?bankBalance:todayRemaining;
  const periodSpent=useMemo(()=>dayCells.filter(c=>!c.isFuture).reduce((s,c)=>s+c.spent,0),[dayCells]);
  const budgetPctFull=effectiveMonthlyBudget>0?Math.min(100,(periodSpent/effectiveMonthlyBudget)*100):0;

  /* ─── Budget threshold alerts — 80% / 100%, fired at most once per period ─── */
  const firedAlertsRef=useRef(null);
  if(firedAlertsRef.current===null)firedAlertsRef.current=store.get("ledger-alerts")||{};
  useEffect(()=>{
    if(!settings||prefs.budgetAlerts===false)return;
    const periodKey=`${settings.startDate}|${settings.periodDays}`;
    const fired=firedAlertsRef.current;
    const slot=fired[periodKey]||(fired[periodKey]={});
    const periodEnd=addDays(settings.startDate,settings.periodDays-1);
    const catTotals={};
    for(const e of expenses){
      if(e.date<settings.startDate||e.date>periodEnd)continue;
      for(const c of expCats(e))catTotals[c]=(catTotals[c]||0)+e.amount;
    }
    const msgs=[];let over=false;
    const check=(key,label,spent,budget)=>{
      if(!(budget>0))return;
      const pct=(spent/budget)*100;
      const rec=slot[key]||(slot[key]={});
      for(const th of [80,100]){
        if(pct>=th&&!rec[th]){
          rec[th]=true;
          if(th>=100){over=true;msgs.push(t('toast.alertOver',{label,pct:Math.round(pct)}))}
          else msgs.push(t('toast.alertAt',{label,pct:Math.round(pct)}));
        }
      }
    };
    check('__monthly__',t('setup.monthlyBudget'),periodSpent,effectiveMonthlyBudget);
    for(const c of cats)check(`cat:${c.id}`,t('toast.categoryBudgetLabel',{label:c.label}),catTotals[c.id]||0,catBudgets[c.id]);
    if(msgs.length){
      store.set("ledger-alerts",fired);
      showToast(msgs.join(' '),over?'error':'warning');
      try{if(typeof Notification!=='undefined'&&Notification.permission==='granted')new Notification(t('toast.alertTitle'),{body:msgs.join(' ')})}catch(e){/* notifications are best-effort */}
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  },[expenses,catBudgets,cats,settings,topUps,prefs.budgetAlerts,periodSpent,effectiveMonthlyBudget]);

  const addTopUp=()=>{
    const val=parseFloat(topUpAmount);
    if(!val||val<=0){showToast(balancesOn?t('toast.invalidAmount'):t('toast.invalidTopUp'),"error");return}
    if(balancesOn&&val>bankBalance){showToast(t('toast.notEnoughBalanceMove',{amount:MYR(bankBalance)}),"error");return}
    const entry={id:uid(),amount:val,date:today,note:topUpNote.trim()};
    persistTopUps([...topUps,entry]);
    setTopUpAmount("");setTopUpNote("");setShowTopUp(false);
    showToast(balancesOn?t('toast.movedToBudget',{amount:MYR(val)}):t('toast.toppedUp',{amount:MYR(val)}),"success");
  };
  const addToBalance=()=>{
    const val=parseFloat(topUpAmount);
    if(!val||val<=0){showToast(t('toast.invalidAmount'),"error");return}
    persistBalance({...balance,start:(balance?.start||0)+val});
    setTopUpAmount("");setTopUpNote("");setShowTopUp(false);
    showToast(t('toast.addedToBalance',{amount:MYR(val)}),"success");
  };
  const returnToBalance=()=>{
    const val=parseFloat(topUpAmount);
    if(!val||val<=0){showToast(t('toast.invalidAmount'),"error");return}
    if(topUpTotal<=0){showToast(t('toast.nothingToReturn'),"error");return}
    if(val>topUpTotal){showToast(t('toast.returnTooMuch',{amount:MYR(topUpTotal)}),"error");return}
    const entry={id:uid(),amount:-val,date:today,note:topUpNote.trim()};
    persistTopUps([...topUps,entry]);
    setTopUpAmount("");setTopUpNote("");setShowTopUp(false);
    showToast(t('toast.returned',{amount:MYR(val)}),"success");
  };
  const withdrawFromBalance=()=>{
    const val=parseFloat(topUpAmount);
    if(!val||val<=0){showToast(t('toast.invalidAmount'),"error");return}
    if(bankBalance<=0){showToast(t('toast.nothingToWithdraw'),"error");return}
    if(val>bankBalance){showToast(t('toast.notEnoughBalanceWithdraw',{amount:MYR(bankBalance)}),"error");return}
    persistBalance({...balance,start:(balance?.start||0)-val});
    setTopUpAmount("");setTopUpNote("");setShowTopUp(false);
    showToast(t('toast.withdrew',{amount:MYR(val)}),"success");
  };
  const submitMoney=()=>{
    if(moveMode==='budget')return addTopUp();
    if(moveMode==='return')return returnToBalance();
    if(moveMode==='withdraw')return withdrawFromBalance();
    return addToBalance();
  };
  const removeTopUp=id=>{
    const removed=topUps.find(x=>x.id===id);
    persistTopUps(topUps.filter(x=>x.id!==id));
    if(removed)showToast(t('toast.transferRemoved'),"info",{
      label:t('toast.actionUndo'),run:()=>{persistTopUps([...topUps.filter(x=>x.id!==id),removed]);showToast(t('toast.restored'),"success")}
    });
  };

  /* ─── Piggy bank operations ─── */
  const addPiggy=(name,target)=>{
    const newId=uid();
    const newPiggy={
      id:newId,
      name:name.trim()||`Piggy #${piggies.length+1}`,
      target:target||0,
      saved:0,
      texture:null,
      soundId:'coin',
      soundCustom:null,
    };
    const next=[...piggies,newPiggy];
    persistPiggies(next);
    setActivePiggyId(newId);
    showToast(t('toast.piggyCreated',{name:newPiggy.name}),"success");
  };

  const renamePiggy=(id,newName)=>{
    const next=piggies.map(p=>p.id===id?{...p,name:newName.trim()||p.name}:p);
    persistPiggies(next);
    showToast(t('toast.piggyRenamed'),"success");
  };

  const savePiggyTarget=(id,targetVal)=>{
    const next=piggies.map(p=>p.id===id?{...p,target:targetVal}:p);
    persistPiggies(next);
    showToast(targetVal>0?t('toast.goalSet',{amount:MYR(targetVal)}):t('toast.goalCleared'),"success");
  };

  const depositPiggy=(id,amountVal)=>{
    if(!amountVal||amountVal<=0){showToast(t('toast.invalidAmount'),"error");return}
    if(amountVal>bankBalance){showToast(t('toast.notEnoughBalanceAdd',{amount:MYR(bankBalance)}),"error");return}
    const targetPiggy=piggies.find(p=>p.id===id)||piggies[0];
    const prev=targetPiggy.saved||0;
    const saved=prev+amountVal;
    persistBalance({...balance,start:(balance?.start||0)-amountVal});
    const next=piggies.map(p=>p.id===targetPiggy.id?{...p,saved}:p);
    persistPiggies(next);
    if(targetPiggy.soundId!=='none'){
      playPiggySound(targetPiggy.soundId||'coin',targetPiggy.soundCustom);
    }
    showToast(t('toast.addedToPiggy',{amount:MYR(amountVal),name:targetPiggy.name}),"success");
    if(targetPiggy.target>0&&prev<targetPiggy.target&&saved>=targetPiggy.target){
      confettiBurst();
      playCelebrate();
      showToast(t('toast.goalComplete',{name:targetPiggy.name}),"success");
    }
  };

  const breakPiggy=(id)=>{
    const targetPiggy=piggies.find(p=>p.id===id)||piggies[0];
    const savedAmt=targetPiggy.saved||0;
    if(savedAmt<=0){showToast(t('toast.piggyEmpty',{name:targetPiggy.name}),"error");return}
    setConfirm({
      title:t('confirm.breakPiggy',{name:targetPiggy.name}),
      msg:t('confirm.breakPiggyMsg',{amount:MYR(savedAmt)}),
      onConfirm:()=>{
        persistBalance({...balance,start:(balance?.start||0)+savedAmt});
        const next=piggies.map(p=>p.id===targetPiggy.id?{...p,saved:0}:p);
        persistPiggies(next);
        setConfirm(null);
        showToast(t('toast.brokePiggy',{name:targetPiggy.name,amount:MYR(savedAmt)}),"success");
      },
      onCancel:()=>setConfirm(null)
    });
  };

  const deletePiggy=(id)=>{
    if(piggies.length<=1){showToast(t('toast.cannotDeleteOnlyPiggy'),"error");return}
    const targetPiggy=piggies.find(p=>p.id===id);
    if(!targetPiggy)return;
    const savedAmt=targetPiggy.saved||0;
    setConfirm({
      title:t('confirm.deletePiggy',{name:targetPiggy.name}),
      msg:savedAmt>0?t('confirm.deletePiggySaved',{amount:MYR(savedAmt)}):t('confirm.deletePiggySure',{name:targetPiggy.name}),
      onConfirm:()=>{
        if(savedAmt>0){
          persistBalance({...balance,start:(balance?.start||0)+savedAmt});
        }
        const next=piggies.filter(p=>p.id!==id);
        if(activePiggyId===id)setActivePiggyId(next[0].id);
        persistPiggies(next);
        setConfirm(null);
        showToast(t('toast.piggyDeleted',{name:targetPiggy.name}),"success");
      },
      onCancel:()=>setConfirm(null)
    });
  };

  const updatePiggyTexture=(id,textureData)=>{
    const next=piggies.map(p=>p.id===id?{...p,texture:textureData}:p);
    persistPiggies(next);
  };

  const updatePiggySound=(id,soundId,soundCustom)=>{
    const next=piggies.map(p=>p.id===id?{...p,soundId,...(soundCustom!==undefined?{soundCustom}:{})}:p);
    persistPiggies(next);
  };

  /* ─── Automations (recurring entries) ─── */
  const runRecurring=(rules=recurring)=>{
    const ex=[...expenses],tu=[...topUps];
    let start=balance?.start||0;
    let changed=false;
    const next=rules.map(r=>{
      if(!r.active)return r;
      const from=r.last?todayKey(advanceDate(r.last,r.freq)):(r.start||today);
      const cursor=new Date(from+"T00:00:00");
      const end=new Date(today+"T00:00:00");
      if(cursor>end)return r;
      const occ=[];
      let cur=cursor;
      while(cur<=end){occ.push(todayKey(cur));cur=advanceDate(cur,r.freq)}
      for(const d of occ){
        if(r.type==='expense')ex.push({id:uid(),date:d,amount:r.amount,category:r.category||'other',categories:[r.category||'other'],note:r.note?`${r.note} (auto)`:""});
        else if(r.type==='budget')tu.push({id:uid(),amount:r.amount,date:d,note:r.note?`${r.note} (auto)`:""});
        else start+=r.amount;
      }
      changed=true;
      return {...r,last:occ[occ.length-1]};
    });
    if(changed){
      persistExpenses(ex);persistTopUps(tu);persistBalance({...balance,start});persistRecurring(next);
      showToast(t('toast.automatedEntries'),"success");
    }
  };
  useEffect(()=>{runRecurring()},[]); // materialize any due recurring entries on load
  const addAutomation=()=>{
    const val=parseFloat(autoAmount);
    if(!val||val<=0){showToast(t('toast.invalidAmount'),"error");return}
    if(!autoStart){showToast(t('toast.pickStartDate'),"error");return}
    const rule={id:uid(),type:autoType,amount:val,category:autoType==='expense'?autoCat:'',note:autoNote.trim(),freq:autoFreq,start:autoStart||today,last:null,active:true};
    const next=[...recurring,rule];
    persistRecurring(next);
    setAutoAmount("");setAutoNote("");
    showToast(t('toast.automationAdded'),"success");
    runRecurring(next); // backfill any occurrences up to today
  };
  const removeAutomation=id=>{
    const removed=recurring.find(r=>r.id===id);
    persistRecurring(recurring.filter(r=>r.id!==id));
    if(removed)showToast(t('toast.automationRemoved'),"info",{
      label:t('toast.actionUndo'),run:()=>{persistRecurring([...recurring.filter(r=>r.id!==id),removed]);showToast(t('toast.restored'),"success")}
    });
  };
  const toggleAutomation=r=>{
    const next={...r,active:!r.active};
    persistRecurring(recurring.map(x=>x.id===r.id?next:x));
    if(next.active)runRecurring(recurring.map(x=>x.id===r.id?next:x));
  };
  const nextRun=r=>{
    if(!r.active)return t('card.auto.paused');
    const from=r.last?todayKey(advanceDate(r.last,r.freq)):(r.start||today);
    const diff=dayDiff(today,from);
    if(diff<=0)return t('card.auto.dueToday');
    return diff===1?t('card.auto.tomorrow'):t('card.auto.inDays',{n:diff});
  };

  const avgDailySpend=useMemo(()=>{
    if(!settings||elapsedDays<=0)return 0;
    const total=dayCells.filter(c=>!c.isFuture).reduce((s,c)=>s+c.spent,0);
    return total/elapsedDays;
  },[settings,dayCells,elapsedDays]);

  const daysOver=useMemo(()=>dayCells.filter(c=>!c.isFuture&&c.delta<0).length,[dayCells]);
  const projectedTotal=useMemo(()=>settings?avgDailySpend*settings.periodDays:0,[settings,avgDailySpend]);
  const projectedDelta=useMemo(()=>settings?effectiveMonthlyBudget-projectedTotal:0,[settings,projectedTotal,effectiveMonthlyBudget]);

  /* ─── Frequent / smart quick-log suggestions ─── */
  const frequentEntries=useMemo(()=>{
    const map={};
    for(const e of expenses){
      const key=`${e.category}|${e.amount}|${(e.note||'').trim().toLowerCase()}`;
      if(!map[key])map[key]={category:e.category,amount:e.amount,note:e.note||'',count:0,last:e.date};
      map[key].count++;
      if(e.date>map[key].last)map[key].last=e.date;
    }
    return Object.values(map).filter(f=>f.count>1).sort((a,b)=>b.count-a.count||(b.last>a.last?1:-1)).slice(0,4);
  },[expenses]);
  const applyFrequent=f=>{setAmount(String(f.amount));setSelCats([f.category]);setNote(f.note)};
  const streak=useMemo(()=>{
    if(!settings)return 0;let c=0;
    for(const cell of[...dayCells].filter(x=>!x.isFuture).reverse()){if(cell.delta>=0)c++;else break}
    return c;
  },[dayCells,settings]);

  /* ─── Daily spend streak (log a spend every day) ─── */
  const {current:spendStreak,best:bestStreak,loggedToday}=useMemo(()=>spendStreaks(expenses,today,prefs.streakGrace||0),[expenses,today,prefs.streakGrace]);

  /* ─── Travel mode ───
     While a trip is active the dashboard is replaced by a travel home page. Entries
     are entered in the foreign currency but always stored with their home-currency
     amount, so every budget and statistic keeps working unchanged. */
  const travel=prefs.travel||{};
  const travelRate=Number(travel.rate)>0?Number(travel.rate):0;
  const travelActive=!!(travel.active&&travel.currency);
  const travelList=useMemo(()=>{
    if(!travelActive)return [];
    return expenses.filter(e=>(e.tags||[]).includes('travel')&&(!travel.start||e.date>=travel.start));
  },[expenses,travelActive,travel.start]);

  /* Every currency used on this trip with its own subtotal. Entries keep the currency they
     were logged in, so changing the trip's currency never rewrites the older ones. */
  const travelByCurrency=useMemo(()=>{
    const map=new Map();
    for(const e of travelList){
      const code=e.currency||travel.currency;
      const row=map.get(code)||{code,foreign:0,home:0,count:0};
      row.foreign+=typeof e.foreignAmount==='number'?e.foreignAmount:e.amount;
      row.home+=e.amount;row.count++;
      map.set(code,row);
    }
    return [...map.values()].sort((a,b)=>b.home-a.home);
  },[travelList,travel.currency]);

  /* ─── Exchange-rate sync ───
     Pulls the current market rate when a trip starts and whenever either currency changes
     (or on demand from the Travel tab). Refreshing only affects *future* entries — every
     logged spend keeps the home-currency amount it was recorded with. */
  const[rateStatus,setRateStatus]=useState('');
  const rateKeyRef=useRef('');
  const refreshTravelRate=useCallback(async()=>{
    const tr=prefsRef.current.travel||{};
    if(!tr.active||!tr.currency){setRateStatus('');return}
    setRateStatus('loading');
    try{
      const {rate,date}=await fetchRate(tr.currency,prefsRef.current.currency||'MYR');
      persistPrefs({...prefsRef.current,travel:{...tr,rate,rateUpdatedAt:date}});
      setRateStatus(date?`ok:${date}`:'ok');
    }catch(err){
      rateKeyRef.current='';
      setRateStatus('error');
    }
  },[persistPrefs]);
  useEffect(()=>{
    const tr=prefs.travel||{};
    if(!travelActive||!tr.currency||tr.rateAuto===false)return;
    const key=`${tr.currency}>${prefs.currency}`;
    if(rateKeyRef.current===key)return;
    rateKeyRef.current=key;
    refreshTravelRate();
  },[travelActive,prefs.travel&&prefs.travel.currency,prefs.currency,prefs.travel&&prefs.travel.rateAuto,refreshTravelRate]);

  /* Ending a trip changes nothing about the entries — confirm first so it isn't a surprise. */
  const endTravel=()=>setConfirm({
    title:t('confirm.endTripTitle'),
    msg:t('confirm.endTripMsg'),
    danger:false,
    onConfirm:()=>{persistPrefs({...prefs,travel:{...travel,active:false}});setConfirm(null);showToast(t('toast.tripEnded'),"info")},
    onCancel:()=>setConfirm(null),
  });

  /* ─── Monthly insights (calendar month over month) ─── */
  const monthInsights=useMemo(()=>{
    const t=new Date(today+"T00:00:00");
    const curStart=firstOfMonthKey(t);
    const daysThis=daysInMonth(t);
    const curEnd=addDays(curStart,daysThis-1);
    const prevDate=new Date(t.getFullYear(),t.getMonth()-1,1);
    const prevStart=firstOfMonthKey(prevDate);
    const prevEnd=addDays(prevStart,daysInMonth(prevDate)-1);
    let thisTotal=0,lastTotal=0;
    const thisCat={},lastCat={},byDay={};
    for(const e of expenses){
      if(e.date>=curStart&&e.date<=curEnd){
        thisTotal+=e.amount;byDay[e.date]=(byDay[e.date]||0)+e.amount;
        for(const c of expCats(e))thisCat[c]=(thisCat[c]||0)+e.amount;
      }else if(e.date>=prevStart&&e.date<=prevEnd){
        lastTotal+=e.amount;
        for(const c of expCats(e))lastCat[c]=(lastCat[c]||0)+e.amount;
      }
    }
    const dayN=t.getDate();
    const avg=thisTotal/Math.max(1,dayN);
    const projected=avg*daysThis;
    const pct=lastTotal>0?((thisTotal-lastTotal)/lastTotal)*100:null;
    let biggest=null;
    for(const c of cats){
      const delta=(thisCat[c.id]||0)-(lastCat[c.id]||0);
      if(!biggest||Math.abs(delta)>Math.abs(biggest.delta))biggest={cat:c,delta};
    }
    let best=null,worst=null;
    for(let i=0;i<dayN;i++){
      const d=addDays(curStart,i);
      const v=byDay[d]||0;
      if(!worst||v>worst.amount)worst={date:d,amount:v};
      if(!best||v<best.amount)best={date:d,amount:v};
    }
    return{thisTotal,lastTotal,pct,avg,projected,daysElapsed:dayN,daysInMonth:daysThis,best,worst,biggest};
  },[expenses,cats,today]);

  /* ─── Breakdown ─── */
  const[overviewRange,setOverviewRange]=useState("period");
  const[ovFrom,setOvFrom]=useState("");
  const[ovTo,setOvTo]=useState("");
  const overviewBounds=useMemo(()=>{
    switch(overviewRange){
      case"week":return{start:addDays(today,-6),end:today};
      case"month":return{start:firstOfMonthKey(new Date()),end:today};
      case"all":return{start:null,end:today};
      case"custom":return{start:ovFrom||null,end:ovTo||today};
      default:return{start:settings?.startDate||today,end:today};
    }
  },[overviewRange,today,settings,ovFrom,ovTo]);
  const overviewExpenses=useMemo(()=>expenses.filter(e=>{
    if(overviewBounds.start&&e.date<overviewBounds.start)return false;
    if(e.date>overviewBounds.end)return false;
    return true;
  }),[expenses,overviewBounds]);
  const categoryTotals=useMemo(()=>{
    const m={};for(const c of cats)m[c.id]=0;
    for(const e of overviewExpenses)for(const c of expCats(e))m[c]=(m[c]||0)+e.amount;
    return m;
  },[overviewExpenses,cats]);
  const maxCategory=Math.max(1,...Object.values(categoryTotals));
  const biggestInRange=useMemo(()=>overviewExpenses.length?overviewExpenses.reduce((max,e)=>e.amount>max.amount?e:max,overviewExpenses[0]):null,[overviewExpenses]);
  const totalSpent=useMemo(()=>overviewExpenses.reduce((s,e)=>s+e.amount,0),[overviewExpenses]);
  const rangeDays=useMemo(()=>{
    if(overviewBounds.start)return Math.max(1,dayDiff(overviewBounds.start,overviewBounds.end)+1);
    if(overviewExpenses.length===0)return 1;
    const earliest=overviewExpenses.reduce((min,e)=>e.date<min?e.date:min,overviewExpenses[0].date);
    return Math.max(1,dayDiff(earliest,overviewBounds.end)+1);
  },[overviewBounds,overviewExpenses]);
  const rangeBudget=dailyBudget*rangeDays;
  const budgetPct=rangeBudget>0?(totalSpent/rangeBudget)*100:0;
  const avgPerDayInRange=totalSpent/rangeDays;
  const topCategory=useMemo(()=>{
    let top=null;for(const c of cats)if(!top||categoryTotals[c.id]>categoryTotals[top.id])top=c;
    return top&&categoryTotals[top.id]>0?top:null;
  },[cats,categoryTotals]);
  const rangeLabel={period:t('card.breakdown.rangeLabelPeriod'),week:t('card.breakdown.rangeLabelWeek'),month:t('card.breakdown.rangeLabelMonth'),all:t('card.breakdown.rangeLabelAll'),custom:t('card.breakdown.rangeLabelCustom')}[overviewRange];

  const pieSlices=useMemo(()=>{
    const pool=Object.values(categoryTotals).reduce((a,b)=>a+b,0);
    if(pool<=0)return[];
    let cumulative=0;
    return cats.map(c=>{
      const val=categoryTotals[c.id]||0;
      const frac=val/pool;const dash=frac*100;
      const slice={...c,value:val,pct:frac*100,dash,offset:cumulative};
      cumulative+=dash;return slice;
    }).filter(s=>s.value>0);
  },[cats,categoryTotals]);

  /* ─── Trend chart data ─── */
  const[trendRange,setTrendRange]=useState(14);
  const[trendSeries,setTrendSeries]=useState(()=>["__total__"]);
  const toggleTrendSeries=id=>setTrendSeries(prev=>prev.includes(id)?prev.filter(x=>x!==id):[...prev,id]);
  const trendData=useMemo(()=>{
    const map={};
    for(let i=trendRange-1;i>=0;i--){
      const date=addDays(today,-i);
      map[date]={date,total:0,byCat:{}};
    }
    for(const e of expenses){
      const d=map[e.date];
      if(!d)continue;
      d.total+=e.amount;
      for(const c of expCats(e))d.byCat[c]=(d.byCat[c]||0)+e.amount;
    }
    return Object.values(map);
  },[expenses,today,trendRange]);
  const trendSeriesList=useMemo(()=>{
    const list=[];
    if(trendSeries.includes("__total__"))list.push({id:"__total__",label:"Total",color:theme.accent,val:d=>d.total});
    for(const id of trendSeries){
      if(id==="__total__")continue;
      const c=cats.find(x=>x.id===id);
      if(c)list.push({id,label:c.label,color:c.color,val:d=>d.byCat[id]||0});
    }
    return list;
  },[trendSeries,cats,theme.accent]);
  const trendMax=Math.max(dailyBudget*1.3,...trendData.flatMap(d=>trendSeriesList.map(s=>s.val(d))),1);
  const[trendHover,setTrendHover]=useState(null);

  /* GitHub-style spending heatmap data (weeks as columns, Mon–Sun rows) */
  const heatColors={...HEAT_DEFAULT_COLORS,...(prefs.heatColors||{})};
  const heatData=useMemo(()=>{
    const end=new Date(today+'T00:00:00');
    const start=new Date(end);start.setDate(end.getDate()-(trendRange-1));
    const first=new Date(start);first.setDate(start.getDate()-((start.getDay()+6)%7)); // Monday on/before the range start
    const cells=[];let total=0,max=0;
    for(let d=new Date(first);d<=end;d.setDate(d.getDate()+1)){
      const date=todayKey(d);
      const spent=spentByDay[date]||0;
      total+=spent;if(spent>max)max=spent;
      cells.push({date,spent});
    }
    const top=max>0?max:1;
    return{
      weeks:Math.ceil(cells.length/7),
      total,
      cells:cells.map(c=>({...c,level:c.spent===0?0:Math.min(4,Math.max(1,Math.ceil((c.spent/top)*4)))})),
    };
  },[spentByDay,today,trendRange]);
  /* Keep the range sensible for the chosen style. */
  useEffect(()=>{
    if(prefs.trendStyle==='heatmap'&&trendRange<30)setTrendRange(30);
    if(prefs.trendStyle==='line'&&trendRange>30)setTrendRange(30);
  },[prefs.trendStyle]);
  /* Size heatmap cells to fit the card: fill the width for long ranges, but cap
     the size so short ranges (few weeks) don't blow up. */
  const heatWrapRef=useRef(null);
  const[heatCell,setHeatCell]=useState(12);
  useEffect(()=>{
    const measure=()=>{
      const el=heatWrapRef.current;
      if(!el)return;
      const weeks=heatData.weeks;
      if(!weeks)return;
      const avail=el.clientWidth;
      const cell=Math.max(10,Math.min(24,Math.floor((avail-(weeks-1)*3)/weeks)));
      setHeatCell(cell);
    };
    measure();
    window.addEventListener('resize',measure);
    return ()=>window.removeEventListener('resize',measure);
  },[heatData.weeks,prefs.trendStyle]);

  /* ─── History ─── */
  const[filterCats,setFilterCats]=useState([]);
  const[filterTags,setFilterTags]=useState([]);
  const[showFilters,setShowFilters]=useState(false);
  const[historySort,setHistorySort]=useState("date-desc");
  const[historySearch,setHistorySearch]=useState("");
  const[dateFrom,setDateFrom]=useState("");
  const[dateTo,setDateTo]=useState("");
  const toggleFilterCat=id=>setFilterCats(p=>p.includes(id)?p.filter(x=>x!==id):[...p,id]);
  const toggleFilterTag=t=>setFilterTags(p=>p.includes(t)?p.filter(x=>x!==t):[...p,t]);
  /* Every tag currently in use, for the History filter row. */
  const allTags=useMemo(()=>[...new Set(expenses.flatMap(e=>e.tags||[]))].sort(),[expenses]);
  const historyList=useMemo(()=>{
    let list=[
      ...expenses.map(e=>({...e,type:'expense'})),
      ...topUps.map(t=>({...t,type:'topup'})),
    ];
    if(filterCats.length)list=list.filter(e=>e.type==='expense'&&expCats(e).some(c=>filterCats.includes(c)));
    if(filterTags.length)list=list.filter(e=>e.type==='expense'&&(e.tags||[]).some(t=>filterTags.includes(t)));
    if(historySearch.trim()){
      const q=historySearch.trim().toLowerCase();
      list=list.filter(e=>e.type==='topup'
        ?((e.note||'').toLowerCase().includes(q)||'move to budget'.includes(q)||'top up'.includes(q)||'return to balance'.includes(q))
        :((e.note||'').toLowerCase().includes(q)||expCats(e).some(c=>c.toLowerCase().includes(q))||(e.tags||[]).some(t=>t.toLowerCase().includes(q))));
    }
    if(dateFrom)list=list.filter(e=>e.date>=dateFrom);
    if(dateTo)list=list.filter(e=>e.date<=dateTo);
    switch(historySort){
      case"date-asc":list.sort((a,b)=>(a.date<b.date?-1:a.date>b.date?1:cmpId(a,b)));break;
      case"amount-desc":list.sort((a,b)=>b.amount-a.amount);break;
      case"amount-asc":list.sort((a,b)=>a.amount-b.amount);break;
      default:list.sort((a,b)=>(a.date<b.date?1:a.date>b.date?-1:cmpId(b,a)));
    }
    return list;
  },[expenses,topUps,filterCats,filterTags,historySearch,dateFrom,dateTo,historySort]);
  const historySpentTotal=useMemo(()=>historyList.filter(e=>e.type!=='topup').reduce((s,e)=>s+e.amount,0),[historyList]);
  const historyToppedTotal=useMemo(()=>historyList.filter(e=>e.type==='topup').reduce((s,e)=>s+e.amount,0),[historyList]);
  const activeFilterCount=(historySearch.trim()?1:0)+(dateFrom||dateTo?1:0)+(historySort!=="date-desc"?1:0)+(filterCats.length?1:0)+(filterTags.length?1:0);
  const resetFilters=()=>{setFilterCats([]);setFilterTags([]);setHistorySearch("");setDateFrom("");setDateTo("");setHistorySort("date-desc")};

  // Group history by date bucket
  const groupedHistory=useMemo(()=>{
    if(!prefs.groupHistory||historySort.startsWith("amount"))return[{label:null,items:historyList}];
    const groups={};const order=[];
    for(const e of historyList){
      const label=groupLabel(e.date,today);
      if(!groups[label]){groups[label]={label,items:[],total:0};order.push(label)}
      groups[label].items.push(e);groups[label].total+=e.type==='topup'?0:e.amount;
    }
    return order.map(l=>groups[l]);
  },[historyList,prefs.groupHistory,historySort,today]);

  /* ─── CRUD ─── */
  const attachReceipt=async file=>{
    if(!file)return;
    if(!file.type.startsWith("image/")){showToast(t('toast.chooseImage'),"error");return}
    if(file.size>10*1024*1024){showToast(t('toast.imageTooLarge10'),"error");return}
    try{setReceipt(await downscaleImage(file,600,0.6));showToast(t('toast.receiptAttached'),"success")}
    catch(err){showToast(t('toast.unreadableImage'),"error")}
  };
  const removeReceipt=()=>setReceipt(null);

  /* ─── Tags ─── */
  const addTag=v=>{
    const t=cleanTags([v])[0];
    if(!t)return;
    setTags(prev=>prev.includes(t)?prev:[...prev,t].slice(0,8));
  };
  const removeTag=t=>setTags(prev=>prev.filter(x=>x!==t));

  const addExpense=()=>{
    const raw=parseFloat(amount);if(!raw||raw<=0)return;
    const cat=selCats[0]||"food";
    /* In travel mode the typed figure is the foreign amount; `amount` always stores
       the home-currency equivalent so budgets stay comparable. */
    const val=travelActive&&travelRate>0?raw*travelRate:raw;
    const entry={
      id:uid(),date:entryDate||today,amount:val,categories:[cat],category:cat,note:note.trim(),receipt:receipt||null,
      tags:travelActive?cleanTags([...tags,'travel',travel.name||'']):tags,
      ...(travelActive?{currency:travel.currency,foreignAmount:raw}:{}),
    };
    persistExpenses([...expenses,entry]);
    setAmount("");setNote("");setEntryDate(today);setReceipt(null);setTags([]);
    const catLabel=cats.find(c=>c.id===cat)?.label||cat;
    showToast(travelActive
      ?t('toast.loggedTravel',{foreign:fmt(raw,travel.currency),amount:MYR(val),category:catLabel})
      :t('toast.logged',{amount:MYR(val),category:catLabel}),"success");
  };
  const startEdit=e=>{
    const ec=expCats(e);setEditingId(e.id);
    /* Show the foreign figure when editing an entry that was logged abroad. */
    setAmount(travelActive&&typeof e.foreignAmount==='number'?String(e.foreignAmount):String(e.amount));
    setNote(e.note);setSelCats(ec.length?[ec[0]]:["food"]);setEntryDate(e.date);setReceipt(e.receipt||null);setTags(cleanTags(e.tags));
    if(addFormRef.current)addFormRef.current.scrollIntoView({behavior:'smooth',block:'center'});
  };
  const updateExpense=()=>{
    const raw=parseFloat(amount);if(!raw||raw<=0)return;
    const cat=selCats[0]||"food";
    persistExpenses(expenses.map(e=>{
      if(e.id!==editingId)return e;
      const isTravel=travelActive&&typeof e.foreignAmount==='number';
      const val=isTravel&&travelRate>0?raw*travelRate:raw;
      return{
        ...e,amount:val,categories:[cat],category:cat,note:note.trim(),date:entryDate||today,receipt:receipt||null,tags,
        ...(isTravel?{currency:e.currency,foreignAmount:raw}:{}),
      };
    }));
    cancelEdit();showToast(t('toast.spendUpdated'),"success");
  };
  const cancelEdit=()=>{setEditingId(null);setAmount("");setNote("");setEntryDate(today);setReceipt(null);setTags([])};
  const removeExpense=id=>{
    const idx=expenses.findIndex(e=>e.id===id);
    const removed=expenses[idx];
    persistExpenses(expenses.filter(e=>e.id!==id));
    if(editingId===id)cancelEdit();
    if(removed){
      setLastAction({type:'delete',data:removed});
      showToast(t('toast.spendRemoved'),"info",{
        label:t('toast.actionUndo'),run:()=>{
          // Re-insert at its original position in the current list.
          const cur=stateRef.current?.expenses||[];
          const next=cur.filter(e=>e.id!==removed.id);
          next.splice(Math.min(Math.max(idx,0),next.length),0,removed);
          persistExpenses(next);setLastAction(null);showToast(t('toast.restored'),"success");
        }
      });
    }
  };
  const duplicateExpense=e=>{
    const entry={...e,id:uid(),date:today};
    persistExpenses([...expenses,entry]);
    showToast(t('toast.duplicated',{amount:MYR(e.amount)}),"success");
  };

  /* ─── Backup ─── */
  const exportData=()=>{
    const payload={type:"ledger-backup",version:6,exportedAt:new Date().toISOString(),settings,expenses,categories,catBudgets,topUps,balance,piggy:piggies[0]||{target:0,saved:0},piggies,recurring,prefs};
    const blob=new Blob([JSON.stringify(payload,null,2)],{type:"application/json"});
    const url=URL.createObjectURL(blob);
    const a=document.createElement("a");a.href=url;a.download=`ledger-backup-${today}.json`;
    document.body.appendChild(a);a.click();document.body.removeChild(a);URL.revokeObjectURL(url);
    showToast(t('toast.backupDownloaded'),"success");
  };
  const exportCSV=()=>{
    if(expenses.length===0){showToast(t('toast.nothingToExport'),"error");return}
    const rows=[["Date","Amount","Currency","Category","Note"]];
    const sorted=[...expenses].sort((a,b)=>(a.date<b.date?-1:a.date>b.date?1:cmpId(a,b)));
    for(const e of sorted){
      const catL=expCats(e).map(id=>(cats.find(c=>c.id===id)||{}).label||id).join(" + ");
      rows.push([e.date,e.amount.toFixed(2),cur,catL,(e.note||"").replace(/"/g,'""')]);
    }
    const csv=rows.map(r=>r.map(c=>`"${c}"`).join(",")).join("\n");
    const blob=new Blob([csv],{type:"text/csv;charset=utf-8;"});
    const url=URL.createObjectURL(blob);
    const a=document.createElement("a");a.href=url;a.download=`ledger-export-${today}.csv`;
    document.body.appendChild(a);a.click();document.body.removeChild(a);URL.revokeObjectURL(url);
    showToast(t('toast.csvExported'),"success");
  };
  const triggerImport=()=>fileInputRef.current?.click();
  const handleImportFile=e=>{
    const file=e.target.files?.[0];if(!file)return;
    const reader=new FileReader();
    reader.onload=ev=>{
      try{
        const data=JSON.parse(ev.target.result);
        if(!data||typeof data!=="object"||!validExpenses(data.expenses)){showToast(t('toast.invalidBackup'),"error");return}
        // Validate every section before touching local state, so a malformed
        // file can never leave the app half-restored.
        if(data.settings!==undefined&&!validSettings(data.settings)){showToast(t('toast.invalidBackupSettings'),"error");return}
        if(data.topUps!==undefined&&!validTopUps(data.topUps)){showToast(t('toast.invalidBackupTransfers'),"error");return}
        if(data.recurring!==undefined&&!validRecurring(data.recurring)){showToast(t('toast.invalidBackupAutomations'),"error");return}
        if(data.categories!==undefined&&!validCats(data.categories)){showToast(t('toast.invalidBackupCategories'),"error");return}
        if(data.catBudgets!==undefined&&!isPlainObj(data.catBudgets)){showToast(t('toast.invalidBackupCatBudgets'),"error");return}
        if(data.piggies!==undefined&&!validPiggies(data.piggies)){showToast(t('toast.invalidBackupPiggies'),"error");return}
        if(data.balance!==undefined&&!(isPlainObj(data.balance)&&typeof data.balance.start==='number')){showToast(t('toast.invalidBackupBalance'),"error");return}
        if(data.piggy!==undefined&&!(isPlainObj(data.piggy)&&typeof data.piggy.target==='number'&&typeof data.piggy.saved==='number')&&!(Array.isArray(data.piggies)&&data.piggies.length)){showToast(t('toast.invalidBackupPiggy'),"error");return}
        const normExpenses=normalizeExpenses(data.expenses);
        if(data.settings)persistSettings(data.settings);
        if(data.categories)persistCats(data.categories);
        if(data.catBudgets)persistCatBudgets(data.catBudgets);
        if(Array.isArray(data.topUps))persistTopUps(data.topUps);
        if(data.balance)persistBalance(data.balance);
        if(Array.isArray(data.piggies)&&data.piggies.length){persistPiggies(data.piggies);if(data.piggies[0])setActivePiggyId(data.piggies[0].id)}
        else if(data.piggy&&isPlainObj(data.piggy)&&typeof data.piggy.target==='number'&&typeof data.piggy.saved==='number')persistPiggies([{id:uid(),name:'Piggy bank',target:data.piggy.target||0,saved:data.piggy.saved||0,texture:null,soundId:'coin',soundCustom:null}]);
        if(Array.isArray(data.recurring))persistRecurring(data.recurring);
        if(data.prefs)persistPrefs({...prefs,...data.prefs});
        persistExpenses(normExpenses);
        showToast(t('toast.restoredEntries',{count:normExpenses.length}),"success");
      }catch(err){showToast(t('toast.unreadableFile'),"error")}
    };
    reader.readAsText(file);e.target.value="";
  };

  const wallpaperInputRef=useRef(null);
  const triggerWallpaperUpload=()=>wallpaperInputRef.current?.click();
  const handleWallpaperFile=e=>{
    const file=e.target.files?.[0];if(!file)return;
    const isVideo=file.type.startsWith("video/");
    const isImage=file.type.startsWith("image/");
    if(!isVideo&&!isImage){showToast(t('toast.chooseMedia'),"error");e.target.value="";return}
    const maxSize=isVideo?24*1024*1024:8*1024*1024;
    if(file.size>maxSize){showToast(t('toast.mediaTooLarge',{kind:isVideo?t('toast.kindVideo'):t('toast.kindImage'),size:isVideo?24:8}),"error");e.target.value="";return}
    const reader=new FileReader();
    reader.onload=ev=>{
      persistPrefs({...prefs,wallpaper:ev.target.result});
      showToast(t('toast.wallpaperSet',{kind:isVideo?t('toast.kindVideo'):t('toast.kindImage')}),"success");
    };
    reader.onerror=()=>showToast(t('toast.unreadableMedia',{kind:isVideo?t('toast.kindVideoLower'):t('toast.kindImageLower')}),"error");
    reader.readAsDataURL(file);e.target.value="";
  };
  const clearWallpaper=()=>{persistPrefs({...prefs,wallpaper:null});showToast(t('toast.wallpaperRemoved'),"success")};

  const cardPanelInputRef=useRef(null);
  const triggerCardPanelUpload=()=>cardPanelInputRef.current?.click();
  const handleCardPanelFile=e=>{
    const file=e.target.files?.[0];if(!file)return;
    if(!file.type.startsWith("image/")){showToast(t('toast.chooseImageFile'),"error");e.target.value="";return}
    if(file.size>8*1024*1024){showToast(t('toast.imageTooLarge8'),"error");e.target.value="";return}
    const reader=new FileReader();
    reader.onload=ev=>{
      persistPrefs({...prefs,cardPanel:ev.target.result});
      showToast(t('toast.cardPanelSet'),"success");
    };
    reader.onerror=()=>showToast(t('toast.unreadableImage'),"error");
    reader.readAsDataURL(file);e.target.value="";
  };
  const clearCardPanel=()=>{persistPrefs({...prefs,cardPanel:null});showToast(t('toast.cardPanelRemoved'),"success")};

  /* ─── Categories ─── */
  const addCategory=()=>{
    const name=newCatName.trim();
    if(!name){showToast(t('toast.enterCategoryName'),"error");return}
    const id=name.toLowerCase().replace(/\s+/g,'-').replace(/[^a-z0-9-]/g,'');
    if(!id||cats.find(c=>c.id===id)){showToast(t('toast.invalidCategoryName'),"error");return}
    const newCat={id,label:name,glyph:newCatGlyph||"★"};
    persistCats([...categories,newCat]);
    persistTheme({...theme,catColors:{...theme.catColors,[id]:'#7c8896'}});
    setNewCatName("");setNewCatGlyph("★");
    showToast(t('toast.categoryAdded',{name}),"success");
  };
  const removeCategory=id=>{
    if(expenses.some(e=>expCats(e).includes(id))){showToast(t('toast.categoryInUse'),"error");return}
    const updated=categories.filter(c=>c.id!==id);
    persistCats(updated);
    if(selCats.includes(id))setSelCats(prev=>{
      const next=prev.filter(x=>x!==id);
      return next.length?next:(updated[0]?[updated[0].id]:["food"]);
    });
    showToast(t('toast.categoryRemoved'));
  };

  /* ─── Theme ─── */
  const activePresetKey=useMemo(()=>{
    for(const[key,preset]of Object.entries(PRESETS)){
      const keys=['bg','surface','surface2','text','textDim','textMuted','border','borderStrong','accent','accentFg','negative','warning','positive'];
      const base=keys.every(k=>preset[k]===theme[k]);
      const catsMatch=categories.every(c=>preset.catColors[c.id]===theme.catColors[c.id]);
      if(base&&catsMatch)return key;
    }
    return null;
  },[theme,categories]);
  const applyPreset=key=>{persistSavedTheme(null);persistTheme({...PRESETS[key]});showToast(t('toast.themeApplied',{name:PRESETS[key].name}),"success")};
  const updateColor=(k,v)=>{persistSavedTheme(null);persistTheme({...theme,[k]:v})};
  const updateCatColor=(catId,v)=>{persistSavedTheme(null);persistTheme({...theme,catColors:{...theme.catColors,[catId]:v}})};
  const resetTheme=()=>{persistSavedTheme(null);persistTheme(DEFAULT_THEME);showToast(t('toast.themeReset'),"success")};
  const isDark=useMemo(()=>{
    const hex=theme.bg.replace('#','');const r=parseInt(hex.substr(0,2),16);const g=parseInt(hex.substr(2,2),16);const b=parseInt(hex.substr(4,2),16);
    return(r*0.299+g*0.587+b*0.114)<128;
  },[theme.bg]);
  const toggleLightDark=()=>{
    // If we previously toggled away from a theme, switching back restores it exactly.
    if(savedTheme){
      persistTheme(savedTheme);
      persistSavedTheme(null);
      showToast(t('toast.themeRestored'),"success");
      return;
    }
    // Otherwise remember the current theme, then flip to the opposite light/dark preset.
    persistSavedTheme(theme);
    if(isDark){persistTheme(PRESETS.paper);showToast(t('toast.paperTheme'),"success")}
    else{persistTheme(PRESETS.mono);showToast(t('toast.monoTheme'),"success")}
  };

  const handleClearAll=()=>setConfirm({
    title:t('confirm.deleteEverything'),
    msg:t('confirm.deleteEverythingMsg'),
    onConfirm:()=>{persistExpenses([]);persistSettings(null);persistCats(DEFAULT_CATS);persistCatBudgets({});persistTopUps([]);persistBalance({start:0});persistPiggies([{id:uid(),name:'Piggy bank',target:0,saved:0,texture:null,soundId:'coin',soundCustom:null}]);persistRecurring([]);setConfirm(null);showToast(t('toast.allDataCleared'),"success")},
    onCancel:()=>setConfirm(null)
  });

  const isVideoWallpaper=useMemo(()=>!!prefs.wallpaper&&prefs.wallpaper.startsWith('data:video'),[prefs.wallpaper]);
  const allFontOptions=useMemo(()=>[...FONT_OPTIONS,...(prefs.customFonts||[])],[prefs.customFonts]);
  const activeFont=useMemo(()=>allFontOptions.find(f=>f.id===prefs.font)||FONT_OPTIONS[0],[allFontOptions,prefs.font]);

  // Re-inject <link> stylesheets for any saved custom Google Fonts on load/change.
  useEffect(()=>{
    (prefs.customFonts||[]).forEach(f=>loadGoogleFont(f.family||f.name));
  },[prefs.customFonts]);

  const addCustomFont=()=>{
    const name=draftFontName.trim();
    if(!name){showToast(t('toast.enterFontName'),"error");return}
    const id=`custom:${name.toLowerCase().replace(/\s+/g,'-')}`;
    if((prefs.customFonts||[]).some(f=>f.id===id)){
      persistPrefs({...prefs,font:id});
      setDraftFontName("");
      showToast(t('toast.fontAlreadyAdded',{name}),"success");
      return;
    }
    loadGoogleFont(name);
    const entry={id,name,family:name,stack:customFontStack(name)};
    persistPrefs({...prefs,customFonts:[...(prefs.customFonts||[]),entry],font:id});
    setDraftFontName("");
    showToast(t('toast.fontAdded',{name}),"success");
  };
  const removeCustomFont=id=>{
    const next=(prefs.customFonts||[]).filter(f=>f.id!==id);
    persistPrefs({...prefs,customFonts:next,font:prefs.font===id?'inter':prefs.font});
  };

  const themeVars={
    '--bg':theme.bg,'--surface':theme.surface,'--surface-2':theme.surface2,
    '--text':theme.text,'--text-dim':theme.textDim,'--text-muted':theme.textMuted,
    '--border':theme.border,'--border-strong':theme.borderStrong,
    '--accent':theme.accent,'--accent-fg':theme.accentFg||'#fff',
    '--negative':theme.negative,'--warning':theme.warning,'--positive':theme.positive,
    '--ui-blur':`${prefs.uiBlur}px`,
    '--ui-opacity':prefs.uiOpacity/100,
    '--wall-blur':`${prefs.wallBlur}px`,
    '--card-panel-image':prefs.cardPanel?`url(${prefs.cardPanel})`:'none',
    '--card-panel-opacity':prefs.cardPanel?(prefs.cardPanelOpacity??100)/100:0,
    '--card-title-bg':prefs.cardPanel
      ?`color-mix(in srgb, var(--accent) ${Math.max(15,100-(prefs.cardPanelOpacity??100)*0.7)}%, transparent)`
      :'var(--accent)',
    fontFamily:activeFont.stack,
    '--font-mono':activeFont.stack,
    ...(prefs.wallpaper?{background:'transparent'}:{}),
  };

  /* Notify the vanilla-JS desktop cat script of live prefs changes (color, on/off). */
  useEffect(()=>{
    window.dispatchEvent(new CustomEvent('ledger:prefs',{detail:prefs}));
  },[prefs]);

  const healthBadge=()=>{
    if(!settings)return null;
    if(todayRemaining<0)return<span className="hero-badge neg">{t('app.badgeOver')}</span>;
    if(todayRemaining/dailyBudget<0.2)return<span className="hero-badge warn">{t('app.badgeNear')}</span>;
    return<span className="hero-badge pos">{t('app.badgeOn')}</span>;
  };

  /* ─── Command actions ─── */
  const cmdActions=useMemo(()=>[
    {id:'add',title:t('palette.addExpense'),icon:I.Plus,kbd:'⌘N',run:()=>{document.querySelector('.amount-field')?.focus()}},
    {id:'theme',title:t('palette.openTheme'),icon:I.Palette,kbd:'⌘,',run:()=>{setDrawerTab('theme');setShowDrawer(true)}},
    {id:'catalog',title:t('palette.manageCategories'),icon:I.Wallet,run:()=>{setDrawerTab('cats');setShowDrawer(true)}},
    {id:'budget',title:t('palette.editBudget'),icon:I.Settings,run:()=>{if(settings){setDraftBudget(String(settings.monthlyBudget));setDraftDays(String(settings.periodDays));setDraftStartDate(settings.startDate);setDraftBalance(String(balance?.start||0));setShowSetup(true)}}},
    {id:'topup',title:balancesOn?t('palette.moveMoney'):t('palette.topUp'),icon:balancesOn?I.Wallet:I.Zap,run:()=>{if(settings){setMoveMode("budget");setShowTopUp(true)}}},
    {id:'darklight',title:isDark?t('palette.switchToLight'):t('palette.switchToDark'),icon:isDark?I.Sun:I.Moon,run:toggleLightDark},
    {id:'backup',title:t('palette.downloadBackup'),icon:I.Download,run:exportData},
    {id:'csv',title:t('palette.exportCsv'),icon:I.Download,run:exportCSV},
    {id:'restore',title:t('palette.restoreBackup'),icon:I.Upload,run:triggerImport},
    {id:'compact',title:prefs.compact?t('palette.comfortableDensity'):t('pref.compact'),icon:I.Chart,run:()=>persistPrefs({...prefs,compact:!prefs.compact})},
    {id:'clear',title:t('palette.clearAll'),icon:I.Trash,run:handleClearAll},
  ],[isDark,settings,prefs,expenses,balance,categories,topUps,piggies,recurring,catBudgets,balancesOn]);

  /* ─── Keyboard ─── */
  useEffect(()=>{
    const handler=e=>{
      if(e.key==='Escape'){
        if(editingId){cancelEdit();return}
        if(showCmd){setShowCmd(false);return}
        if(showDrawer){setShowDrawer(false);return}
        if(showSetup){setShowSetup(false);return}
        if(showTopUp){setShowTopUp(false);return}
      }
      const meta=e.ctrlKey||e.metaKey;
      if(meta&&e.key.toLowerCase()==='k'){e.preventDefault();setShowCmd(true)}
      if(meta&&e.key.toLowerCase()==='n'&&settings){e.preventDefault();document.querySelector('.amount-field')?.focus()}
      if(meta&&e.key===','){e.preventDefault();setDrawerTab('theme');setShowDrawer(true)}
      const tag=(e.target.tagName||'').toLowerCase();
      const typing=tag==='input'||tag==='textarea'||e.target.isContentEditable;
      if(!meta&&e.key==='/'&&!typing){e.preventDefault();setShowFilters(true);document.querySelector('.history-search-field')?.focus()}
    };
    window.addEventListener('keydown',handler);
    return()=>window.removeEventListener('keydown',handler);
  },[editingId,settings,showDrawer,showSetup,showTopUp,showCmd]);

  /* Lock background scroll while any overlay is open (helps on mobile). */
  useEffect(()=>{
    const locked=showDrawer||showSetup||showTopUp||showCmd||!!confirm;
    const prev=document.body.style.overflow;
    document.body.style.overflow=locked?'hidden':'';
    return()=>{document.body.style.overflow=prev};
  },[showDrawer,showSetup,showTopUp,showCmd,confirm]);

  /* 3D tilt — lean panels toward the cursor (setting-controlled). */
  useEffect(()=>{
    if(prefs.tilt)tiltEnable();else tiltDisable();
    return ()=>tiltDisable();
  },[prefs.tilt]);

  /* Re-lock after the tab has been in the background for a while. */
  useEffect(()=>{
    if(!prefs.appLock)return;
    const onVisibility=()=>{
      if(document.hidden){lockAwayRef.current=Date.now()}
      else if(lockAwayRef.current&&Date.now()-lockAwayRef.current>30000){
        lockAwayRef.current=0;
        const rec=getLock();if(rec&&rec.hash)setLocked(true);
      }
    };
    document.addEventListener('visibilitychange',onVisibility);
    return()=>document.removeEventListener('visibilitychange',onVisibility);
  },[prefs.appLock]);

  /* ═══════════════════════════════════════════
     RENDER
     ═══════════════════════════════════════════ */
  /* ─── Reorderable cards ─── */
  const cardOrder=mergeCardOrder(prefs.cardOrder,DEFAULT_CARD_ORDER).filter(id=>balancesOn||id!=='piggy');
  const moveCard=(id,dir)=>{
    const cur=cardOrder;
    const idx=cur.indexOf(id);
    const swapWith=idx+dir;
    if(swapWith<0||swapWith>=cur.length)return;
    const next=[...cur];
    [next[idx],next[swapWith]]=[next[swapWith],next[idx]];
    persistPrefs({...prefs,cardOrder:next});
  };
  const dragCardId=useRef(null);
  const handleCardDragStart=id=>e=>{dragCardId.current=id;e.dataTransfer.effectAllowed='move'};
  const handleCardDragOver=e=>{e.preventDefault();e.dataTransfer.dropEffect='move'};
  const handleCardDrop=id=>e=>{
    e.preventDefault();
    const fromId=dragCardId.current;
    dragCardId.current=null;
    if(!fromId||fromId===id)return;
    const cur=cardOrder;
    const from=cur.indexOf(fromId),to=cur.indexOf(id);
    if(from<0||to<0)return;
    const next=[...cur];
    next.splice(from,1);
    next.splice(to,0,fromId);
    persistPrefs({...prefs,cardOrder:next});
  };
  const resetCardOrder=()=>{persistPrefs({...prefs,cardOrder:DEFAULT_CARD_ORDER});showToast(t('toast.cardOrderReset'),'success')};

  const startCatBudgetEdit=()=>{setCatBudgetDraft({...catBudgets});setCatBudgetEdit(true)};
  const setCatBudgetField=(id,v)=>setCatBudgetDraft(d=>({...d,[id]:v}));
  const saveCatBudgets=()=>{
    const next={};
    for(const c of cats){
      const v=parseFloat(catBudgetDraft[c.id]);
      if(isFinite(v)&&v>0)next[c.id]=v;
    }
    persistCatBudgets(next);
    setCatBudgetEdit(false);
    showToast(t('toast.catBudgetsSaved'),"success");
  };
  const catBarWidth=c=>{
    const b=catBudgets[c.id];
    if(b>0)return Math.min(100,(categoryTotals[c.id]/b)*100);
    return (categoryTotals[c.id]/maxCategory)*100;
  };
  const catBarColor=c=>{
    const b=catBudgets[c.id];
    if(b>0&&categoryTotals[c.id]>b)return theme.negative;
    return c.color;
  };

  const CARD_LABELS={log:t('app.cardLabel.log'),breakdown:t('app.cardLabel.breakdown'),trend:t('app.cardLabel.trend'),history:t('app.cardLabel.history'),insights:t('app.cardLabel.insights'),streak:t('app.cardLabel.streak'),auto:t('app.cardLabel.auto'),piggy:t('app.cardLabel.piggy'),backup:t('app.cardLabel.backup')};
  /* Shared with the travel home page, which reuses the same entry form with the
     foreign currency swapped in. */
  const logProps={
    cats,editingId,amount,setAmount,note,setNote:onNoteChange,entryDate,setEntryDate,today,
    selCats,toggleSelCat,frequentEntries,applyFrequent,addExpense,updateExpense,cancelEdit,addFormRef,
    receipt,onReceiptFile:attachReceipt,onRemoveReceipt:removeReceipt,onViewReceipt:setReceiptView,
    tags,addTag,removeTag,
  };
  const cardMap={
    log:<LogCard {...logProps} cur={cur}/>,
    breakdown:<BreakdownCard
      cats={cats} catBudgets={catBudgets} catBudgetEdit={catBudgetEdit} catBudgetDraft={catBudgetDraft}
      startCatBudgetEdit={startCatBudgetEdit} setCatBudgetField={setCatBudgetField} saveCatBudgets={saveCatBudgets}
      overviewRange={overviewRange} setOverviewRange={setOverviewRange} ovFrom={ovFrom} setOvFrom={setOvFrom}
      ovTo={ovTo} setOvTo={setOvTo} today={today} MYR={MYR} totalSpent={totalSpent} rangeLabel={rangeLabel}
      budgetPct={budgetPct} rangeBudget={rangeBudget} rangeDays={rangeDays} avgPerDayInRange={avgPerDayInRange}
      topCategory={topCategory} categoryTotals={categoryTotals} biggestInRange={biggestInRange}
      overviewExpenses={overviewExpenses} pieSlices={pieSlices} prefs={prefs} catBarWidth={catBarWidth} catBarColor={catBarColor}
    />,
    trend:<TrendCard
      prefs={prefs} trendRange={trendRange} setTrendRange={setTrendRange} trendSeries={trendSeries}
      toggleTrendSeries={toggleTrendSeries} cats={cats} theme={theme} trendData={trendData}
      trendSeriesList={trendSeriesList} trendMax={trendMax} trendHover={trendHover} setTrendHover={setTrendHover}
      heatData={heatData} heatColors={heatColors} heatCell={heatCell} heatWrapRef={heatWrapRef}
      MYR={MYR} today={today} relativeDate={relativeDate} dailyBudget={dailyBudget}
    />,
    history:<HistoryCard
      expenses={expenses} topUps={topUps} cats={cats} filterCats={filterCats} toggleFilterCat={toggleFilterCat}
      setFilterCats={setFilterCats} showFilters={showFilters} setShowFilters={setShowFilters}
      historySearch={historySearch} setHistorySearch={setHistorySearch} historySort={historySort}
      setHistorySort={setHistorySort} dateFrom={dateFrom} setDateFrom={setDateFrom} dateTo={dateTo}
      setDateTo={setDateTo} activeFilterCount={activeFilterCount} resetFilters={resetFilters}
      historyList={historyList} historySpentTotal={historySpentTotal} historyToppedTotal={historyToppedTotal}
      groupedHistory={groupedHistory} MYR={MYR} today={today} balancesOn={balancesOn}
      startEdit={startEdit} duplicateExpense={duplicateExpense} removeExpense={removeExpense} removeTopUp={removeTopUp}
      onViewReceipt={setReceiptView}
      filterTags={filterTags} toggleFilterTag={toggleFilterTag} setFilterTags={setFilterTags} allTags={allTags}
    />,
    insights:<InsightsCard
      insights={monthInsights} MYR={MYR} today={today} relativeDate={relativeDate}
    />,
    streak:<StreakCard
      streak={spendStreak} best={bestStreak} loggedToday={loggedToday} grace={prefs.streakGrace||0}
    />,
    auto:<AutoCard
      autoType={autoType} setAutoType={setAutoType} autoAmount={autoAmount} setAutoAmount={setAutoAmount}
      autoCat={autoCat} setAutoCat={setAutoCat} autoFreq={autoFreq} setAutoFreq={setAutoFreq}
      autoStart={autoStart} setAutoStart={setAutoStart} autoNote={autoNote} setAutoNote={setAutoNote}
      addAutomation={addAutomation} cats={cats} balancesOn={balancesOn} recurring={recurring}
      runRecurring={runRecurring} removeAutomation={removeAutomation} toggleAutomation={toggleAutomation}
      nextRun={nextRun} MYR={MYR} today={today}
    />,
    piggy:<PiggyCard
      piggies={piggies}
      activePiggyId={activePiggyId}
      setActivePiggyId={setActivePiggyId}
      addPiggy={addPiggy}
      renamePiggy={renamePiggy}
      savePiggyTarget={savePiggyTarget}
      depositPiggy={depositPiggy}
      breakPiggy={breakPiggy}
      deletePiggy={deletePiggy}
      updatePiggyTexture={updatePiggyTexture}
      updatePiggySound={updatePiggySound}
      MYR={MYR}
      showToast={showToast}
    />,
    backup:<BackupCard
      exportData={exportData} exportCSV={exportCSV} triggerImport={triggerImport} settings={settings}
      setDraftBudget={setDraftBudget} setDraftDays={setDraftDays} setDraftStartDate={setDraftStartDate}
      setDraftBalance={setDraftBalance} setShowSetup={setShowSetup} setMoveMode={setMoveMode}
      setShowTopUp={setShowTopUp} balancesOn={balancesOn} handleClearAll={handleClearAll} balance={balance}
    />,
  };

  return(
    <div className={`app ${prefs.compact?'compact':''}`} style={themeVars}>
      {prefs.wallpaper&&(
        <div className={`wallpaper-backdrop ${prefs.wallBlur>0?'blurred':''}`} style={isVideoWallpaper?{}:{backgroundImage:`url(${prefs.wallpaper})`}}>
          {isVideoWallpaper&&<video className="wallpaper-video" src={prefs.wallpaper} autoPlay loop muted playsInline/>}
          <div className="wallpaper-scrim" style={{background:theme.bg,opacity:prefs.wallpaperDim/100}}/>
        </div>
      )}
      <WeatherEffect type={prefs.weather} speed={prefs.weatherSpeed}/>
      <TopBar scrolled={scrolled} isDark={isDark} toggleLightDark={toggleLightDark} showDrawer={showDrawer} setShowDrawer={setShowDrawer} setDrawerTab={setDrawerTab} setShowCmd={setShowCmd} edgeBlur={prefs.edgeBlur!==false}/>
      <div className="shell">
        <input ref={fileInputRef} type="file" accept="application/json" style={{display:"none"}} onChange={handleImportFile}/>
        <input ref={wallpaperInputRef} type="file" accept="image/*,video/*" style={{display:"none"}} onChange={handleWallpaperFile}/>
        {!settings?(
          <SetupCard balancesOn={balancesOn} draftBudget={draftBudget} setDraftBudget={setDraftBudget} draftCurrency={draftCurrency} setDraftCurrency={setDraftCurrency} draftDays={draftDays} setDraftDays={setDraftDays} draftStartDate={draftStartDate} setDraftStartDate={setDraftStartDate} today={today} draftBalance={draftBalance} setDraftBalance={setDraftBalance} saveSetup={saveSetup} triggerImport={triggerImport} lang={prefs.lang||'en'} setLangPref={l=>persistPrefs({...prefs,lang:l})}/>
        ):(
          <>
            {travelActive?(
              <TravelHome
                travel={travel} cur={cur} MYR={MYR} travelList={travelList} travelByCurrency={travelByCurrency} cats={cats} today={today}
                editingId={editingId} startEdit={startEdit} removeExpense={removeExpense}
                onViewReceipt={setReceiptView} endTravel={endTravel} logProps={logProps}
              />
            ):(
              <>
            <Hero
              heroLabel={heroLabel} heroValue={heroValue} MYR={MYR} healthBadge={healthBadge}
              streak={streak} balancesOn={balancesOn} todaySaved={todaySaved} topUpTotal={topUpTotal}
              setMoveMode={setMoveMode} setShowTopUp={setShowTopUp} todayRemaining={todayRemaining}
              dailyBudget={dailyBudget} effectiveMonthlyBudget={effectiveMonthlyBudget} settings={settings}
              runningBalance={runningBalance} avgDailySpend={avgDailySpend} daysOver={daysOver}
              projectedTotal={projectedTotal} projectedDelta={projectedDelta} budgetPctFull={budgetPctFull}
              periodSpent={periodSpent} dayCells={dayCells} theme={theme} today={today}
              relativeDate={relativeDate} elapsedDays={elapsedDays} bankedSoFar={bankedSoFar}
            />
            <div className="cards-stack">
              {cardOrder.map((id,idx)=>(
                <div key={id} className="card-wrap"
                  style={{order:idx,gridColumn:(id==='log'||id==='breakdown')?'span 1':'1 / -1'}}
                  draggable
                  onDragStart={handleCardDragStart(id)}
                  onDragOver={handleCardDragOver}
                  onDrop={handleCardDrop(id)}>
                  <div className="card-reorder-bar">
                    <span className="card-drag-handle" title={t('app.dragToReorder')}><I.Grip/></span>
                    <span className="card-reorder-name">{CARD_LABELS[id]}</span>
                    <span className="card-reorder-actions">
                      <button className="card-reorder-btn" onClick={()=>moveCard(id,-1)} disabled={idx===0} title={t('app.moveUp')} aria-label={t('app.moveUp')}><I.ChevronUp/></button>
                      <button className="card-reorder-btn" onClick={()=>moveCard(id,1)} disabled={idx===cardOrder.length-1} title={t('app.moveDown')} aria-label={t('app.moveDown')}><I.ChevronDown/></button>
                    </span>
                  </div>
                  {cardMap[id]}
                </div>
              ))}
            </div>
              </>
            )}
          </>
        )}

        {showSetup&&settings&&(
          <BudgetDrawer
            cur={cur} persistPrefs={persistPrefs} prefs={prefs} balancesOn={balancesOn}
            draftBudget={draftBudget} setDraftBudget={setDraftBudget} draftDays={draftDays}
            setDraftDays={setDraftDays} draftStartDate={draftStartDate} setDraftStartDate={setDraftStartDate}
            today={today} draftBalance={draftBalance} setDraftBalance={setDraftBalance}
            saveSetup={saveSetup} onClose={()=>setShowSetup(false)}
          />
        )}

        {showTopUp&&settings&&(
          <MoneyDrawer
            balancesOn={balancesOn} moveMode={moveMode} setMoveMode={setMoveMode} bankBalance={bankBalance}
            topUpAmount={topUpAmount} setTopUpAmount={setTopUpAmount} topUpNote={topUpNote}
            setTopUpNote={setTopUpNote} submitMoney={submitMoney} topUps={topUps} MYR={MYR}
            today={today} relativeDate={relativeDate} removeTopUp={removeTopUp} cur={cur}
            onClose={()=>setShowTopUp(false)}
          />
        )}

        {showDrawer&&(
          <CustomizeDrawer
            drawerTab={drawerTab} setDrawerTab={setDrawerTab} onClose={()=>setShowDrawer(false)}
            prefs={prefs} persistPrefs={persistPrefs}
            isVideoWallpaper={isVideoWallpaper} triggerWallpaperUpload={triggerWallpaperUpload} clearWallpaper={clearWallpaper}
            theme={theme} activePresetKey={activePresetKey} applyPreset={applyPreset} updateColor={updateColor} updateCatColor={updateCatColor}
            allFontOptions={allFontOptions} draftFontName={draftFontName} setDraftFontName={setDraftFontName}
            addCustomFont={addCustomFont} removeCustomFont={removeCustomFont}
            triggerCardPanelUpload={triggerCardPanelUpload} clearCardPanel={clearCardPanel} resetCardOrder={resetCardOrder}
            cats={cats} categories={categories} removeCategory={removeCategory} addCategory={addCategory}
            newCatName={newCatName} setNewCatName={setNewCatName} newCatGlyph={newCatGlyph} setNewCatGlyph={setNewCatGlyph}
            heatColors={heatColors} cur={cur} balancesOn={balancesOn} heroMode={heroMode}
            showToast={showToast}
            authUser={authUser} signInGoogle={signInGoogle} signOutGoogle={signOutGoogle}
            syncError={syncError} syncErrorMsg={syncErrorMsg} lastSyncedAt={lastSyncedAt} resetTheme={resetTheme}
            requestNotifyPermission={requestNotifyPermission} onToggleAppLock={handleToggleAppLock}
            onChangePin={()=>setLockSetup(true)} onSetupBiometric={handleSetupBiometric}
            lockSet={!!(lockRec&&lockRec.hash)} biometricReady={biometricAvailable()}
            rateStatus={rateStatus} refreshRate={refreshTravelRate} travelByCurrency={travelByCurrency} endTravel={endTravel}
          />
        )}

        {showCmd&&<CommandPalette actions={cmdActions} onClose={()=>setShowCmd(false)}/>}

        {toast&&<Toast toast={toast} onDismiss={id=>setToast(t=>t&&t.id===id?null:t)}/>}

        {receiptView&&(
          <div className="confirm-overlay" onClick={()=>setReceiptView(null)}>
            <img className="receipt-full" src={receiptView} alt={t('app.receiptAlt')} onClick={e=>e.stopPropagation()}/>
          </div>
        )}

        {confirm&&<Confirm {...confirm}/>}

        {locked&&<LockScreen mode="unlock" onUnlock={handleUnlock} onForgot={handleForgotPin} biometric={biometricAvailable()} onBiometric={handleBiometric}/>}
        {lockSetup&&<LockScreen mode="setup" onSetup={handleSetPin} onCancel={()=>setLockSetup(false)}/>}
      </div>
    </div>
  );
}
