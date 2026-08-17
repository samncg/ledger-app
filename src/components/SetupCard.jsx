import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';

/* ─── Setup ─── */
export default function SetupCard({
  balancesOn,draftBudget,setDraftBudget,draftCurrency,setDraftCurrency,
  draftDays,setDraftDays,draftStartDate,setDraftStartDate,today,
  draftBalance,setDraftBalance,saveSetup,triggerImport,
}){
  return(
    <div className="setup-wrap">
      <div className="setup-card fade-in">
        <div className="setup-icon">L</div>
        <div className="setup-title">Welcome to Ledger</div>
        <div className="setup-sub">
          {balancesOn
            ? <>Set a bank balance, track your daily allowance,<br/>and bank whatever you don't spend each day.</>
            : <>Set your budget, track your daily allowance,<br/>and carry over what you don't spend.</>}
        </div>
        <div className="setup-form">
          <div>
            <div className="field-label">Budget amount</div>
            <div className="currency-row">
              <select className="input currency-select" value={draftCurrency} onChange={e=>setDraftCurrency(e.target.value)}>
                {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
              </select>
              <input className="input mono" type="number" inputMode="decimal" placeholder="600" value={draftBudget} onChange={e=>setDraftBudget(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()} autoFocus/>
            </div>
          </div>
          {balancesOn&&(
            <div>
              <div className="field-label">Starting bank balance</div>
              <div className="currency-row">
                <select className="input currency-select" value={draftCurrency} onChange={e=>setDraftCurrency(e.target.value)}>
                  {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
                </select>
                <input className="input mono" type="number" inputMode="decimal" placeholder="1000" value={draftBalance} onChange={e=>setDraftBalance(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()}/>
              </div>
              <div className="hero-stat-note" style={{fontSize:12}}>The money you have right now — you can move it into your budget whenever you need it.</div>
            </div>
          )}
          <div>
            <div className="field-label">Period length (days)</div>
            <input className="input mono" type="number" inputMode="numeric" placeholder="30" value={draftDays} onChange={e=>setDraftDays(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()}/>
          </div>
          <div>
            <div className="field-label">Start date</div>
            <input className="input mono" type="date" value={draftStartDate} max={today} onChange={e=>setDraftStartDate(e.target.value)}/>
          </div>
          <button className="btn btn-block" onClick={saveSetup} style={{marginTop:8}}>
            <I.Plus/> Start tracking
          </button>
          <div className="setup-divider"><span>or</span></div>
          <button className="btn btn-block btn-ghost" onClick={triggerImport}>
            <I.Upload/> Restore from a backup
          </button>
          <div className="setup-sub" style={{marginTop:2,fontSize:11}}>
            Already have a ledger-backup .json file? Load it to pick up right where you left off.
          </div>
        </div>
      </div>
    </div>
  );
}
