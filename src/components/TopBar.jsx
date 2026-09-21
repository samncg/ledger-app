import { I } from '../lib/icons';
import { t } from '../lib/i18n';

/* Top Bar — full width, content centered */
export default function TopBar({scrolled,isDark,toggleLightDark,showDrawer,setShowDrawer,setDrawerTab,setShowCmd,edgeBlur=true}){
  return(
    <div className={`topbar ${scrolled?'scrolled':''} ${edgeBlur===false?'no-edge-blur':''}`}>
      <div className="topbar-inner">
        <div className="brand">
          <div className="brand-mark">L</div>
          <div>
            <div className="brand-name">Ledger</div>
            <div className="brand-sub">samncg.github.io/ledger</div>
          </div>
        </div>
        <div className="topbar-actions">
          <button type="button" className="cmd-hint" onClick={()=>setShowCmd(true)} title={t('palette.title')}>
            <I.Search style={{width:14,height:14}}/>
            <span>{t('top.quickActions')}</span>
            <span className="kbd">⌘K</span>
          </button>
          <button className="icon-btn" onClick={toggleLightDark} title={t('top.toggleTheme')} aria-label={t('top.toggleTheme')}>
            {isDark?<I.Sun/>:<I.Moon/>}
          </button>
          <button className="icon-btn" onClick={()=>{setShowDrawer(open=>!open);if(!showDrawer)setDrawerTab('theme')}} title={t('top.customize')} aria-label={t('top.customize')}>
            <I.Palette/>
          </button>
        </div>
      </div>
    </div>
  );
}
