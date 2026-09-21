import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';
import { daysInMonth } from '../lib/helpers';
import { LANGS, t } from '../lib/i18n';

/* ─── Setup ─── */
export default function SetupCard({
  balancesOn,draftBudget,setDraftBudget,draftCurrency,setDraftCurrency,
  draftDays,setDraftDays,draftStartDate,setDraftStartDate,today,
  draftBalance,setDraftBalance,saveSetup,triggerImport,lang,setLangPref,
}){
  return(
    <div className="setup-wrap">
      <div className="setup-card fade-in">
        <div className="setup-icon">L</div>
        <div className="setup-title">{t('setup.welcome')}</div>
        <div className="setup-sub">
          {balancesOn
            ? <>{t('setup.subBalances1')}<br/>{t('setup.subBalances2')}</>
            : <>{t('setup.subBudget1')}<br/>{t('setup.subBudget2')}</>}
        </div>
        <div className="setup-form">
          <div>
            <div className="field-label">{t('pref.language')}</div>
            <select className="input" value={lang||'en'} onChange={e=>setLangPref&&setLangPref(e.target.value)}>
              {LANGS.map(l=><option key={l.id} value={l.id}>{l.label}</option>)}
            </select>
          </div>
          <div>
            <div className="field-label">{t('setup.budgetAmount')}</div>
            <div className="currency-row">
              <select className="input currency-select" value={draftCurrency} onChange={e=>setDraftCurrency(e.target.value)}>
                {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
              </select>
              <input className="input mono" type="number" inputMode="decimal" placeholder="600" value={draftBudget} onChange={e=>setDraftBudget(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()} autoFocus/>
            </div>
          </div>
          {balancesOn&&(
            <div>
              <div className="field-label">{t('setup.startingBankBalance')}</div>
              <div className="currency-row">
                <select className="input currency-select" value={draftCurrency} onChange={e=>setDraftCurrency(e.target.value)}>
                  {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code}</option>)}
                </select>
                <input className="input mono" type="number" inputMode="decimal" placeholder="1000" value={draftBalance} onChange={e=>setDraftBalance(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()}/>
              </div>
              <div className="hero-stat-note" style={{fontSize:12}}>{t('setup.startingBankBalanceDesc')}</div>
            </div>
          )}
          <div>
            <div className="field-label">{t('setup.periodDays')}</div>
            <input className="input mono" type="number" inputMode="numeric" placeholder={String(daysInMonth())} value={draftDays} onChange={e=>setDraftDays(e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveSetup()}/>
          </div>
          <div>
            <div className="field-label">{t('setup.startDate')}</div>
            <input className="input mono" type="date" value={draftStartDate} max={today} onChange={e=>setDraftStartDate(e.target.value)}/>
          </div>
          <button className="btn btn-block" onClick={saveSetup} style={{marginTop:8}}>
            <I.Plus/> {t('setup.startTracking')}
          </button>
          <div className="setup-divider"><span>{t('setup.or')}</span></div>
          <button className="btn btn-block btn-ghost" onClick={triggerImport}>
            <I.Upload/> {t('setup.restore')}
          </button>
          <div className="setup-sub" style={{marginTop:2,fontSize:11}}>
            {t('setup.restoreDesc')}
          </div>
        </div>
      </div>
    </div>
  );
}
