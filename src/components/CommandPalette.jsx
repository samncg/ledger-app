import { useEffect, useMemo, useRef, useState } from 'react';

/* ═══════════════════════════════════════════
   COMMAND PALETTE
   ═══════════════════════════════════════════ */
export default function CommandPalette({actions,onClose}){
  const[q,setQ]=useState("");
  const[idx,setIdx]=useState(0);
  const inputRef=useRef();
  const filtered=useMemo(()=>{
    if(!q.trim())return actions;
    const s=q.toLowerCase();
    return actions.filter(a=>a.title.toLowerCase().includes(s)||(a.keywords||"").toLowerCase().includes(s));
  },[q,actions]);
  useEffect(()=>{inputRef.current?.focus();setIdx(0)},[q]);
  useEffect(()=>{
    const h=e=>{
      if(e.key==='Escape'){onClose();return}
      if(e.key==='ArrowDown'){e.preventDefault();setIdx(i=>Math.min(i+1,filtered.length-1))}
      if(e.key==='ArrowUp'){e.preventDefault();setIdx(i=>Math.max(i-1,0))}
      if(e.key==='Enter'){e.preventDefault();const a=filtered[idx];if(a){a.run();onClose()}}
    };
    window.addEventListener('keydown',h);return()=>window.removeEventListener('keydown',h);
  },[idx,filtered,onClose]);
  return(
    <div className="cmd-overlay" onClick={onClose}>
      <div className="cmd-box" onClick={e=>e.stopPropagation()}>
        <input ref={inputRef} className="cmd-input" placeholder="Search commands…" value={q} onChange={e=>setQ(e.target.value)}/>
        <div className="cmd-list">
          {filtered.length===0&&<div className="cmd-empty">No commands match.</div>}
          {filtered.map((a,i)=>{
            const Icon=a.icon;
            return(
              <div key={a.id} className={`cmd-item ${i===idx?'active':''}`}
                onMouseEnter={()=>setIdx(i)}
                onClick={()=>{a.run();onClose()}}>
                <span className="cmd-item-icon"><Icon/></span>
                <span className="cmd-item-title">{a.title}</span>
                {a.kbd&&<span className="cmd-item-kbd kbd">{a.kbd}</span>}
              </div>
            );
          })}
        </div>
        <div className="cmd-hint-bar">
          <span><span className="kbd">↑↓</span> Navigate</span>
          <span><span className="kbd">↵</span> Select</span>
          <span><span className="kbd">Esc</span> Close</span>
        </div>
      </div>
    </div>
  );
}
