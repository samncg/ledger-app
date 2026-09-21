import { I } from '../../lib/icons';
import { t } from '../../lib/i18n';

/* Spending trend — line chart or GitHub-style heatmap */
export default function TrendCard({
  prefs,trendRange,setTrendRange,trendSeries,toggleTrendSeries,cats,theme,
  trendData,trendSeriesList,trendMax,trendHover,setTrendHover,
  heatData,heatColors,heatCell,heatWrapRef,MYR,today,relativeDate,dailyBudget,
}){
  return(
    <div className="card fade-in stagger-3">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Trend/></span>
          {prefs.trendStyle==='heatmap'?t('card.trend.heatmapTitle'):t('card.trend.title')}
        </span>
        <div className="range-tabs" style={{marginBottom:0,padding:3}}>
          {(prefs.trendStyle==='heatmap'?[30,90,365]:[7,14,30]).map(n=>(
            <button key={n} className={`range-tab ${trendRange===n?'active':''}`} onClick={()=>setTrendRange(n)} style={{padding:'6px 12px',fontSize:11}}>{n<365?t('card.trend.days',{n}):t('card.trend.year')}</button>
          ))}
        </div>
      </div>
      {prefs.trendStyle==='heatmap'?(
        <div className="heatmap-wrap" ref={heatWrapRef}>
          <div className="heatmap-body">
            <div className="heat-days" style={{gridTemplateRows:`repeat(7,${heatCell}px)`,gap:3}}>
              <span className="heat-day-label" style={{gridRow:1}}>{t('card.trend.mon')}</span>
              <span className="heat-day-label" style={{gridRow:2}}>{t('card.trend.tue')}</span>
              <span className="heat-day-label" style={{gridRow:3}}>{t('card.trend.wed')}</span>
              <span className="heat-day-label" style={{gridRow:4}}>{t('card.trend.thu')}</span>
              <span className="heat-day-label" style={{gridRow:5}}>{t('card.trend.fri')}</span>
              <span className="heat-day-label" style={{gridRow:6}}>{t('card.trend.sat')}</span>
              <span className="heat-day-label" style={{gridRow:7}}>{t('card.trend.sun')}</span>
            </div>
            <div className="heatmap-grid" style={{gridTemplateColumns:`repeat(${heatData.weeks},${heatCell}px)`,gridTemplateRows:`repeat(7,${heatCell}px)`,gap:3}}>
              {heatData.cells.map(c=>(
                <div key={c.date} className="heat-cell"
                  style={{width:heatCell,height:heatCell,background:c.level===0?(heatColors.l0&&heatColors.l0!=='transparent'?heatColors.l0:'transparent'):heatColors['l'+c.level]}}
                  title={t('card.trend.cellTitle',{date:relativeDate(c.date,today),amount:MYR(c.spent)})}/>
              ))}
            </div>
          </div>
          <div className="heat-legend">
            <span className="heat-legend-label">{t('card.trend.less')}</span>
            {['l0','l1','l2','l3','l4'].map(k=>(
              <span key={k} className="heat-legend-cell" style={{background:(k==='l0'&&(heatColors.l0==='transparent'||!heatColors.l0))?'transparent':heatColors[k]}}/>
            ))}
            <span className="heat-legend-label">{t('card.trend.more')}</span>
            <span className="heat-legend-total">{t('card.trend.totalSpent',{amount:MYR(heatData.total)})}</span>
          </div>
        </div>
      ):(
      <>
      <div className="cat-pills" style={{marginBottom:8}}>
        <button className={`cat-pill ${trendSeries.includes("__total__")?"active":""}`} onClick={()=>toggleTrendSeries("__total__")} style={{'--cat-color':theme.accent}}>
          <span className="cat-dot"/> {t('card.trend.total')}
        </button>
        {cats.map(c=>(
          <button key={c.id} className={`cat-pill ${trendSeries.includes(c.id)?"active":""}`} onClick={()=>toggleTrendSeries(c.id)} style={{'--cat-color':c.color}}>
            <span className="cat-dot"/> {c.label}
          </button>
        ))}
      </div>
      <div className="section-desc" style={{marginTop:-4,marginBottom:14}}>{t('card.trend.pickHint')}</div>
      <div className="trend-chart">
        <svg className="trend-svg" viewBox={`0 0 ${trendData.length*40} 160`} preserveAspectRatio="none">
          <g className="trend-grid">
            {[0,0.25,0.5,0.75,1].map(f=>(
              <line key={f} x1="0" x2={trendData.length*40} y1={20+f*120} y2={20+f*120} vectorEffect="non-scaling-stroke"/>
            ))}
          </g>
          {dailyBudget>0&&(
            <line className="trend-baseline" x1="0" x2={trendData.length*40}
              y1={20+(1-Math.min(1,dailyBudget/trendMax))*120}
              y2={20+(1-Math.min(1,dailyBudget/trendMax))*120} vectorEffect="non-scaling-stroke"/>
          )}
          {trendSeriesList.map(s=>{
            const pts=trendData.map((d,i)=>({x:i*40+20,y:20+(1-Math.min(1,s.val(d)/trendMax))*120}));
            const path=pts.map((p,i)=>`${i===0?'M':'L'} ${p.x} ${p.y}`).join(' ');
            if(s.id==="__total__"){
              const area=`${path} L ${pts[pts.length-1].x} 140 L ${pts[0].x} 140 Z`;
              return(
                <g key={s.id}>
                  <path className="trend-area" d={area} style={{fill:s.color}}/>
                  <path className="trend-line" d={path} style={{stroke:s.color}} vectorEffect="non-scaling-stroke"/>
                </g>
              );
            }
            return <path key={s.id} className="trend-line" d={path} style={{stroke:s.color,strokeWidth:1.8}} vectorEffect="non-scaling-stroke"/>;
          })}
        </svg>
        {trendData.map((d,i)=>{
          const x=i*40+20;
          const y=20+(1-Math.min(1,d.total/trendMax))*120;
          const xPct=(x/(trendData.length*40))*100;
          const yPct=(y/160)*100;
          return(
            <div key={i} className="trend-dot-marker" style={{left:`${xPct}%`,top:`${yPct}%`}}
              onMouseEnter={()=>setTrendHover({day:d,idx:i})}
              onMouseLeave={()=>setTrendHover(null)}/>
          );
        })}
        {trendHover&&trendSeriesList.length>0&&(
          <div className="trend-tip" style={{left:`${(trendHover.idx+0.5)/trendData.length*100}%`,top:24,transform:'translateX(-50%)',whiteSpace:'normal'}}>
            <strong>{relativeDate(trendHover.day.date,today)}</strong>
            {trendSeriesList.map(s=>(
              <div key={s.id} style={{color:'var(--text-dim)'}}><span style={{color:s.color}}>●</span> {s.label}: {MYR(s.val(trendHover.day))}</div>
            ))}
          </div>
        )}
      </div>
      <div className="trend-legend">
        {trendSeriesList.map(s=>(
          <span key={s.id}><span className="legend-dot" style={{background:s.color}}/>{s.label}</span>
        ))}
        {dailyBudget>0&&<span><span className="legend-dot" style={{background:theme.warning}}/>{t('card.trend.allowance',{amount:MYR(dailyBudget)})}</span>}
      </div>
      </>
      )}
    </div>
  );
}
