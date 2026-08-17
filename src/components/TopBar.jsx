import { I } from '../lib/icons';

/* Top Bar — full width, content centered */
export default function TopBar({scrolled,isDark,toggleLightDark,showDrawer,setShowDrawer,setDrawerTab,setShowCmd}){
  return(
    <div className={`topbar ${scrolled?'scrolled':''}`}>
      <div className="topbar-inner">
        <div className="brand">
          <div className="brand-mark">L</div>
          <div>
            <div className="brand-name">Ledger</div>
            <div className="brand-sub">samncg.github.io/ledger</div>
          </div>
        </div>
        <div className="topbar-actions">
          <div className="cmd-hint" onClick={()=>setShowCmd(true)} title="Command palette">
            <I.Search style={{width:14,height:14}}/>
            <span>Quick actions</span>
            <span className="kbd">⌘K</span>
          </div>
          <button className="icon-btn" onClick={toggleLightDark} title="Toggle light/dark" aria-label="Toggle theme">
            {isDark?<I.Sun/>:<I.Moon/>}
          </button>
          <button className="icon-btn" onClick={()=>{setShowDrawer(open=>!open);if(!showDrawer)setDrawerTab('theme')}} title="Customize" aria-label="Customize">
            <I.Palette/>
          </button>
        </div>
      </div>
    </div>
  );
}
