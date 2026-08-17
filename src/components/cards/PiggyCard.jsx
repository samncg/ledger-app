import { I } from '../../lib/icons';
import { PIGGY_GIF } from '../../lib/constants';

/* Piggy bank — savings goal with flying-piggy GIF */
export default function PiggyCard({
  prefs,piggySaved,piggyTarget,piggyPct,piggyTargetEdit,piggyTargetDraft,setPiggyTargetDraft,
  setPiggyTargetEdit,savePiggyTarget,piggyAmount,setPiggyAmount,depositPiggy,
  piggyOpen,setPiggyOpen,breakPiggy,MYR,
}){
  return(
    <div className="card fade-in">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Target/></span>
          Piggy bank
          {piggyTarget>0&&<span className="card-title-count">{Math.round(piggyPct)}%</span>}
        </span>
        {!piggyTargetEdit&&(
          <button className="link-btn" onClick={()=>{setPiggyTargetDraft(piggyTarget>0?String(piggyTarget):"");setPiggyTargetEdit(true)}}>Set goal</button>
        )}
      </div>
      <div className="piggy-wrap">
        <div className="piggy-stage">
          <div style={{width:130,maxWidth:'45%',flexShrink:0}}>
            <img className={prefs.piggyTexture?"piggy-img":"piggy-img default"} src={prefs.piggyTexture||PIGGY_GIF} alt="Piggy bank"/>
          </div>
          <div className="piggy-info">
            {piggyTargetEdit?(
              <div className="currency-row">
                <input className="input mono" type="number" inputMode="decimal" placeholder="200" value={piggyTargetDraft} onChange={e=>setPiggyTargetDraft(e.target.value)} onKeyDown={e=>e.key==='Enter'&&savePiggyTarget()} autoFocus/>
                <button className="btn btn-sm" onClick={savePiggyTarget} title="Save goal"><I.Check/></button>
                <button className="btn btn-ghost btn-sm" onClick={()=>setPiggyTargetEdit(false)} title="Cancel"><I.Close/></button>
              </div>
            ):(
              <div className="totals-row" style={{paddingBottom:0,borderBottom:'none',marginBottom:0}}>
                <span className="totals-label">{piggyTarget>0?"Saved":"No goal yet"}</span>
                <span className="totals-value mono" style={{fontSize:20}}>{MYR(piggySaved)}{piggyTarget>0&&<span style={{color:'var(--text-muted)',fontSize:12,fontWeight:600}}> / {MYR(piggyTarget)}</span>}</span>
              </div>
            )}
            <div className="piggy-progress" style={{marginTop:10}}>
              <div className="piggy-progress-fill" style={{width:`${piggyPct}%`}}/>
            </div>
            <div className="hero-stat-note" style={{marginTop:6}}>
              {piggyTarget>0
                ? (piggySaved>=piggyTarget?"Goal complete! 🎉":`${MYR(piggyTarget-piggySaved)} to go`)
                : "Set a goal and watch it fill up."}
            </div>
            <div className="piggy-actions" style={{marginTop:10}}>
              <button className="btn btn-secondary btn-sm" onClick={()=>setPiggyOpen(o=>!o)}><I.Plus/> Add</button>
              <button className="btn btn-ghost btn-sm" onClick={breakPiggy} disabled={piggySaved<=0}>Break</button>
            </div>
            {piggyOpen&&(
              <div className="piggy-amt">
                <input className="input mono" type="number" inputMode="decimal" placeholder="20" value={piggyAmount} onChange={e=>setPiggyAmount(e.target.value)} onKeyDown={e=>e.key==='Enter'&&depositPiggy()} autoFocus/>
                <button className="btn btn-sm" onClick={depositPiggy}>Add</button>
                <button className="btn btn-ghost btn-sm" onClick={()=>setPiggyOpen(false)}><I.Close/></button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
