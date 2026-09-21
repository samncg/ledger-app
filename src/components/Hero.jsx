import { I } from '../lib/icons';
import { t } from '../lib/i18n';

/* Hero — daily allowance, balance, and the budget progress strip */
export default function Hero({
  heroLabel,heroValue,MYR,healthBadge,streak,balancesOn,todaySaved,topUpTotal,
  setMoveMode,setShowTopUp,todayRemaining,dailyBudget,effectiveMonthlyBudget,settings,
  runningBalance,avgDailySpend,daysOver,projectedTotal,projectedDelta,
  budgetPctFull,periodSpent,dayCells,theme,today,relativeDate,elapsedDays,bankedSoFar,
}){
  return(
    <div className="hero fade-in">
      <div className="hero-accent"/>
      <div className="hero-glow"/>
      <div className="hero-top">
        <div>
          <div className="hero-label">{heroLabel}</div>
          <div className={`hero-number mono ${heroValue<0?'negative':''}`}>{MYR(heroValue)}</div>
          <div className="hero-meta">
            {healthBadge()}
            {streak>=2&&(
              <span className="streak-badge">
                <I.Fire style={{width:12,height:12}}/> {t('hero.daysUnder',{days:streak})}
              </span>
            )}
            {balancesOn&&todaySaved>0&&(
              <span className="hero-badge pos"><I.Wallet style={{width:11,height:11}}/> {t('hero.savedToday',{amount:MYR(todaySaved)})}</span>
            )}
            {topUpTotal>0&&(
              <span className="hero-badge pos"><I.Zap style={{width:11,height:11}}/> {MYR(topUpTotal)} {balancesOn?t('history.movedToBudget'):t('history.toppedUp')}</span>
            )}
          </div>
        </div>
        <button className="btn btn-secondary btn-sm" onClick={()=>{setMoveMode("budget");setShowTopUp(true)}} title={balancesOn?t('hero.moveMoney'):t('hero.topUp')}>
          {balancesOn?<I.Wallet style={{width:14,height:14}}/>:<I.Zap style={{width:14,height:14}}/>} {balancesOn?t('hero.moveMoney'):t('hero.topUp')}
        </button>
      </div>
      {todayRemaining<0&&(()=>{
        /* The sentence wraps the amount in a <strong>, so split the translation on
           the placeholder and slot the formatted figure between the halves. */
        const parts=t('hero.overTodayBy',{amount:'\u0000'}).split('\u0000');
        return(
          <div className="overspend">
            {parts[0]}<strong className="mono">{MYR(Math.abs(todayRemaining))}</strong>{parts[1]}
          </div>
        );
      })()}
      <div className="hero-stats">
        <div>
          <div className="hero-stat-label">{t('hero.dailyAllowance')}</div>
          <div className="hero-stat-value">{MYR(dailyBudget)}</div>
          <div className="hero-stat-note">{MYR(effectiveMonthlyBudget)} / {settings.periodDays}d</div>
        </div>
        {balancesOn?(
          <div>
            <div className="hero-stat-label">{t('hero.savedToBalance')}</div>
            <div className="hero-stat-value" style={{color:bankedSoFar<0?'var(--negative)':'var(--positive)'}}>{MYR(bankedSoFar)}</div>
            <div className="hero-stat-note">{t('hero.leftoverBanked')}</div>
          </div>
        ):(
          <div>
            <div className="hero-stat-label">{runningBalance<0?t('hero.totalOver'):t('hero.rollover')}</div>
            <div className="hero-stat-value" style={{color:runningBalance<0?'var(--negative)':'var(--positive)'}}>{MYR(Math.abs(runningBalance))}</div>
            <div className="hero-stat-note">{runningBalance<0?t('hero.spentOverAllowance'):t('hero.unspentCarries')}</div>
          </div>
        )}
        <div>
          <div className="hero-stat-label">{t('hero.avgPerDay')}</div>
          <div className="hero-stat-value">{MYR(avgDailySpend)}</div>
          <div className="hero-stat-note">
            {avgDailySpend<=dailyBudget?t('hero.underAllowance'):t('hero.overAllowance')}
            {daysOver>0&&` · ${daysOver}d`}
          </div>
        </div>
        <div>
          <div className="hero-stat-label">{t('hero.projectedTotal')}</div>
          <div className="hero-stat-value" style={{color:projectedDelta<0?'var(--negative)':'var(--text)'}}>{MYR(projectedTotal)}</div>
          <div className="hero-stat-note">
            {projectedDelta<0?t('hero.overIfPace',{amount:MYR(Math.abs(projectedDelta))}):t('hero.leftIfPace',{amount:MYR(projectedDelta)})}
          </div>
        </div>
      </div>

      <div style={{marginBottom:22}}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'baseline',marginBottom:6}}>
          <span className="strip-label">{t('hero.budgetProgress')}</span>
          <span className="mono" style={{fontSize:11,color:'var(--text-dim)'}}>
            {MYR(periodSpent)} / {MYR(effectiveMonthlyBudget)} · {effectiveMonthlyBudget-periodSpent>=0?t('hero.left',{amount:MYR(effectiveMonthlyBudget-periodSpent)}):t('hero.over',{amount:MYR(periodSpent-effectiveMonthlyBudget)})}
          </span>
        </div>
        <div className="piggy-progress">
          <div className="piggy-progress-fill" style={{width:`${budgetPctFull}%`,background:periodSpent>effectiveMonthlyBudget?'var(--negative)':'var(--accent)'}}/>
        </div>
      </div>

      <div className="strip-wrap">
        <div className="strip-header">
          <span className="strip-label">{t('hero.dailySpendPeriod')}</span>
          <span className="strip-progress">{t('hero.dayOf',{day:elapsedDays,total:settings.periodDays})}</span>
        </div>
        <div className="strip">
          {dayCells.map(c=>{
            const pct=c.isFuture?8:Math.max(8,Math.min(100,(c.spent/((dailyBudget||1)*1.6))*100));
            let color=theme.borderStrong;
            /* Warn as soon as 3 or less is left for the day; red only once well over. */
            if(!c.isFuture)color=c.delta>3?theme.positive:c.delta>=-dailyBudget*0.4?theme.warning:theme.negative;
            return(
              <div key={c.date} className={`tick ${c.isToday?"today":""}`}
                title={`${relativeDate(c.date,today)} · Spent ${MYR(c.spent)} · ${c.delta>=0?'Left':'Over'} ${MYR(Math.abs(c.delta))}`}
                style={{height:`${pct}%`,background:color,opacity:c.isFuture?.35:1}}
              />
            );
          })}
        </div>
        <div className="strip-legend">
          <span><span className="legend-dot" style={{background:theme.positive}}/>{t('hero.under')}</span>
          <span><span className="legend-dot" style={{background:theme.warning}}/>{t('hero.near')}</span>
          <span><span className="legend-dot" style={{background:theme.negative}}/>{t('hero.over2')}</span>
          <span><span className="legend-dot" style={{background:theme.accent}}/>{t('hero.today')}</span>
        </div>
      </div>
    </div>
  );
}
