import { useEffect } from 'react';
import { t } from '../lib/i18n';

/* ═══════════════════════════════════════════
   CONFIRM
   ═══════════════════════════════════════════ */
export default function Confirm({title,msg,onConfirm,onCancel,danger=true}){
  useEffect(()=>{
    const h=e=>{if(e.key==='Escape')onCancel();if(e.key==='Enter'){e.preventDefault();onConfirm()}};
    window.addEventListener('keydown',h);return()=>window.removeEventListener('keydown',h);
  },[onConfirm,onCancel]);
  return(
    <div className="confirm-overlay" onClick={onCancel}>
      <div className="confirm-box" onClick={e=>e.stopPropagation()}>
        <div className="confirm-title">{title}</div>
        <div className="confirm-msg">{msg}</div>
        <div className="confirm-actions">
          <button className="btn btn-ghost" onClick={onCancel}>{t('confirm.cancel')}</button>
          <button className={`btn ${danger?'btn-danger':''}`} onClick={onConfirm} autoFocus>{t('confirm.confirm')}</button>
        </div>
      </div>
    </div>
  );
}
