import { useEffect } from 'react';

/* ═══════════════════════════════════════════
   CONFIRM
   ═══════════════════════════════════════════ */
export default function Confirm({title,msg,onConfirm,onCancel,danger=true}){
  useEffect(()=>{
    const h=e=>{if(e.key==='Escape')onCancel();if(e.key==='Enter')onConfirm()};
    window.addEventListener('keydown',h);return()=>window.removeEventListener('keydown',h);
  },[onConfirm,onCancel]);
  return(
    <div className="confirm-overlay" onClick={onCancel}>
      <div className="confirm-box" onClick={e=>e.stopPropagation()}>
        <div className="confirm-title">{title}</div>
        <div className="confirm-msg">{msg}</div>
        <div className="confirm-actions">
          <button className="btn btn-ghost" onClick={onCancel}>Cancel</button>
          <button className={`btn ${danger?'btn-danger':''}`} onClick={onConfirm} autoFocus>Confirm</button>
        </div>
      </div>
    </div>
  );
}
