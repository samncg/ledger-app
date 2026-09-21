import { useState } from 'react';
import { I } from '../lib/icons';
import { CURRENCIES, PRESETS, CAT_COLOR_PRESETS, HEAT_PRESETS, HEAT_DEFAULT_COLORS } from '../lib/constants';
import { fmt, heatColorVal } from '../lib/helpers';
import { LANGS, t } from '../lib/i18n';
import { FIREBASE_CONFIGURED } from '../lib/firebase';

/* One collapsible block of the drawer. Every settings group lives behind its own
   button so the drawer reads as a short index instead of a long scroll. */
function Section({ title, desc, defaultOpen = false, children }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="sec">
      <button type="button" className={`sec-head ${open ? 'open' : ''}`} onClick={() => setOpen(v => !v)} aria-expanded={open}>
        <span className="sec-title">{title}</span>
        <span className={`chevron ${open ? 'open' : ''}`}>▾</span>
      </button>
      {open && (
        <div className="sec-body">
          {desc && <div className="section-desc">{desc}</div>}
          {children}
        </div>
      )}
    </div>
  );
}

/* Shared look for a label + toggle row. */
function ToggleRow({ label, desc, on, onToggle, title }) {
  return (
    <div className="toggle-row">
      <div>
        <div className="toggle-label">{label}</div>
        {desc && <div className="toggle-desc">{desc}</div>}
      </div>
      <button className={`toggle ${on ? 'on' : ''}`} onClick={onToggle} title={title || label} />
    </div>
  );
}

/* Theme / Chart / Categories / Prefs drawer */
export default function CustomizeDrawer({
  drawerTab,setDrawerTab,onClose,
  prefs,persistPrefs,
  isVideoWallpaper,triggerWallpaperUpload,clearWallpaper,
  theme,activePresetKey,applyPreset,updateColor,updateCatColor,
  allFontOptions,draftFontName,setDraftFontName,addCustomFont,removeCustomFont,
  triggerCardPanelUpload,clearCardPanel,resetCardOrder,
  cats,categories,removeCategory,addCategory,newCatName,setNewCatName,newCatGlyph,setNewCatGlyph,
  heatColors,cur,balancesOn,heroMode,
  showToast,
  authUser,signInGoogle,signOutGoogle,syncError,syncErrorMsg,lastSyncedAt,
  resetTheme,
  requestNotifyPermission,onToggleAppLock,onChangePin,onSetupBiometric,lockSet,biometricReady,
  rateStatus,refreshRate,travelByCurrency=[],endTravel,
}){
  const travel = { active: false, name: '', currency: 'USD', rate: 1, start: '', rateAuto: true, rateUpdatedAt: '', ...(prefs.travel||{}) };
  const setTravel = patch => persistPrefs({ ...prefs, travel: { ...travel, ...patch } });

  return(
    <>
      <div className="drawer-overlay" onClick={onClose}/>
      <div className="drawer">
        <div className="drawer-header">
          <span className="drawer-title">{t('top.customize')}</span>
          <button className="icon-btn" onClick={onClose}><I.Close/></button>
        </div>
        <div className="drawer-body">
          <div className="drawer-tabs">
            <button className={`drawer-tab ${drawerTab==='theme'?'active':''}`} onClick={()=>setDrawerTab('theme')}>{t('tab.theme')}</button>
            <button className={`drawer-tab ${drawerTab==='chart'?'active':''}`} onClick={()=>setDrawerTab('chart')}>{t('tab.chart')}</button>
            <button className={`drawer-tab ${drawerTab==='cats'?'active':''}`} onClick={()=>setDrawerTab('cats')}>{t('tab.cats')}</button>
            <button className={`drawer-tab ${drawerTab==='travel'?'active':''}`} onClick={()=>setDrawerTab('travel')}>{t('tab.travel')}</button>
            <button className={`drawer-tab ${drawerTab==='prefs'?'active':''}`} onClick={()=>setDrawerTab('prefs')}>{t('tab.prefs')}</button>
          </div>

          {drawerTab==='theme'&&(
            <>
              <Section title={t('sec.wallpaper')} defaultOpen={!!prefs.wallpaper}>
                {prefs.wallpaper?(
                  <>
                    <div className={`wallpaper-preview ${prefs.wallBlur>0?'blurred':''}`} style={isVideoWallpaper?{}:{backgroundImage:`url(${prefs.wallpaper})`}}>
                      {isVideoWallpaper&&<video className="wallpaper-video" src={prefs.wallpaper} autoPlay loop muted playsInline/>}
                      <div className="wallpaper-preview-scrim" style={{background:theme.bg,opacity:prefs.wallpaperDim/100}}/>
                    </div>
                    <div className="slider-row">
                      <div className="slider-header">
                        <span className="slider-label">{t('drawer.theme.backgroundDim')}</span>
                        <span className="slider-value">{prefs.wallpaperDim}%</span>
                      </div>
                      <input className="slider" type="range" min="0" max="90" step="5" value={prefs.wallpaperDim} onChange={e=>persistPrefs({...prefs,wallpaperDim:parseInt(e.target.value)})}/>
                    </div>
                    <div className="backup-grid" style={{marginTop:8}}>
                      <button className="btn btn-secondary" onClick={triggerWallpaperUpload}><I.Upload/> {t('drawer.theme.replace')}</button>
                      <button className="btn btn-ghost" onClick={clearWallpaper}><I.Trash/> {t('drawer.remove')}</button>
                    </div>
                  </>
                ):(
                  <>
                    <button className="btn btn-secondary btn-block" onClick={triggerWallpaperUpload}><I.Upload/> {t('drawer.theme.uploadWallpaper')}</button>
                    <div className="section-desc" style={{marginTop:8}}>{t('drawer.theme.wallpaperDesc')}</div>
                  </>
                )}
              </Section>

              <Section title={t('sec.weather')} defaultOpen={prefs.weather&&prefs.weather!=='none'}>
                <div className="drawer-tabs">
                  {[['none',t('drawer.theme.weatherNone')],['rain',t('drawer.theme.weatherRain')],['snow',t('drawer.theme.weatherSnow')]].map(([id,label])=>(
                    <button key={id} className={`drawer-tab ${prefs.weather===id?'active':''}`} onClick={()=>persistPrefs({...prefs,weather:id})}>{label}</button>
                  ))}
                </div>
                {prefs.weather!=='none'&&(
                  <div className="slider-row">
                    <div className="slider-header">
                      <span className="slider-label">{t('drawer.theme.effectSpeed')}</span>
                      <span className="slider-value">{Number(prefs.weatherSpeed??1).toFixed(2)}×</span>
                    </div>
                    <input className="slider" type="range" min="0.1" max="3" step="0.1" value={prefs.weatherSpeed||1} onChange={e=>persistPrefs({...prefs,weatherSpeed:parseFloat(e.target.value)})}/>
                  </div>
                )}
              </Section>

              <Section title={t('sec.glass')}>
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">{t('drawer.theme.uiBlur')}</span>
                    <span className="slider-value">{prefs.uiBlur}px</span>
                  </div>
                  <input className="slider" type="range" min="0" max="32" step="2" value={prefs.uiBlur} onChange={e=>persistPrefs({...prefs,uiBlur:parseInt(e.target.value)})}/>
                </div>
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">{t('drawer.theme.uiTransparency')}</span>
                    <span className="slider-value">{prefs.uiOpacity}%</span>
                  </div>
                  <input className="slider" type="range" min="0" max="100" step="5" value={prefs.uiOpacity} onChange={e=>persistPrefs({...prefs,uiOpacity:parseInt(e.target.value)})}/>
                </div>
                {prefs.wallpaper&&(
                  <div className="slider-row">
                    <div className="slider-header">
                      <span className="slider-label">{t('drawer.theme.backgroundBlur')}</span>
                      <span className="slider-value">{prefs.wallBlur}px</span>
                    </div>
                    <input className="slider" type="range" min="0" max="40" step="2" value={prefs.wallBlur} onChange={e=>persistPrefs({...prefs,wallBlur:parseInt(e.target.value)})}/>
                  </div>
                )}
                <ToggleRow label={t('pref.edgeBlur')} desc={t('pref.edgeBlurDesc')}
                  on={prefs.edgeBlur!==false} onToggle={()=>persistPrefs({...prefs,edgeBlur:prefs.edgeBlur===false})}/>
              </Section>

              <Section title={t('sec.presets')}>
                <div className="theme-preset-grid">
                  {Object.entries(PRESETS).map(([key,p])=>(
                    <button key={key} className={`theme-preset ${activePresetKey===key?"active":""}`} onClick={()=>applyPreset(key)}>
                      <div className="theme-preset-swatch">
                        <span style={{background:p.bg}}/><span style={{background:p.surface}}/><span style={{background:p.accent}}/>
                      </div>
                      <div className="theme-preset-name">{p.name}</div>
                    </button>
                  ))}
                </div>
              </Section>

              <Section title={t('sec.interface')}>
                {[['bg',t('drawer.theme.colorBackground')],['surface',t('drawer.theme.colorSurface')],['accent',t('drawer.theme.colorAccent')],['accentFg',t('drawer.theme.colorAccentText')],['text',t('drawer.theme.colorText')]].map(([k,l])=>(
                  <div className="color-row" key={k}>
                    <span className="color-row-label">{l}</span>
                    <span style={{display:'flex',alignItems:'center',gap:8}}>
                      <span className="color-hex">{theme[k]}</span>
                      <input className="color-input" type="color" value={theme[k]} onChange={e=>updateColor(k,e.target.value)}/>
                    </span>
                  </div>
                ))}
              </Section>

              <Section title={t('sec.status')}>
                {[['positive',t('drawer.theme.statusPositive')],['warning',t('drawer.theme.statusWarning')],['negative',t('drawer.theme.statusNegative')]].map(([k,l])=>(
                  <div className="color-row" key={k}>
                    <span className="color-row-label">{l}</span>
                    <span style={{display:'flex',alignItems:'center',gap:8}}>
                      <span className="color-hex">{theme[k]}</span>
                      <input className="color-input" type="color" value={theme[k]} onChange={e=>updateColor(k,e.target.value)}/>
                    </span>
                  </div>
                ))}
              </Section>

              <Section title={t('sec.typography')}>
                <div className="font-option-grid">
                  {allFontOptions.map(f=>(
                    <button key={f.id} className={`font-option ${prefs.font===f.id?'active':''}`} onClick={()=>persistPrefs({...prefs,font:f.id})}>
                      <div className="font-option-sample" style={{fontFamily:f.stack}}>Aa</div>
                      <div className="font-option-name">
                        {f.name}
                        {f.id.startsWith('custom:')&&(
                          <span className="font-option-remove" onClick={ev=>{ev.stopPropagation();removeCustomFont(f.id)}} title={t('drawer.theme.removeFont')}>✕</span>
                        )}
                      </div>
                    </button>
                  ))}
                </div>
                <div className="add-font-row">
                  <input className="input" placeholder={t('drawer.theme.fontPlaceholder')} value={draftFontName}
                    onChange={e=>setDraftFontName(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addCustomFont()}/>
                  <button className="btn btn-secondary" onClick={addCustomFont}><I.Plus/> {t('drawer.theme.fontAdd')}</button>
                </div>
                <div className="section-desc" style={{marginTop:4}}>{t('drawer.theme.fontDesc')}</div>
              </Section>

              <Section title={t('sec.cardPanels')}>
                <div className="section-desc">{t('drawer.theme.panelDesc')}</div>
                {prefs.cardPanel?(
                  <>
                    <div className="wallpaper-preview">
                      <div style={{position:'absolute',inset:0,backgroundImage:`url(${prefs.cardPanel})`,backgroundSize:'cover',backgroundPosition:'center'}}/>
                    </div>
                    <div className="slider-row">
                      <div className="slider-header">
                        <span className="slider-label">{t('drawer.theme.panelStrength')}</span>
                        <span className="slider-value">{prefs.cardPanelOpacity}%</span>
                      </div>
                      <input className="slider" type="range" min="10" max="100" step="5" value={prefs.cardPanelOpacity} onChange={e=>persistPrefs({...prefs,cardPanelOpacity:parseInt(e.target.value)})}/>
                    </div>
                    <div className="backup-grid" style={{marginTop:8}}>
                      <button className="btn btn-secondary" onClick={triggerCardPanelUpload}><I.Upload/> {t('drawer.theme.replace')}</button>
                      <button className="btn btn-ghost" onClick={clearCardPanel}><I.Trash/> {t('drawer.remove')}</button>
                    </div>
                  </>
                ):(
                  <div className="backup-grid">
                    <button className="btn btn-secondary btn-block" onClick={triggerCardPanelUpload}><I.Upload/> {t('drawer.theme.uploadImage')}</button>
                  </div>
                )}
              </Section>

              <Section title={t('sec.cardLayout')}>
                <div className="section-desc">{t('drawer.theme.layoutDesc')}</div>
                <button className="btn btn-ghost btn-block" onClick={resetCardOrder}>{t('drawer.theme.resetCardOrder')}</button>
              </Section>

              <Section title={t('sec.desktopCat')}>
                <ToggleRow label={t('drawer.theme.catShow')} desc={t('drawer.theme.catDesc')}
                  on={!!prefs.catEnabled} onToggle={()=>persistPrefs({...prefs,catEnabled:!prefs.catEnabled})}/>
                {prefs.catEnabled&&(
                  <div className="cat-color-grid">
                    {CAT_COLOR_PRESETS.map((c,i)=>(
                      <button
                        key={c||'classic'}
                        className={`cat-color-swatch ${c===null?'classic':''} ${prefs.catColor===c?'active':''}`}
                        style={c?{background:c}:{}}
                        title={c||t('drawer.theme.catClassic')}
                        onClick={()=>persistPrefs({...prefs,catColor:c})}
                      />
                    ))}
                    <label className="cat-color-swatch-custom" title={t('drawer.theme.catCustomColor')}>
                      <input type="color" value={prefs.catColor||'#ffffff'} onChange={e=>persistPrefs({...prefs,catColor:e.target.value})}/>
                    </label>
                  </div>
                )}
              </Section>
            </>
          )}

          {drawerTab==='chart'&&(
            <>
              <Section title={t('drawer.chart.pieTitle')} defaultOpen>
                <div className="pie-preview">
                  <svg viewBox="0 0 36 36" style={{transform:'rotate(-90deg)'}}>
                    <circle cx="18" cy="18" r="15.9155" fill="none" stroke={theme.border} strokeWidth={prefs.pieThickness}/>
                    {(()=>{
                      const preview=cats.slice(0,4).map((c,i)=>({...c,pct:[35,25,25,15][i]||10}));
                      let off=0;
                      return preview.map(s=>{
                        const gap=prefs.pieGap*0.3;
                        const dash=Math.max(0.1,s.pct-gap);
                        const el=<circle key={s.id} cx="18" cy="18" r="15.9155" fill="none" stroke={s.color} strokeWidth={prefs.pieThickness} strokeDasharray={`${dash} ${100-dash}`} strokeDashoffset={25-off}/>;
                        off+=s.pct;return el;
                      });
                    })()}
                  </svg>
                </div>
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">{t('drawer.chart.ringThickness')}</span>
                    <span className="slider-value">{Number(prefs.pieThickness??3.6).toFixed(1)}</span>
                  </div>
                  <input className="slider" type="range" min="1" max="8" step="0.2" value={prefs.pieThickness} onChange={e=>persistPrefs({...prefs,pieThickness:parseFloat(e.target.value)})}/>
                </div>
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">{t('drawer.chart.segmentGap')}</span>
                    <span className="slider-value">{Number(prefs.pieGap??0).toFixed(1)}</span>
                  </div>
                  <input className="slider" type="range" min="0" max="4" step="0.2" value={prefs.pieGap} onChange={e=>persistPrefs({...prefs,pieGap:parseFloat(e.target.value)})}/>
                </div>
              </Section>

              <Section title={t('drawer.chart.trendStyle')}>
                <div className="drawer-tabs">
                  {[['line',t('drawer.chart.line')],['heatmap',t('drawer.chart.heatmap')]].map(([id,label])=>(
                    <button key={id} className={`drawer-tab ${prefs.trendStyle===id?'active':''}`} onClick={()=>persistPrefs({...prefs,trendStyle:id})}>{label}</button>
                  ))}
                </div>
              </Section>

              {prefs.trendStyle==='heatmap'&&(
                <Section title={t('drawer.chart.heatmapColors')}>
                  <div className="theme-preset-grid">
                    {Object.entries(HEAT_PRESETS).map(([key,p])=>{
                      const active=Object.keys(HEAT_DEFAULT_COLORS).every(k=>heatColors[k]===p.colors[k]);
                      return(
                        <button key={key} className={`theme-preset ${active?'active':''}`} onClick={()=>persistPrefs({...prefs,heatColors:{...p.colors}})}>
                          <div className="theme-preset-swatch" style={{display:'flex',gap:2}}>
                            {['l0','l1','l2','l3','l4'].map(k=>(
                              <span key={k} style={{width:14,height:14,borderRadius:3,background:p.colors[k]==='transparent'?'var(--surface-2)':p.colors[k],border:'1px solid var(--border)'}}/>
                            ))}
                          </div>
                          <div className="theme-preset-name">{p.name}</div>
                        </button>
                      );
                    })}
                  </div>
                  {['l0','l1','l2','l3','l4'].map(k=>(
                    <div className="color-row" key={k}>
                      <span className="color-row-label">{k==='l0'?t('drawer.chart.emptyDays'):t('drawer.chart.level',{n:k.slice(1)})}</span>
                      <span style={{display:'flex',alignItems:'center',gap:8}}>
                        <span className="color-hex">{heatColors[k]==='transparent'?'none':heatColors[k]}</span>
                        <input className="color-input" type="color" value={heatColorVal(heatColors[k])} onChange={e=>persistPrefs({...prefs,heatColors:{...heatColors,[k]:e.target.value}})}/>
                      </span>
                    </div>
                  ))}
                  <div className="section-desc">{t('drawer.chart.heatDesc')}</div>
                </Section>
              )}

              <Section title={t('drawer.chart.categoryColors')}>
                <div className="section-desc">{t('drawer.chart.categoryColorsDesc')}</div>
                {cats.map(c=>(
                  <div className="cat-color-row" key={c.id}>
                    <span className="cat-color-label">
                      <span className="cat-color-dot" style={{background:c.color}}/>
                      {c.glyph} {c.label}
                    </span>
                    <span style={{display:'flex',alignItems:'center',gap:8}}>
                      <span className="color-hex">{c.color}</span>
                      <input className="color-input" type="color" value={c.color} onChange={e=>updateCatColor(c.id,e.target.value)}/>
                    </span>
                  </div>
                ))}
              </Section>
            </>
          )}

          {drawerTab==='cats'&&(
            <>
              <Section title={t('drawer.cats.title')} defaultOpen>
                {categories.map(c=>(
                  <div className="cat-manage-row" key={c.id}>
                    <span className="cat-manage-glyph">{c.glyph}</span>
                    <span className="cat-manage-name">{c.label}</span>
                    {categories.length>1&&(
                      <button className="cat-manage-remove" onClick={()=>removeCategory(c.id)} title={t('drawer.remove')}><I.Trash style={{width:13,height:13}}/></button>
                    )}
                  </div>
                ))}
              </Section>
              <Section title={t('drawer.cats.add')}>
                <div style={{display:'flex',gap:8}}>
                  <input className="input" type="text" placeholder={t('drawer.cats.namePlaceholder')} value={newCatName} onChange={e=>setNewCatName(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addCategory()} style={{flex:1}}/>
                  <input className="input mono" type="text" placeholder="★" value={newCatGlyph} onChange={e=>setNewCatGlyph(e.target.value)} style={{maxWidth:52,textAlign:'center'}}/>
                  <button className="btn btn-sm" onClick={addCategory}><I.Plus/></button>
                </div>
                <div className="section-desc" style={{marginTop:8}}>{t('drawer.cats.desc')}</div>
              </Section>
            </>
          )}

          {drawerTab==='travel'&&(
            <>
              <div className="section-desc">{t('travel.desc')}</div>
              <ToggleRow label={t('travel.toggle')} on={!!travel.active} onToggle={()=>setTravel({active:!travel.active})}/>
              {travel.active&&(
                <>
                  <div className="field-label" style={{marginTop:4}}>{t('travel.tripName')}</div>
                  <input className="input" type="text" placeholder={t('travel.tripNamePlaceholder')} value={travel.name||''}
                    onChange={e=>setTravel({name:e.target.value})}/>
                  <div className="field-label" style={{marginTop:10}}>{t('travel.currency')}</div>
                  <select className="input" value={travel.currency||'USD'} onChange={e=>setTravel({currency:e.target.value})}>
                    {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code} — {c.label}</option>)}
                  </select>

                  <div className="field-label" style={{marginTop:10}}>{t('travel.rate')}</div>
                  <input className="input mono" type="number" inputMode="decimal" step="0.000001" min="0"
                    value={travel.rate??1}
                    onChange={e=>setTravel({rate:parseFloat(e.target.value)||0})}/>
                  <div className="section-desc" style={{marginTop:6}}>
                    {t('travel.rateNote',{foreign:CURRENCIES[travel.currency||'USD']?.symbol||travel.currency})}
                  </div>
                  <ToggleRow label={t('travel.rateAuto')} desc={t('travel.rateAutoDesc')}
                    on={travel.rateAuto!==false} onToggle={()=>setTravel({rateAuto:travel.rateAuto===false})}/>
                  <button className="btn btn-secondary btn-block" style={{marginTop:8}}
                    onClick={()=>refreshRate&&refreshRate()} disabled={rateStatus==='loading'}>
                    <I.Zap/> {rateStatus==='loading'?t('travel.rateFetching'):t('travel.refreshRate')}
                  </button>
                  {rateStatus&&rateStatus!=='loading'&&(
                    <div className="section-desc" style={{marginTop:6,color:rateStatus==='error'?'var(--negative)':'var(--text-dim)'}}>
                      {rateStatus==='error'
                        ? t('travel.rateFailed')
                        : (rateStatus.startsWith('ok:')?t('travel.rateAsOf',{date:rateStatus.slice(3)}):t('travel.rateUpdated'))}
                    </div>
                  )}
                  <div className="section-desc" style={{marginTop:6}}>{t('travel.ratePrivacy')}</div>

                  <div className="field-label" style={{marginTop:10}}>{t('setup.startDate')}</div>
                  <input className="input mono" type="date" value={travel.start||''} onChange={e=>setTravel({start:e.target.value})}/>
                  <button className="btn btn-ghost btn-block" style={{marginTop:12}} onClick={()=>endTravel?endTravel():setTravel({active:false})}>{t('travel.end')}</button>
                </>
              )}
              {!travel.active&&(travel.currency&&travel.rate>0?null:(
                <div className="section-desc" style={{marginTop:6,color:'var(--warning)'}}>{t('travel.needsSetup')}</div>
              ))}

              <Section title={t('travel.homeCurrency')} defaultOpen>
                <select className="input" value={cur} onChange={e=>persistPrefs({...prefs,currency:e.target.value})}>
                  {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code} — {c.label}</option>)}
                </select>
                <div className="section-desc" style={{marginTop:8}}>{t('travel.homeCurrencyDesc')}</div>
              </Section>

              <Section title={t('travel.currenciesUsed')}>
                <div className="section-desc">{t('travel.currenciesUsedDesc')}</div>
                {(travelByCurrency||[]).length===0&&<div className="section-desc">{t('travel.noCurrencies')}</div>}
                {(travelByCurrency||[]).map(r=>(
                  <div className="travel-currency-row" key={r.code}>
                    <span className="mono travel-currency-code">{r.code}</span>
                    <span className="mono travel-currency-foreign">{fmt(r.foreign,r.code)}</span>
                    <span className="mono travel-currency-home">≈ {fmt(r.home,cur)}</span>
                    <span className="travel-currency-count">{r.count} {r.count===1?t('travel.entry'):t('travel.entries')}</span>
                  </div>
                ))}
              </Section>
            </>
          )}

          {drawerTab==='prefs'&&(
            <>
              <Section title={t('sec.language')}>
                <div className="field-label" style={{marginBottom:8}}>{t('pref.language')}</div>
                <select className="input" value={prefs.lang||'en'} onChange={e=>persistPrefs({...prefs,lang:e.target.value})}>
                  {LANGS.map(l=><option key={l.id} value={l.id}>{l.label}</option>)}
                </select>
                <div className="section-desc" style={{marginTop:8}}>{t('drawer.prefs.languageDesc')}</div>
              </Section>

              <Section title={t('sec.streaks')}>
                <div className="field-label" style={{marginBottom:8}}>{t('pref.streakGrace')}</div>
                <div className="drawer-tabs">
                  {[0,1,2].map(n=>(
                    <button key={n} className={`drawer-tab ${(prefs.streakGrace||0)===n?'active':''}`} onClick={()=>persistPrefs({...prefs,streakGrace:n})}>
                      {n===0?t('pref.graceOff'):n}
                    </button>
                  ))}
                </div>
                <div className="section-desc" style={{marginTop:8}}>{t('pref.streakGraceDesc')}</div>
              </Section>

              <Section title={t('sec.preferences')} defaultOpen>
                <ToggleRow label={t('pref.compact')} desc={t('pref.compactDesc')}
                  on={!!prefs.compact} onToggle={()=>persistPrefs({...prefs,compact:!prefs.compact})}/>
                <ToggleRow label={t('pref.groupHistory')} desc={t('pref.groupHistoryDesc')}
                  on={!!prefs.groupHistory} onToggle={()=>persistPrefs({...prefs,groupHistory:!prefs.groupHistory})}/>
                <ToggleRow label={t('pref.tilt')} desc={t('pref.tiltDesc')}
                  on={!!prefs.tilt} onToggle={()=>persistPrefs({...prefs,tilt:!prefs.tilt})}/>
              </Section>

              <Section title={t('sec.balance')}>
                <ToggleRow label={t('pref.balances')} desc={balancesOn?t('pref.balancesOn'):t('pref.balancesOff')}
                  on={balancesOn} onToggle={()=>persistPrefs({...prefs,balancesEnabled:!balancesOn})}/>
                {balancesOn&&(
                  <div style={{marginTop:12}}>
                    <div className="field-label" style={{marginBottom:8}}>{t('pref.heroShows')}</div>
                    <div className="drawer-tabs">
                      <button className={`drawer-tab ${heroMode==='daily'?'active':''}`} onClick={()=>persistPrefs({...prefs,heroMode:'daily'})}>{t('pref.heroDaily')}</button>
                      <button className={`drawer-tab ${heroMode==='balance'?'active':''}`} onClick={()=>persistPrefs({...prefs,heroMode:'balance'})}>{t('pref.heroBalance')}</button>
                    </div>
                  </div>
                )}
              </Section>

              <Section title={t('sec.alerts')}>
                <ToggleRow label={t('pref.budgetAlerts')} desc={t('pref.budgetAlertsDesc')}
                  on={prefs.budgetAlerts!==false} onToggle={()=>persistPrefs({...prefs,budgetAlerts:prefs.budgetAlerts===false})}/>
                <button className="btn btn-ghost btn-block" style={{marginTop:8}} onClick={requestNotifyPermission}>{t('pref.enableNotifications')}</button>
                <div className="section-desc" style={{marginTop:8}}>{t('drawer.prefs.alertsDesc')}</div>
              </Section>

              <Section title={t('sec.appLock')}>
                <ToggleRow label={t('sec.appLock')} desc={t('drawer.prefs.lockDesc')}
                  on={!!prefs.appLock} onToggle={onToggleAppLock}/>
                {prefs.appLock&&(
                  <>
                    {!lockSet&&<div className="section-desc" style={{marginTop:4,color:'var(--warning)'}}>{t('drawer.prefs.lockNoPin')}</div>}
                    <div className="backup-grid" style={{marginTop:8}}>
                      <button className="btn btn-secondary" onClick={onChangePin}>{lockSet?t('drawer.prefs.lockChangePin'):t('drawer.prefs.lockSetPin')}</button>
                      {biometricReady&&<button className="btn btn-ghost" onClick={onSetupBiometric}>{t('drawer.prefs.lockAddDevice')}</button>}
                    </div>
                  </>
                )}
              </Section>

              <Section title={t('sec.cloudSync')}>
                {FIREBASE_CONFIGURED?(
                  <>
                    {authUser?(
                      <div style={{display:'flex',alignItems:'center',gap:10,marginBottom:12}}>
                        {authUser.photo
                          ? <img src={authUser.photo} alt="" style={{width:36,height:36,borderRadius:'50%',flexShrink:0}}/>
                          : <div className="brand-mark" style={{width:36,height:36}}>{authUser.name?authUser.name[0].toUpperCase():'G'}</div>
                        }
                        <div style={{flex:1,minWidth:0}}>
                          <div style={{fontSize:13,fontWeight:600,color:'var(--text)',overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>{authUser.name||authUser.email||t('drawer.prefs.syncSignedIn')}</div>
                          <div style={{fontSize:11.5,color:syncError?'var(--negative)':'var(--text-muted)',overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>
                            {syncError?(syncErrorMsg||t('drawer.prefs.syncError')):(authUser.email||'')}
                          </div>
                          {!syncError&&authUser.uid&&(
                            <div style={{fontSize:10,color:'var(--text-muted)',marginTop:1,fontFamily:'var(--font-mono,monospace)'}}>{t('drawer.prefs.syncAccountId',{id:String(authUser.uid).slice(0,8)})}</div>
                          )}
                          {syncError&&syncErrorMsg&&(
                            <div style={{fontSize:10.5,color:'var(--negative)',marginTop:2,wordBreak:'break-word'}}>{syncErrorMsg}</div>
                          )}
                          {!syncError&&lastSyncedAt>0&&(
                            <div style={{fontSize:10.5,color:'var(--text-muted)',marginTop:2}}>{t('drawer.prefs.syncLastSynced',{time:new Date(lastSyncedAt).toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit'})})}</div>
                          )}
                        </div>
                        <button className="btn btn-ghost btn-sm" onClick={signOutGoogle}>{t('drawer.prefs.syncSignOut')}</button>
                      </div>
                    ):(
                      <button className="btn btn-secondary btn-block" onClick={signInGoogle}><I.Cloud style={{width:14,height:14}}/> {t('drawer.prefs.syncSignIn')}</button>
                    )}
                    <div className="section-desc" style={{marginTop:8}}>
                      {authUser
                        ? t('drawer.prefs.syncDescIn')
                        : t('drawer.prefs.syncDescOut')}
                    </div>
                  </>
                ):(
                  <div className="section-desc">
                    {t('drawer.prefs.syncFbBefore')}<span className="mono">FIREBASE_CONFIG</span>{t('drawer.prefs.syncFbAfter')}
                  </div>
                )}
              </Section>

              <Section title={t('sec.shortcuts')}>
                <div style={{display:'flex',flexDirection:'column',gap:10}}>
                  <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>{t('drawer.prefs.shortcutPalette')}</span><span className="kbd">⌘K</span></div>
                  <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>{t('drawer.prefs.shortcutAddSpend')}</span><span className="kbd">⌘N</span></div>
                  <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>{t('drawer.prefs.shortcutCustomize')}</span><span className="kbd">⌘,</span></div>
                  <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>{t('drawer.prefs.shortcutCancelEdit')}</span><span className="kbd">Esc</span></div>
                </div>
              </Section>
            </>
          )}
        </div>
        <div className="drawer-footer">
          <button className="btn btn-ghost btn-block" onClick={resetTheme}>{t('drawer.prefs.resetTheme')}</button>
        </div>
      </div>
    </>
  );
}
