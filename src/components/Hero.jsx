import { I } from '../lib/icons';

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
                <I.Fire style={{width:12,height:12}}/> {streak}-day streak
              </span>
            )}
            {balancesOn&&todaySaved>0&&(
              <span className="hero-badge pos"><I.Wallet style={{width:11,height:11}}/> {MYR(todaySaved)} saved today</span>
            )}
            {topUpTotal>0&&(
              <span className="hero-badge pos"><I.Zap style={{width:11,height:11}}/> {MYR(topUpTotal)} {balancesOn?'moved to budget':'topped up'}</span>
            )}
          </div>
        </div>
        <button className="btn btn-secondary btn-sm" onClick={()=>{setMoveMode("budget");setShowTopUp(true)}} title={balancesOn?"Move money between your balance and this month's budget":"Add extra funds to your budget"}>
          {balancesOn?<I.Wallet style={{width:14,height:14}}/>:<I.Zap style={{width:14,height:14}}/>} {balancesOn?'Move money':'Top up'}
        </button>
      </div>
      {todayRemaining<0&&(
        <div className="overspend">
          Over today's allowance by <strong className="mono">{MYR(Math.abs(todayRemaining))}</strong>
        </div>
      )}
      <div className="hero-stats">
        <div>
          <div className="hero-stat-label">Daily allowance</div>
          <div className="hero-stat-value">{MYR(dailyBudget)}</div>
          <div className="hero-stat-note">{MYR(effectiveMonthlyBudget)} / {settings.periodDays}d</div>
        </div>
        {balancesOn?(
          <div>
            <div className="hero-stat-label">Saved to balance</div>
            <div className="hero-stat-value" style={{color:'var(--positive)'}}>{MYR(bankedSoFar)}</div>
            <div className="hero-stat-note">Leftover allowance banked so far</div>
          </div>
        ):(
          <div>
            <div className="hero-stat-label">{runningBalance<0?"Total over":"Rollover"}</div>
            <div className="hero-stat-value" style={{color:runningBalance<0?'var(--negative)':'var(--positive)'}}>{MYR(Math.abs(runningBalance))}</div>
            <div className="hero-stat-note">{runningBalance<0?"Spent over allowance":"Unspent allowance carries over"}</div>
          </div>
        )}
        <div>
          <div className="hero-stat-label">Avg / day</div>
          <div className="hero-stat-value">{MYR(avgDailySpend)}</div>
          <div className="hero-stat-note">
            {avgDailySpend<=dailyBudget?"Under allowance":"Over allowance"}
            {daysOver>0&&` · ${daysOver}d over`}
          </div>
        </div>
        <div>
          <div className="hero-stat-label">Projected total</div>
          <div className="hero-stat-value" style={{color:projectedDelta<0?'var(--negative)':'var(--text)'}}>{MYR(projectedTotal)}</div>
          <div className="hero-stat-note">
            {projectedDelta<0?`${MYR(Math.abs(projectedDelta))} over if pace holds`:`${MYR(projectedDelta)} left if pace holds`}
          </div>
        </div>
      </div>

      <div style={{marginBottom:22}}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'baseline',marginBottom:6}}>
          <span className="strip-label">Budget progress</span>
          <span className="mono" style={{fontSize:11,color:'var(--text-dim)'}}>{MYR(periodSpent)} / {MYR(effectiveMonthlyBudget)}</span>
        </div>
        <div className="piggy-progress">
          <div className="piggy-progress-fill" style={{width:`${budgetPctFull}%`,background:periodSpent>effectiveMonthlyBudget?'var(--negative)':'var(--accent)'}}/>
        </div>
      </div>

      <div className="strip-wrap">
        <div className="strip-header">
          <span className="strip-label">Daily spend · this period</span>
          <span className="strip-progress">Day {elapsedDays} / {settings.periodDays}</span>
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
          <span><span className="legend-dot" style={{background:theme.positive}}/>Under</span>
          <span><span className="legend-dot" style={{background:theme.warning}}/>Near</span>
          <span><span className="legend-dot" style={{background:theme.negative}}/>Over</span>
          <span><span className="legend-dot" style={{background:theme.accent}}/>Today</span>
        </div>
      </div>
    </div>
  );
}
