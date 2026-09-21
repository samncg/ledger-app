import { I } from '../lib/icons';
import { CURRENCIES } from '../lib/constants';
import { expCats, fmt, relativeDate } from '../lib/helpers';
import { t } from '../lib/i18n';
import LogCard from './cards/LogCard';

/* Travel mode home — a self-contained page that replaces the dashboard while a trip is
   active. Amounts are entered in a foreign currency; every entry stores both the foreign
   figure and its home-currency equivalent, so budgets and statistics stay untouched.

   Each entry keeps the currency it was logged in, even if you switch the trip's currency
   later — older rows are never re-expressed in the new currency. */
export default function TravelHome({
  travel,cur,MYR,travelList,travelByCurrency=[],cats,today,editingId,
  startEdit,removeExpense,onViewReceipt,endTravel,logProps,
}){
  const code=travel.currency||'USD';
  const fc=CURRENCIES[code]||{code,symbol:code};
  const F=(n,c)=>fmt(n,c||code);
  const multi=travelByCurrency.length>1;
  const primary=travelByCurrency[0];

  /* While editing, the amount field follows the currency that entry was logged in, so an
     older entry is never silently re-expressed in the trip's current currency. */
  const editingEntry=editingId?travelList.find(e=>e.id===editingId):null;
  const logCurrency=editingEntry?.currency||code;

  const spentHome=travelList.reduce((s,e)=>s+e.amount,0);
  const todayHome=travelList.filter(e=>e.date===today).reduce((s,e)=>s+e.amount,0);

  return(
    <>
      <div className="hero fade-in travel-hero">
        <div className="hero-accent"/>
        <div className="hero-glow"/>
        <div className="hero-top">
          <div>
            <div className="hero-label">
              <I.Plane style={{width:12,height:12}}/> {travel.name||t('travel.active')} · {multi?t('travel.currencies',{n:travelByCurrency.length}):fc.code}
            </div>
            <div className="hero-number mono">{multi?F(spentHome,cur):F(primary?primary.foreign:0,code)}</div>
            <div className="hero-meta">
              {multi?(
                travelByCurrency.map(r=><span className="hero-badge" key={r.code}>{F(r.foreign,r.code)} · {r.count}</span>)
              ):(
                <>
                  <span className="hero-badge">{t('travel.equivalent',{amount:MYR(spentHome),home:cur})}</span>
                  {travel.rate>0&&(
                    <span className="hero-badge">{t('travel.rateHint',{foreign:fc.symbol,home:CURRENCIES[cur]?.symbol||cur})} {travel.rate}</span>
                  )}
                </>
              )}
              {todayHome>0&&<span className="hero-badge pos">{MYR(todayHome)} · {t('hero.today')}</span>}
            </div>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={endTravel} title={t('travel.end')}>
            <I.Close style={{width:14,height:14}}/> {t('travel.end')}
          </button>
        </div>

        <div className="hero-stats">
          <div>
            <div className="hero-stat-label">{t('travel.spent')}</div>
            <div className="hero-stat-value">{MYR(spentHome)}</div>
            <div className="hero-stat-note">{travelList.length} {travelList.length===1?t('travel.entry'):t('travel.entries')}</div>
          </div>
          <div>
            <div className="hero-stat-label">{t('travel.homeEquivalent')}</div>
            <div className="hero-stat-value" style={{color:'var(--text)'}}>{MYR(spentHome)}</div>
            <div className="hero-stat-note">{t('travel.homeCurrencyIs',{home:cur})}</div>
          </div>
          <div>
            <div className="hero-stat-label">{t('hero.today')}</div>
            <div className="hero-stat-value">{MYR(todayHome)}</div>
            <div className="hero-stat-note">{travel.rate>0?`1 ${code} ≈ ${MYR(travel.rate)}`:'—'}</div>
          </div>
        </div>
      </div>

      <div className="cards-stack">
        <div className="card-wrap" style={{order:0}}>
          <LogCard {...logProps} cur={logCurrency}/>
        </div>

        <div className="card-wrap" style={{order:1}}>
          <div className="card fade-in">
            <div className="card-title">
              <span className="card-title-left">
                <span className="card-title-icon"><I.History/></span>
                {travel.name||t('travel.title')}
                {travelList.length>0&&<span className="card-title-count">({travelList.length})</span>}
              </span>
            </div>

            {multi&&(
              <div className="travel-currency-totals">
                {travelByCurrency.map(r=>(
                  <div className="travel-currency-row" key={r.code}>
                    <span className="mono travel-currency-code">{r.code}</span>
                    <span className="mono travel-currency-foreign">{F(r.foreign,r.code)}</span>
                    <span className="mono travel-currency-home">≈ {MYR(r.home)}</span>
                    <span className="travel-currency-count">{r.count} {r.count===1?t('travel.entry'):t('travel.entries')}</span>
                  </div>
                ))}
              </div>
            )}

            <div className="tx-list scroll">
              {travelList.length===0&&(
                <div className="empty">
                  <div className="empty-illustration">✈</div>
                  {t('travel.noSpends')}
                  <div className="empty-sub">{t('travel.logSpend')}</div>
                </div>
              )}
              {[...travelList].sort((a,b)=>a.date<b.date?1:-1).map(e=>{
                const cat=cats.find(c=>c.id===expCats(e)[0]);
                const rowCur=e.currency||code;
                const foreign=typeof e.foreignAmount==='number'?e.foreignAmount:e.amount;
                return(
                  <div className="tx-row" key={e.id} style={{'--cat-color':cat?.color}}>
                    <span className="tx-glyph">{cat?.glyph}</span>
                    <div className="tx-main">
                      <div className="tx-cat">{cat?.label||expCats(e)[0]}</div>
                      {e.note&&<div className="tx-note">{e.note}</div>}
                      <div className="tx-note mono" style={{color:'var(--text-muted)'}}>{MYR(e.amount)}</div>
                      {e.receipt&&<img className="tx-receipt" src={e.receipt} alt={t('common.receipt')} onClick={()=>onViewReceipt&&onViewReceipt(e.receipt)}/>}
                    </div>
                    <span className="tx-date">{relativeDate(e.date,today)}</span>
                    <span className="tx-amount">{F(foreign,rowCur)}</span>
                    <div className="tx-actions">
                      <button className="tx-action-btn" onClick={()=>startEdit(e)} title={t('common.edit')} disabled={!!editingId}><I.Edit/></button>
                      <button className="tx-action-btn danger" onClick={()=>removeExpense(e.id)} title={t('common.delete')}><I.Trash/></button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
