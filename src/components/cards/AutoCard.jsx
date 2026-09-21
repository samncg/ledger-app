import { I } from '../../lib/icons';
import { FREQ_OPTIONS } from '../../lib/constants';
import { relativeDate } from '../../lib/helpers';
import { t } from '../../lib/i18n';

/* Automations — recurring entries (spending, top-ups, balance) */
const FREQ_KEYS={daily:'card.auto.freqDaily',weekly:'card.auto.freqWeekly',monthly:'card.auto.freqMonthly'};
const freqLabel=k=>FREQ_KEYS[k]?t(FREQ_KEYS[k]):k;
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
          {t('card.auto.title')}
          {recurring.length>0&&<span className="card-title-count">({recurring.length})</span>}
        </span>
        {recurring.length>0&&(
          <button className="link-btn" onClick={()=>runRecurring()} title={t('card.auto.runNowTitle')}>{t('card.auto.runNow')}</button>
        )}
      </div>
      <div className="setup-form">
        <div className="filter-row">
          <select className="input" value={autoType} onChange={e=>setAutoType(e.target.value)}>
            <option value="expense">{t('card.auto.typeExpense')}</option>
            <option value="budget">{t('card.auto.typeBudget')}</option>
            {balancesOn&&<option value="balance">{t('card.auto.typeBalance')}</option>}
          </select>
          <input className="input mono" type="number" inputMode="decimal" placeholder={t('card.auto.amountPlaceholder')} value={autoAmount} onChange={e=>setAutoAmount(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addAutomation()}/>
        </div>
        <div className="filter-row">
          {autoType==='expense'&&(
            <select className="input" value={autoCat} onChange={e=>setAutoCat(e.target.value)}>
              {cats.map(c=><option key={c.id} value={c.id}>{c.glyph} {c.label}</option>)}
            </select>
          )}
          <select className="input" value={autoFreq} onChange={e=>setAutoFreq(e.target.value)}>
            {Object.entries(FREQ_OPTIONS).map(([k])=><option key={k} value={k}>{freqLabel(k)}</option>)}
          </select>
          <input className="input mono" type="date" value={autoStart} max={today} onChange={e=>setAutoStart(e.target.value)}/>
        </div>
        <div className="filter-row">
          <input className="input" type="text" placeholder={t('card.auto.notePlaceholder')} value={autoNote} onChange={e=>setAutoNote(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addAutomation()}/>
          <button className="btn" onClick={addAutomation}><I.Plus/> {t('card.auto.add')}</button>
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
                  <div className="tx-cat">{freqLabel(r.freq)} · {MYR(r.amount)} {r.type==='expense'?`· ${cat?.label||r.category}`:r.type==='budget'?t('card.auto.toBudget'):t('card.auto.toBalance')}</div>
                  {r.note&&<div className="tx-note">{r.note}</div>}
                  <div className="auto-rule-note">{t('card.auto.started',{date:relativeDate(r.start,today),next:nextRun(r)})}</div>
                </div>
                <div className="tx-actions">
                  <button className={`toggle ${r.active?'on':''}`} onClick={()=>toggleAutomation(r)} title={r.active?t('card.auto.pause'):t('card.auto.resume')}/>
                  <button className="tx-action-btn danger" onClick={()=>removeAutomation(r.id)} title={t('card.auto.removeTitle')}><I.Trash/></button>
                </div>
              </div>
            );
          })}
        </div>
      ):(
        <div className="empty" style={{marginTop:12}}>
          <div className="empty-illustration">↻</div>
          {t('card.auto.empty')}
          <div className="empty-sub">{t('card.auto.emptySub')}</div>
        </div>
      )}
    </div>
  );
}
