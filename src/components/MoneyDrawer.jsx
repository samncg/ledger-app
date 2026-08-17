import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';

/* Move Money / Top Up Drawer */
export default function MoneyDrawer({
  balancesOn,moveMode,setMoveMode,bankBalance,topUpAmount,setTopUpAmount,
  topUpNote,setTopUpNote,submitMoney,topUps,MYR,today,relativeDate,removeTopUp,
  cur,onClose,
}){
  return(
    <>
      <div className="drawer-overlay" onClick={onClose}/>
      <div className="drawer" style={{width:420}}>
        <div className="drawer-header">
          <span className="drawer-title">{balancesOn?<I.Wallet style={{width:15,height:15,verticalAlign:-2,marginRight:6}}/>:<I.Zap style={{width:15,height:15,verticalAlign:-2,marginRight:6}}/>}{balancesOn?'Money':'Top up budget'}</span>
          <button className="icon-btn" onClick={onClose}><I.Close/></button>
        </div>
        <div className="drawer-body">
          {balancesOn&&(
            <>
              <div className="drawer-tabs">
                <button className={`drawer-tab ${moveMode==='budget'?'active':''}`} onClick={()=>setMoveMode('budget')}>To budget</button>
                <button className={`drawer-tab ${moveMode==='return'?'active':''}`} onClick={()=>setMoveMode('return')}>To balance</button>
                <button className={`drawer-tab ${moveMode==='add'?'active':''}`} onClick={()=>setMoveMode('add')}>Add balance</button>
                <button className={`drawer-tab ${moveMode==='withdraw'?'active':''}`} onClick={()=>setMoveMode('withdraw')}>Withdraw balance</button>
              </div>
              <div className="totals-row" style={{marginTop:14}}>
                <span className="totals-label">Balance</span>
                <span className="totals-value mono">{MYR(bankBalance)}</span>
              </div>
              <div className="totals-row">
                <span className="totals-label">After</span>
                <span className="totals-value mono">
                  {MYR((moveMode==='return'||moveMode==='add')
                    ? bankBalance+(parseFloat(topUpAmount)||0)
                    : bankBalance-(parseFloat(topUpAmount)||0))}
                </span>
              </div>
            </>
          )}
          <div className="setup-form" style={{marginTop:balancesOn?14:0}}>
            <div>
              <div className="field-label">{!balancesOn?'Amount to add':moveMode==='budget'?"Amount to move to budget":moveMode==='return'?"Amount to return to balance":moveMode==='withdraw'?"Amount to withdraw":"Amount to add"}</div>
              <div className="currency-row">
                <select className="input currency-select" value={cur} disabled>
                  <option>{CURRENCIES[cur]?.symbol||cur}</option>
                </select>
                <input className="input mono" type="number" inputMode="decimal" placeholder="50" value={topUpAmount} onChange={e=>setTopUpAmount(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submitMoney()} autoFocus/>
              </div>
            </div>
            {(!balancesOn||moveMode==='budget'||moveMode==='return')&&(
              <div>
                <div className="field-label">Note (optional)</div>
                <input className="input" type="text" placeholder={!balancesOn?"e.g. bonus, birthday money":moveMode==='return'?"e.g. took out the extra food money":"e.g. extra cash for food"} value={topUpNote} onChange={e=>setTopUpNote(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submitMoney()}/>
              </div>
            )}
            <div className="hero-stat-note" style={{fontSize:12}}>
              {!balancesOn
                ? "Added to your total monthly budget — your daily allowance rises for the rest of the period."
                : moveMode==='budget'
                  ? "Moved out of your balance into this month's budget — your daily allowance rises for the rest of the period."
                  : moveMode==='return'
                    ? "Moves money from your budget back to your balance — you can only take back what you moved in."
                    : moveMode==='withdraw'
                      ? "Removes money from your balance, e.g. to spend it elsewhere — it stays gone even if you stay under budget."
                      : "Money you add from outside the app — it raises your balance and is protected from spending."}
            </div>
          </div>
          {(!balancesOn||moveMode==='budget'||moveMode==='return')&&topUps.length>0&&(
            <div style={{marginTop:20}}>
              <div className="field-label" style={{marginBottom:8}}>Recent {balancesOn?'transfers':'top-ups'}</div>
              <div className="tx-list">
                {[...topUps].reverse().slice(0,8).map(t=>(
                  <div className="tx-row" key={t.id} style={{'--cat-color':t.amount<0?'var(--warning)':'var(--positive)'}}>
                    <span className="tx-glyph">{t.amount>=0?(balancesOn?<I.Wallet style={{width:14,height:14}}/>:<I.Zap style={{width:14,height:14}}/>):<I.Wallet style={{width:14,height:14}}/>}</span>
                    <div className="tx-main">
                      <div className="tx-cat">{t.amount>=0?MYR(t.amount):'-'+MYR(Math.abs(t.amount))}</div>
                      {t.note&&<div className="tx-note">{t.note}</div>}
                    </div>
                    <span className="tx-date">{relativeDate(t.date,today)}</span>
                    <div className="tx-actions">
                      <button className="tx-action-btn danger" onClick={()=>removeTopUp(t.id)} title="Remove"><I.Trash/></button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="drawer-footer" style={{display:'flex',gap:8}}>
          <button className="btn" style={{flex:1}} onClick={submitMoney}>
            <I.Plus/> {!balancesOn?'Add funds':moveMode==='budget'?'Move to budget':moveMode==='return'?'Return to balance':moveMode==='withdraw'?'Withdraw':'Add to balance'}
          </button>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        </div>
      </div>
    </>
  );
}
