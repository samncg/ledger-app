import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';
import { t } from '../lib/i18n';

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
          <span className="drawer-title">{balancesOn?<I.Wallet style={{width:15,height:15,verticalAlign:-2,marginRight:6}}/>:<I.Zap style={{width:15,height:15,verticalAlign:-2,marginRight:6}}/>}{balancesOn?t('drawer.money.title'):t('drawer.money.topUpTitle')}</span>
          <button className="icon-btn" onClick={onClose}><I.Close/></button>
        </div>
        <div className="drawer-body">
          {balancesOn&&(
            <>
              <div className="drawer-tabs">
                <button className={`drawer-tab ${moveMode==='budget'?'active':''}`} onClick={()=>setMoveMode('budget')}>{t('drawer.money.toBudget')}</button>
                <button className={`drawer-tab ${moveMode==='return'?'active':''}`} onClick={()=>setMoveMode('return')}>{t('drawer.money.toBalance')}</button>
                <button className={`drawer-tab ${moveMode==='add'?'active':''}`} onClick={()=>setMoveMode('add')}>{t('drawer.money.addBalance')}</button>
                <button className={`drawer-tab ${moveMode==='withdraw'?'active':''}`} onClick={()=>setMoveMode('withdraw')}>{t('drawer.money.withdrawBalance')}</button>
              </div>
              <div className="totals-row" style={{marginTop:14}}>
                <span className="totals-label">{t('drawer.money.balance')}</span>
                <span className="totals-value mono">{MYR(bankBalance)}</span>
              </div>
              <div className="totals-row">
                <span className="totals-label">{t('drawer.money.after')}</span>
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
              <div className="field-label">{!balancesOn?t('drawer.money.amountAdd'):moveMode==='budget'?t('drawer.money.amountToBudget'):moveMode==='return'?t('drawer.money.amountToReturn'):moveMode==='withdraw'?t('drawer.money.amountWithdraw'):t('drawer.money.amountAdd')}</div>
              <div className="currency-row">
                <select className="input currency-select" value={cur} disabled>
                  <option>{CURRENCIES[cur]?.symbol||cur}</option>
                </select>
                <input className="input mono" type="number" inputMode="decimal" placeholder="50" value={topUpAmount} onChange={e=>setTopUpAmount(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submitMoney()} autoFocus/>
              </div>
            </div>
            {(!balancesOn||moveMode==='budget'||moveMode==='return')&&(
              <div>
                <div className="field-label">{t('drawer.money.note')}</div>
                <input className="input" type="text" placeholder={!balancesOn?t('drawer.money.notePlaceholderAdd'):moveMode==='return'?t('drawer.money.notePlaceholderReturn'):t('drawer.money.notePlaceholderBudget')} value={topUpNote} onChange={e=>setTopUpNote(e.target.value)} onKeyDown={e=>e.key==='Enter'&&submitMoney()}/>
              </div>
            )}
            <div className="hero-stat-note" style={{fontSize:12}}>
              {!balancesOn
                ? t('drawer.money.descTopUp')
                : moveMode==='budget'
                  ? t('drawer.money.descToBudget')
                  : moveMode==='return'
                    ? t('drawer.money.descReturn')
                    : moveMode==='withdraw'
                      ? t('drawer.money.descWithdraw')
                      : t('drawer.money.descAddBalance')}
            </div>
          </div>
          {(!balancesOn||moveMode==='budget'||moveMode==='return')&&topUps.length>0&&(
            <div style={{marginTop:20}}>
              <div className="field-label" style={{marginBottom:8}}>{t('drawer.money.recent',{kind:balancesOn?t('drawer.money.transfers'):t('drawer.money.topUps')})}</div>
              <div className="tx-list">
                {[...topUps].reverse().slice(0,8).map(tx=>(
                  <div className="tx-row" key={tx.id} style={{'--cat-color':tx.amount<0?'var(--warning)':'var(--positive)'}}>
                    <span className="tx-glyph">{tx.amount>=0?(balancesOn?<I.Wallet style={{width:14,height:14}}/>:<I.Zap style={{width:14,height:14}}/>):<I.Wallet style={{width:14,height:14}}/>}</span>
                    <div className="tx-main">
                      <div className="tx-cat">{tx.amount>=0?MYR(tx.amount):'-'+MYR(Math.abs(tx.amount))}</div>
                      {tx.note&&<div className="tx-note">{tx.note}</div>}
                    </div>
                    <span className="tx-date">{relativeDate(tx.date,today)}</span>
                    <div className="tx-actions">
                      <button className="tx-action-btn danger" onClick={()=>removeTopUp(tx.id)} title={t('drawer.remove')}><I.Trash/></button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div className="drawer-footer" style={{display:'flex',gap:8}}>
          <button className="btn" style={{flex:1}} onClick={submitMoney}>
            <I.Plus/> {!balancesOn?t('drawer.money.actionAddFunds'):moveMode==='budget'?t('drawer.money.actionToBudget'):moveMode==='return'?t('drawer.money.actionReturn'):moveMode==='withdraw'?t('drawer.money.actionWithdraw'):t('drawer.money.actionAddBalance')}
          </button>
          <button className="btn btn-ghost" onClick={onClose}>{t('drawer.cancel')}</button>
        </div>
      </div>
    </>
  );
}
