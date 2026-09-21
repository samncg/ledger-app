import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';
import { firstOfMonthKey } from '../lib/helpers';
import { t } from '../lib/i18n';

/* Edit Budget Drawer */
export default function BudgetDrawer({
  cur,persistPrefs,prefs,balancesOn,draftBudget,setDraftBudget,draftDays,setDraftDays,
  draftStartDate,setDraftStartDate,today,draftBalance,setDraftBalance,saveSetup,onClose,
}){
  return(
    <>
      <div className="drawer-overlay" onClick={onClose}/>
      <div className="drawer" style={{width:460}}>
        <div className="drawer-header">
          <span className="drawer-title">{t('setup.budgetSettings')}</span>
          <button className="icon-btn" onClick={onClose}><I.Close/></button>
        </div>
        <div className="drawer-body">
          <div className="setup-form">
            <div>
              <div className="field-label">{t('setup.monthlyBudget')}</div>
              <div className="currency-row">
                <select className="input currency-select" value={cur} onChange={e=>persistPrefs({...prefs,currency:e.target.value})}>
                  {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
                </select>
                <input className="input mono" type="number" inputMode="decimal" value={draftBudget} onChange={e=>setDraftBudget(e.target.value)}/>
              </div>
            </div>
            {balancesOn&&(
              <div>
                <div className="field-label">{t('setup.bankBalance')}</div>
                <div className="currency-row">
                  <select className="input currency-select" value={cur} onChange={e=>persistPrefs({...prefs,currency:e.target.value})}>
                    {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
                  </select>
                  <input className="input mono" type="number" inputMode="decimal" value={draftBalance} onChange={e=>setDraftBalance(e.target.value)}/>
                </div>
                <div className="hero-stat-note" style={{fontSize:12}}>{t('setup.bankBalanceDesc')}</div>
                <div className="toggle-row">
                  <div>
                    <div className="toggle-label">{t('setup.overspendFromBalance')}</div>
                    <div className="toggle-desc">{t('setup.overspendDesc')}</div>
                  </div>
                  <button className={`toggle ${prefs.overspendFromBalance?'on':''}`} onClick={()=>persistPrefs({...prefs,overspendFromBalance:!prefs.overspendFromBalance})} title={t('setup.overspendToggle')}/>
                </div>
              </div>
            )}
            <div>
              <div className="field-label">{t('setup.periodDays')}</div>
              <input className="input mono" type="number" inputMode="numeric" value={draftDays} onChange={e=>setDraftDays(e.target.value)}/>
            </div>
            <div>
              <div className="field-label">{t('setup.startDate')}</div>
              <input className="input mono" type="date" value={draftStartDate} max={today} onChange={e=>setDraftStartDate(e.target.value)}/>
              <button className="link-btn" style={{marginTop:8}} onClick={()=>setDraftStartDate(firstOfMonthKey())}>{t('setup.realign')}</button>
            </div>
          </div>
        </div>
        <div className="drawer-footer" style={{display:'flex',gap:8}}>
          <button className="btn" style={{flex:1}} onClick={saveSetup}>{t('setup.saveChanges')}</button>
          <button className="btn btn-ghost" onClick={onClose}>{t('drawer.cancel')}</button>
        </div>
      </div>
    </>
  );
}
