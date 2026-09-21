import { I } from '../../lib/icons';
import { t } from '../../lib/i18n';

/* ═══════════════════════════════════════════
   MONTHLY INSIGHTS — this vs last month, pace, best/worst day
   ═══════════════════════════════════════════ */
export default function InsightsCard({insights,MYR,today,relativeDate}){
  const {thisTotal,lastTotal,pct,avg,projected,daysElapsed,daysInMonth,best,worst,biggest}=insights;
  const pctTxt=pct===null?'—':`${pct>=0?'+':''}${pct.toFixed(0)}%`;
  const pctColor=pct===null?'var(--text-muted)':pct>0?'var(--negative)':pct<0?'var(--positive)':'var(--text-dim)';
  const deltaColor=biggest?(biggest.delta>0?'var(--negative)':biggest.delta<0?'var(--positive)':'var(--text-dim)'):'var(--text-muted)';
  return(
    <div className="card fade-in stagger-3">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Trend/></span>
          {t('card.insights.title')}
        </span>
        <span className="card-title-count">{t('card.insights.dayCount',{elapsed:daysElapsed,total:daysInMonth})}</span>
      </div>

      <div className="insights-row">
        <span className="insight-item">{t('card.insights.thisMonth')} <strong>{MYR(thisTotal)}</strong></span>
        <span className="insight-sep">·</span>
        <span className="insight-item">{t('card.insights.lastMonth')} <strong>{MYR(lastTotal)}</strong></span>
        <span className="insight-sep">·</span>
        <span className="insight-item">{t('card.insights.change')} <strong style={{color:pctColor}}>{pctTxt}</strong></span>
      </div>

      <div className="insights-row" style={{marginBottom:0}}>
        <span className="insight-item">{t('card.insights.avgPerDay')} <strong>{MYR(avg)}</strong></span>
        <span className="insight-sep">·</span>
        <span className="insight-item">{t('card.insights.projected')} <strong>{MYR(projected)}</strong></span>
        {lastTotal>0&&<><span className="insight-sep">·</span><span className="insight-item">{t('card.insights.vsLastMonth')} <strong style={{color:projected>lastTotal?'var(--warning)':'var(--text)'}}>{projected>lastTotal?t('card.insights.over'):t('card.insights.under')}</strong></span></>}
      </div>

      <div className="insight-split">
        <div className="insight-block">
          <div className="insight-block-label">{t('card.insights.biggestChange')}</div>
          {biggest?(
            <div className="insight-block-value">
              {biggest.cat.glyph} {biggest.cat.label} <span style={{color:deltaColor,fontWeight:700}}>{biggest.delta>=0?'▲ +':'▼ '}{MYR(Math.abs(biggest.delta))}</span>
            </div>
          ):<div className="insight-block-value">—</div>}
        </div>
        <div className="insight-block">
          <div className="insight-block-label">{t('card.insights.bestDay')}</div>
          <div className="insight-block-value">{best?`${relativeDate(best.date,today)} · ${MYR(best.amount)}`:'—'}</div>
        </div>
        <div className="insight-block">
          <div className="insight-block-label">{t('card.insights.worstDay')}</div>
          <div className="insight-block-value">{worst?`${relativeDate(worst.date,today)} · ${MYR(worst.amount)}`:'—'}</div>
        </div>
      </div>
    </div>
  );
}
