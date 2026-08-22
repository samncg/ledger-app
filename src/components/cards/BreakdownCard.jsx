import { I } from '../../lib/icons';

/* Category breakdown — range tabs, pie chart, per-category bars */
export default function BreakdownCard({
  cats,catBudgets,catBudgetEdit,catBudgetDraft,startCatBudgetEdit,setCatBudgetField,saveCatBudgets,
  overviewRange,setOverviewRange,ovFrom,setOvFrom,ovTo,setOvTo,today,MYR,
  totalSpent,rangeLabel,budgetPct,rangeBudget,rangeDays,avgPerDayInRange,topCategory,
  categoryTotals,biggestInRange,overviewExpenses,pieSlices,prefs,catBarWidth,catBarColor,
}){
  return(
    <div className="card fade-in stagger-2">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Chart/></span>
          Category breakdown
        </span>
        {catBudgetEdit?(
          <span style={{display:'flex',gap:8}}>
            <button className="link-btn" onClick={saveCatBudgets}>Save</button>
            <button className="link-btn" onClick={()=>setCatBudgetEdit(false)}>Cancel</button>
          </span>
        ):(
          <button className="link-btn" onClick={startCatBudgetEdit}>Budgets</button>
        )}
      </div>

      <div className="range-tabs">
        {[{id:"period",label:"Period"},{id:"week",label:"7d"},{id:"month",label:"Month"},{id:"all",label:"All"},{id:"custom",label:"Custom"}].map(r=>(
          <button key={r.id} className={`range-tab ${overviewRange===r.id?"active":""}`} onClick={()=>setOverviewRange(r.id)}>{r.label}</button>
        ))}
      </div>

      {overviewRange==="custom"&&(
        <div className="filter-row" style={{marginBottom:14}}>
          <div><div className="field-label">From</div><input className="input mono" type="date" value={ovFrom} max={ovTo||today} onChange={e=>setOvFrom(e.target.value)}/></div>
          <div><div className="field-label">To</div><input className="input mono" type="date" value={ovTo} min={ovFrom} max={today} onChange={e=>setOvTo(e.target.value)}/></div>
        </div>
      )}

      <div className="totals-row">
        <div>
          <div className="totals-label">Total spent</div>
          <div className="totals-value">{MYR(totalSpent)}</div>
          <div className="totals-caption">{rangeLabel}</div>
        </div>
        <div>
          <div className="totals-label">% of allowance</div>
          <div className="totals-value" style={{color:budgetPct>=100?'var(--negative)':budgetPct>=75?'var(--warning)':'var(--text)'}}>
            {budgetPct.toFixed(1)}%
          </div>
          <div className="totals-caption">vs {MYR(rangeBudget)} · {rangeDays}d</div>
        </div>
      </div>

      <div className="insights-row">
        <span className="insight-item">Avg spending/day <strong>{MYR(avgPerDayInRange)}</strong></span>
        <span className="insight-sep">·</span>
        <span className="insight-item">Top {topCategory?<strong>{topCategory.label}</strong>:<strong>—</strong>}{topCategory&&` (${MYR(categoryTotals[topCategory.id])})`}</span>
        <span className="insight-sep">·</span>
        <span className="insight-item">Txns <strong>{overviewExpenses.length}</strong></span>
        <span className="insight-sep">·</span>
        <span className="insight-item">Biggest {biggestInRange?<strong>{MYR(biggestInRange.amount)}</strong>:<strong>—</strong>}</span>
      </div>

      <div className="breakdown-body">
        <div className="pie-wrap">
          {pieSlices.length===0?(
            <>
              <svg viewBox="0 0 36 36" className="pie-svg"><circle cx="18" cy="18" r="15.9155" fill="none" stroke="var(--border-strong)" strokeWidth={prefs.pieThickness}/></svg>
              <div className="pie-center"><div className="pie-center-val">{MYR(0)}</div><div className="pie-center-sub">spent</div></div>
            </>
          ):(
            <>
              <svg viewBox="0 0 36 36" className="pie-svg">
                <circle cx="18" cy="18" r="15.9155" fill="none" stroke="var(--border)" strokeWidth={prefs.pieThickness}/>
                {pieSlices.map(s=>{
                  const gap=prefs.pieGap*0.3;
                  const dash=Math.max(0.1,s.dash-gap);
                  return(
                    <circle key={s.id} className="pie-slice" cx="18" cy="18" r="15.9155" fill="none"
                      stroke={s.color} strokeWidth={prefs.pieThickness}
                      strokeDasharray={`${dash} ${100-dash}`}
                      strokeDashoffset={25-s.offset} strokeLinecap="butt"
                    >
                      <title>{`${s.label}: ${MYR(s.value)} (${s.pct.toFixed(1)}%)`}</title>
                    </circle>
                  );
                })}
              </svg>
              <div className="pie-center">
                <div className="pie-center-val">{MYR(totalSpent)}</div>
                <div className="pie-center-sub">spent</div>
              </div>
            </>
          )}
        </div>

        {catBudgetEdit&&(
          <div className="cat-bars" style={{marginBottom:16}}>
            {cats.map(c=>(
              <div className="cat-bar-row" key={c.id}>
                <div className="cat-bar-label"><span className="cat-dot" style={{background:c.color}}/> {c.label}</div>
                <input className="input mono" type="number" inputMode="decimal" placeholder="No limit" value={catBudgetDraft[c.id]??''} onChange={e=>setCatBudgetField(c.id,e.target.value)} onKeyDown={e=>e.key==='Enter'&&saveCatBudgets()} style={{flex:1}}/>
              </div>
            ))}
          </div>
        )}
        <div className="cat-bars">
          {cats.map(c=>{
            const b=catBudgets[c.id];
            const over=b>0&&categoryTotals[c.id]>b;
            return(
              <div className="cat-bar-row" key={c.id}>
                <div className="cat-bar-label"><span className="cat-dot" style={{background:c.color}}/> {c.label}</div>
                <div className="cat-bar-track"><div className="cat-bar-fill" style={{width:`${catBarWidth(c)}%`,background:catBarColor(c)}}/></div>
                <div className="cat-bar-val">{MYR(categoryTotals[c.id])}{b>0&&<span style={{color:over?'var(--negative)':'var(--text-muted)',fontSize:11}}> / {MYR(b)}</span>}</div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
