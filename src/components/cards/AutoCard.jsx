import { I } from '../../lib/icons';
import { FREQ_OPTIONS } from '../../lib/constants';
import { relativeDate } from '../../lib/helpers';

/* Automations — recurring entries (spending, top-ups, balance) */
export default function AutoCard({
  autoType,setAutoType,autoAmount,setAutoAmount,autoCat,setAutoCat,autoFreq,setAutoFreq,
  autoStart,setAutoStart,autoNote,setAutoNote,addAutomation,cats,balancesOn,
  recurring,runRecurring,removeAutomation,toggleAutomation,nextRun,MYR,today,
}){
  return(
    <div className="card fade-in">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Zap/></span>
          Automations
          {recurring.length>0&&<span className="card-title-count">({recurring.length})</span>}
        </span>
        {recurring.length>0&&(
          <button className="link-btn" onClick={()=>runRecurring()} title="Materialize any due entries now">Run now</button>
        )}
      </div>
      <div className="setup-form">
        <div className="filter-row">
          <select className="input" value={autoType} onChange={e=>setAutoType(e.target.value)}>
            <option value="expense">Spending</option>
            <option value="budget">Top up budget</option>
            {balancesOn&&<option value="balance">Top up balance</option>}
          </select>
          <input className="input mono" type="number" inputMode="decimal" placeholder="Amount" value={autoAmount} onChange={e=>setAutoAmount(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addAutomation()}/>
        </div>
        <div className="filter-row">
          {autoType==='expense'&&(
            <select className="input" value={autoCat} onChange={e=>setAutoCat(e.target.value)}>
              {cats.map(c=><option key={c.id} value={c.id}>{c.glyph} {c.label}</option>)}
            </select>
          )}
          <select className="input" value={autoFreq} onChange={e=>setAutoFreq(e.target.value)}>
            {Object.entries(FREQ_OPTIONS).map(([k,l])=><option key={k} value={k}>{l}</option>)}
          </select>
          <input className="input mono" type="date" value={autoStart} max={today} onChange={e=>setAutoStart(e.target.value)}/>
        </div>
        <div className="filter-row">
          <input className="input" type="text" placeholder="Note (optional)" value={autoNote} onChange={e=>setAutoNote(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addAutomation()}/>
          <button className="btn" onClick={addAutomation}><I.Plus/> Add</button>
        </div>
      </div>
      {recurring.length>0?(
        <div className="tx-list" style={{marginTop:12}}>
          {recurring.map(r=>{
            const cat=cats.find(c=>c.id===r.category);
            return(
              <div className="tx-row" key={r.id} style={{opacity:r.active?1:.45}}>
                <span className="tx-glyph">{r.type==='expense'?(cat?.glyph||'◌'):r.type==='budget'?(balancesOn?<I.Wallet style={{width:14,height:14}}/>:<I.Zap style={{width:14,height:14}}/>):<I.Plus style={{width:14,height:14}}/>}</span>
                <div className="tx-main">
                  <div className="tx-cat">{FREQ_OPTIONS[r.freq]||r.freq} · {MYR(r.amount)} {r.type==='expense'?`· ${cat?.label||r.category}`:r.type==='budget'?'to budget':'to balance'}</div>
                  {r.note&&<div className="tx-note">{r.note}</div>}
                  <div className="auto-rule-note">Started {relativeDate(r.start,today)} · {nextRun(r)}</div>
                </div>
                <div className="tx-actions">
                  <button className={`toggle ${r.active?'on':''}`} onClick={()=>toggleAutomation(r)} title={r.active?"Pause":"Resume"}/>
                  <button className="tx-action-btn danger" onClick={()=>removeAutomation(r.id)} title="Remove"><I.Trash/></button>
                </div>
              </div>
            );
          })}
        </div>
      ):(
        <div className="empty" style={{marginTop:12}}>
          <div className="empty-illustration">↻</div>
          Repeating entries appear here.
          <div className="empty-sub">e.g. rent on the 1st, salary on the 25th.</div>
        </div>
      )}
    </div>
  );
}
