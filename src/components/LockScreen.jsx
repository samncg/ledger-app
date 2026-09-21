import { useEffect, useRef, useState } from 'react';
import { I } from '../lib/icons';
import { t } from '../lib/i18n';

/* ═══════════════════════════════════════════
   LOCK SCREEN — PIN gate (with optional device unlock)
   mode 'unlock' verifies an existing PIN; mode 'setup' creates one.
   ═══════════════════════════════════════════ */
export default function LockScreen({mode='unlock',onUnlock,onSetup,onCancel,onForgot,biometric=false,onBiometric}){
  const [pin,setPin]=useState("");
  const [confirmPin,setConfirmPin]=useState("");
  const [err,setErr]=useState("");
  const [busy,setBusy]=useState(false);
  const inputRef=useRef(null);

  useEffect(()=>{if(mode==='unlock'&&inputRef.current)inputRef.current.focus()},[mode]);

  const submit=async()=>{
    if(busy)return;
    setErr("");
    if(mode==='setup'){
      if(pin.length<4){setErr(t('lock.pinTooShort'));return}
      if(pin!==confirmPin){setErr(t('lock.pinMismatch'));return}
      setBusy(true);
      try{await onSetup(pin)}finally{setBusy(false)}
      return;
    }
    if(!pin){setErr(t('lock.enterPin'));return}
    setBusy(true);
    try{
      const ok=await onUnlock(pin);
      if(!ok){setErr(t('lock.incorrectPin'));setPin("")}
    }finally{setBusy(false)}
  };

  const tryBiometric=async()=>{
    if(busy||!onBiometric)return;
    setErr("");setBusy(true);
    try{const ok=await onBiometric();if(!ok)setErr(t('lock.biometricUnavailable'))}
    finally{setBusy(false)}
  };

  const title=mode==='setup'?t('lock.setupTitle'):t('lock.lockedTitle');

  return(
    <div className="lock-overlay">
      <div className="lock-box">
        <div className="lock-badge"><I.Target style={{width:22,height:22}}/></div>
        <div className="lock-title">{title}</div>
        <div className="lock-desc">
          {mode==='setup'
            ? t('lock.setupDesc')
            : t('lock.unlockDesc')}
        </div>
        <input ref={inputRef} className="input mono lock-input" type="password" inputMode="numeric" autoComplete="off"
          placeholder={mode==='setup'?t('lock.newPin'):t('lock.pin')} value={pin}
          onChange={e=>{setPin(e.target.value.replace(/\D/g,''))}}
          onKeyDown={e=>e.key==='Enter'&&submit()}/>
        {mode==='setup'&&(
          <input className="input mono lock-input" type="password" inputMode="numeric" autoComplete="off"
            placeholder={t('lock.confirmPin')} value={confirmPin}
            onChange={e=>{setConfirmPin(e.target.value.replace(/\D/g,''))}}
            onKeyDown={e=>e.key==='Enter'&&submit()}/>
        )}
        {err&&<div className="lock-err">{err}</div>}
        <button className="btn btn-block" onClick={submit} disabled={busy}>{mode==='setup'?t('lock.enable'):t('lock.unlock')}</button>
        {mode==='setup'
          ?<button className="btn btn-ghost btn-block" style={{marginTop:8}} onClick={onCancel}>{t('lock.cancel')}</button>
          :<>
            {biometric&&<button className="btn btn-secondary btn-block" style={{marginTop:8}} onClick={tryBiometric} disabled={busy}><I.Check/> {t('lock.useDevice')}</button>}
            <button className="link-btn lock-forgot" onClick={onForgot}>{t('lock.forgot')}</button>
          </>}
      </div>
    </div>
  );
}
