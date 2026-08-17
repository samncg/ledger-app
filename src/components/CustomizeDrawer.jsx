import { I } from '../lib/icons';
import { CURRENCIES, PRESETS, CAT_COLOR_PRESETS, HEAT_PRESETS, HEAT_DEFAULT_COLORS } from '../lib/constants';
import { heatColorVal } from '../lib/helpers';
import { playPiggySound } from '../lib/sound';
import { FIREBASE_CONFIGURED } from '../lib/firebase';

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
  triggerPiggyTextureUpload,triggerPiggySoundUpload,showToast,
  authUser,signInGoogle,signOutGoogle,syncError,syncErrorMsg,lastSyncedAt,
  resetTheme,
}){
  return(
    <>
      <div className="drawer-overlay" onClick={onClose}/>
      <div className="drawer">
        <div className="drawer-header">
          <span className="drawer-title">Customize</span>
          <button className="icon-btn" onClick={onClose}><I.Close/></button>
        </div>
        <div className="drawer-body">
          <div className="drawer-tabs">
            <button className={`drawer-tab ${drawerTab==='theme'?'active':''}`} onClick={()=>setDrawerTab('theme')}>Theme</button>
            <button className={`drawer-tab ${drawerTab==='chart'?'active':''}`} onClick={()=>setDrawerTab('chart')}>Chart</button>
            <button className={`drawer-tab ${drawerTab==='cats'?'active':''}`} onClick={()=>setDrawerTab('cats')}>Categories</button>
            <button className={`drawer-tab ${drawerTab==='prefs'?'active':''}`} onClick={()=>setDrawerTab('prefs')}>Prefs</button>
          </div>

          {drawerTab==='theme'&&(
            <>
              <div className="section-title">Wallpaper (Local)</div>
              {prefs.wallpaper?(
                <>
                  <div className={`wallpaper-preview ${prefs.wallBlur>0?'blurred':''}`} style={isVideoWallpaper?{}:{backgroundImage:`url(${prefs.wallpaper})`}}>
                    {isVideoWallpaper&&<video className="wallpaper-video" src={prefs.wallpaper} autoPlay loop muted playsInline/>}
                    <div className="wallpaper-preview-scrim" style={{background:theme.bg,opacity:prefs.wallpaperDim/100}}/>
                  </div>
                  <div className="slider-row">
                    <div className="slider-header">
                      <span className="slider-label">Background dim</span>
                      <span className="slider-value">{prefs.wallpaperDim}%</span>
                    </div>
                    <input className="slider" type="range" min="0" max="90" step="5" value={prefs.wallpaperDim} onChange={e=>persistPrefs({...prefs,wallpaperDim:parseInt(e.target.value)})}/>
                  </div>
                  <div className="backup-grid" style={{marginTop:8}}>
                    <button className="btn btn-secondary" onClick={triggerWallpaperUpload}><I.Upload/> Replace</button>
                    <button className="btn btn-ghost" onClick={clearWallpaper}><I.Trash/> Remove</button>
                  </div>
                </>
              ):(
                <>
                  <button className="btn btn-secondary btn-block" onClick={triggerWallpaperUpload}><I.Upload/> Upload a photo or video</button>
                  <div className="section-desc" style={{marginTop:8}}>Static images or short looping video clips (MP4/WebM). Large files may not survive a reload due to browser storage limits.</div>
                </>
              )}

              <div className="section-title">Weather effects</div>
              <div className="drawer-tabs">
                {[['none','None'],['rain','Rain'],['snow','Snow']].map(([id,label])=>(
                  <button key={id} className={`drawer-tab ${prefs.weather===id?'active':''}`} onClick={()=>persistPrefs({...prefs,weather:id})}>{label}</button>
                ))}
              </div>
              {prefs.weather!=='none'&&(
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">Effect speed</span>
                    <span className="slider-value">{(prefs.weatherSpeed||1).toFixed(2)}×</span>
                  </div>
                  <input className="slider" type="range" min="0.1" max="3" step="0.1" value={prefs.weatherSpeed||1} onChange={e=>persistPrefs({...prefs,weatherSpeed:parseFloat(e.target.value)})}/>
                </div>
              )}

              <div className="section-title">Glass & transparency</div>
              <div className="slider-row">
                <div className="slider-header">
                  <span className="slider-label">UI blur</span>
                  <span className="slider-value">{prefs.uiBlur}px</span>
                </div>
                <input className="slider" type="range" min="0" max="32" step="2" value={prefs.uiBlur} onChange={e=>persistPrefs({...prefs,uiBlur:parseInt(e.target.value)})}/>
              </div>
              <div className="slider-row">
                <div className="slider-header">
                  <span className="slider-label">UI transparency</span>
                  <span className="slider-value">{prefs.uiOpacity}%</span>
                </div>
                <input className="slider" type="range" min="0" max="100" step="5" value={prefs.uiOpacity} onChange={e=>persistPrefs({...prefs,uiOpacity:parseInt(e.target.value)})}/>
              </div>
              {prefs.wallpaper&&(
                <div className="slider-row">
                  <div className="slider-header">
                    <span className="slider-label">Background blur</span>
                    <span className="slider-value">{prefs.wallBlur}px</span>
                  </div>
                  <input className="slider" type="range" min="0" max="40" step="2" value={prefs.wallBlur} onChange={e=>persistPrefs({...prefs,wallBlur:parseInt(e.target.value)})}/>
                </div>
              )}

              <div className="section-title">Presets</div>
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

              <div className="section-title">Interface</div>
              {[['bg','Background'],['surface','Surface'],['accent','Accent'],['accentFg','Accent text'],['text','Text']].map(([k,l])=>(
                <div className="color-row" key={k}>
                  <span className="color-row-label">{l}</span>
                  <span style={{display:'flex',alignItems:'center',gap:8}}>
                    <span className="color-hex">{theme[k]}</span>
                    <input className="color-input" type="color" value={theme[k]} onChange={e=>updateColor(k,e.target.value)}/>
                  </span>
                </div>
              ))}

              <div className="section-title">Status</div>
              {[['positive','Positive / Under'],['warning','Warning / Near'],['negative','Negative / Over']].map(([k,l])=>(
                <div className="color-row" key={k}>
                  <span className="color-row-label">{l}</span>
                  <span style={{display:'flex',alignItems:'center',gap:8}}>
                    <span className="color-hex">{theme[k]}</span>
                    <input className="color-input" type="color" value={theme[k]} onChange={e=>updateColor(k,e.target.value)}/>
                  </span>
                </div>
              ))}

              <div className="section-title">Typography</div>
              <div className="font-option-grid">
                {allFontOptions.map(f=>(
                  <button key={f.id} className={`font-option ${prefs.font===f.id?'active':''}`} onClick={()=>persistPrefs({...prefs,font:f.id})}>
                    <div className="font-option-sample" style={{fontFamily:f.stack}}>Aa</div>
                    <div className="font-option-name">
                      {f.name}
                      {f.id.startsWith('custom:')&&(
                        <span className="font-option-remove" onClick={ev=>{ev.stopPropagation();removeCustomFont(f.id)}} title="Remove font">✕</span>
                      )}
                    </div>
                  </button>
                ))}
              </div>
              <div className="add-font-row">
                <input className="input" placeholder="Any Google Font, e.g. Bebas Neue" value={draftFontName}
                  onChange={e=>setDraftFontName(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addCustomFont()}/>
                <button className="btn btn-secondary" onClick={addCustomFont}><I.Plus/> Add</button>
              </div>
              <div className="section-desc" style={{marginTop:-4,marginBottom:4}}>Type any font name from fonts.google.com — it loads instantly and is remembered.</div>

              <div className="section-title">Card panels</div>
              <div className="section-desc" style={{marginTop:-4}}>Skin every card with your own image — like a nameplate or sign texture wrapping the whole card.</div>
              {prefs.cardPanel?(
                <>
                  <div className="wallpaper-preview">
                    <div style={{position:'absolute',inset:0,backgroundImage:`url(${prefs.cardPanel})`,backgroundSize:'cover',backgroundPosition:'center'}}/>
                  </div>
                  <div className="slider-row">
                    <div className="slider-header">
                      <span className="slider-label">Panel strength</span>
                      <span className="slider-value">{prefs.cardPanelOpacity}%</span>
                    </div>
                    <input className="slider" type="range" min="10" max="100" step="5" value={prefs.cardPanelOpacity} onChange={e=>persistPrefs({...prefs,cardPanelOpacity:parseInt(e.target.value)})}/>
                  </div>
                  <div className="backup-grid" style={{marginTop:8}}>
                    <button className="btn btn-secondary" onClick={triggerCardPanelUpload}><I.Upload/> Replace</button>
                    <button className="btn btn-ghost" onClick={clearCardPanel}><I.Trash/> Remove</button>
                  </div>
                </>
              ):(
                <div className="backup-grid">
                  <button className="btn btn-secondary btn-block" onClick={triggerCardPanelUpload}><I.Upload/> Upload an image</button>
                </div>
              )}

              <div className="section-title">Card layout</div>
              <div className="section-desc" style={{marginTop:-4}}>Drag the grip, or use the arrow buttons, on any card to rearrange the order.</div>
              <button className="btn btn-ghost btn-block" onClick={resetCardOrder}>Reset card order</button>

              <div className="section-title">Desktop cat</div>
              <div className="toggle-row">
                <div>
                  <div className="toggle-label">Show desktop cat</div>
                  <div className="toggle-desc">A little cat that chases your cursor.</div>
                </div>
                <button className={`toggle ${prefs.catEnabled?'on':''}`} onClick={()=>persistPrefs({...prefs,catEnabled:!prefs.catEnabled})}/>
              </div>
              {prefs.catEnabled&&(
                <div className="cat-color-grid">
                  {CAT_COLOR_PRESETS.map((c,i)=>(
                    <button
                      key={c||'classic'}
                      className={`cat-color-swatch ${c===null?'classic':''} ${prefs.catColor===c?'active':''}`}
                      style={c?{background:c}:{}}
                      title={c||'Classic'}
                      onClick={()=>persistPrefs({...prefs,catColor:c})}
                    />
                  ))}
                  <label className="cat-color-swatch-custom" title="Custom color">
                    <input type="color" value={prefs.catColor||'#ffffff'} onChange={e=>persistPrefs({...prefs,catColor:e.target.value})}/>
                  </label>
                </div>
              )}
            </>
          )}

          {drawerTab==='chart'&&(
            <>
              <div className="section-title">Preview</div>
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
                  <span className="slider-label">Ring thickness</span>
                  <span className="slider-value">{prefs.pieThickness.toFixed(1)}</span>
                </div>
                <input className="slider" type="range" min="1" max="8" step="0.2" value={prefs.pieThickness} onChange={e=>persistPrefs({...prefs,pieThickness:parseFloat(e.target.value)})}/>
              </div>
              <div className="slider-row">
                <div className="slider-header">
                  <span className="slider-label">Segment gap</span>
                  <span className="slider-value">{prefs.pieGap.toFixed(1)}</span>
                </div>
                <input className="slider" type="range" min="0" max="4" step="0.2" value={prefs.pieGap} onChange={e=>persistPrefs({...prefs,pieGap:parseFloat(e.target.value)})}/>
              </div>

              <div className="section-title">Trend style</div>
              <div className="drawer-tabs">
                {[['line','Line chart'],['heatmap','Heatmap']].map(([id,label])=>(
                  <button key={id} className={`drawer-tab ${prefs.trendStyle===id?'active':''}`} onClick={()=>persistPrefs({...prefs,trendStyle:id})}>{label}</button>
                ))}
              </div>
              {prefs.trendStyle==='heatmap'&&(
                <>
                  <div className="section-title">Heatmap colors</div>
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
                      <span className="color-row-label">{k==='l0'?'Empty days':'Level '+k.slice(1)}</span>
                      <span style={{display:'flex',alignItems:'center',gap:8}}>
                        <span className="color-hex">{heatColors[k]==='transparent'?'none':heatColors[k]}</span>
                        <input className="color-input" type="color" value={heatColorVal(heatColors[k])} onChange={e=>persistPrefs({...prefs,heatColors:{...heatColors,[k]:e.target.value}})}/>
                      </span>
                    </div>
                  ))}
                  <div className="section-desc">"Empty days" is the base cell color — set to none for the default transparent look.</div>
                </>
              )}

              <div className="section-title">Category colors</div>
              <div className="section-desc">Controls pie chart, bar chart, and badges.</div>
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
            </>
          )}

          {drawerTab==='cats'&&(
            <>
              <div className="section-title">Your categories</div>
              {categories.map(c=>(
                <div className="cat-manage-row" key={c.id}>
                  <span className="cat-manage-glyph">{c.glyph}</span>
                  <span className="cat-manage-name">{c.label}</span>
                  {categories.length>1&&(
                    <button className="cat-manage-remove" onClick={()=>removeCategory(c.id)} title="Remove"><I.Trash style={{width:13,height:13}}/></button>
                  )}
                </div>
              ))}
              <div className="section-title">Add category</div>
              <div style={{display:'flex',gap:8}}>
                <input className="input" type="text" placeholder="Name (e.g. Health)" value={newCatName} onChange={e=>setNewCatName(e.target.value)} onKeyDown={e=>e.key==='Enter'&&addCategory()} style={{flex:1}}/>
                <input className="input mono" type="text" placeholder="★" value={newCatGlyph} onChange={e=>setNewCatGlyph(e.target.value)} style={{maxWidth:52,textAlign:'center'}}/>
                <button className="btn btn-sm" onClick={addCategory}><I.Plus/></button>
              </div>
              <div className="section-desc" style={{marginTop:8}}>Use short symbols (◇ ★ ♥ ● ▲ ◐) for the icon.</div>
            </>
          )}

          {drawerTab==='prefs'&&(
            <>
              <div className="section-title">Preferences</div>
              <div className="toggle-row">
                <div>
                  <div className="toggle-label">Compact density</div>
                  <div className="toggle-desc">Tighter spacing throughout the app.</div>
                </div>
                <button className={`toggle ${prefs.compact?'on':''}`} onClick={()=>persistPrefs({...prefs,compact:!prefs.compact})}/>
              </div>
              <div className="toggle-row">
                <div>
                  <div className="toggle-label">Group history by date</div>
                  <div className="toggle-desc">Show Today, Yesterday, This week, and monthly headers.</div>
                </div>
                <button className={`toggle ${prefs.groupHistory?'on':''}`} onClick={()=>persistPrefs({...prefs,groupHistory:!prefs.groupHistory})}/>
              </div>
              <div className="toggle-row">
                <div>
                  <div className="toggle-label">3D tilt panels</div>
                  <div className="toggle-desc">Cards and the hero lean toward your cursor. Mouse only — no effect on touch screens.</div>
                </div>
                <button className={`toggle ${prefs.tilt?'on':''}`} onClick={()=>persistPrefs({...prefs,tilt:!prefs.tilt})} title="Toggle 3D tilt"/>
              </div>

              <div className="section-title">Balance</div>
              <div className="toggle-row">
                <div>
                  <div className="toggle-label">Bank balance system</div>
                  <div className="toggle-desc">
                    {balancesOn
                      ? "On — keep a balance, move money to your budget, and bank leftover allowance at the end of each day."
                      : "Off — plain budgeting without a balance or transfers."}
                  </div>
                </div>
                <button className={`toggle ${balancesOn?'on':''}`} onClick={()=>persistPrefs({...prefs,balancesEnabled:!balancesOn})} title="Toggle bank balance system"/>
              </div>
              {balancesOn&&(
                <div style={{marginTop:12}}>
                  <div className="field-label" style={{marginBottom:8}}>Hero shows</div>
                  <div className="drawer-tabs">
                    <button className={`drawer-tab ${heroMode==='daily'?'active':''}`} onClick={()=>persistPrefs({...prefs,heroMode:'daily'})}>Daily allowance</button>
                    <button className={`drawer-tab ${heroMode==='balance'?'active':''}`} onClick={()=>persistPrefs({...prefs,heroMode:'balance'})}>Balance</button>
                  </div>
                </div>
              )}

              <div className="section-title">Piggy bank</div>
              {balancesOn?(
                <>
                  <div className="section-desc" style={{marginTop:-4,marginBottom:8}}>Texture</div>
                  <div className="backup-grid">
                    <button className="btn btn-secondary" onClick={triggerPiggyTextureUpload}><I.Upload/> Custom texture</button>
                    {prefs.piggyTexture&&<button className="btn btn-ghost" onClick={()=>persistPrefs({...prefs,piggyTexture:null})}><I.Trash/> Remove</button>}
                  </div>
                  <div className="section-desc" style={{marginTop:8}}>No custom texture? The flying piggy rides in by default.</div>
                  <div className="toggle-row">
                    <div>
                      <div className="toggle-label">Deposit sounds</div>
                      <div className="toggle-desc">Coin effects when you add money, plus a fanfare when the goal is complete.</div>
                    </div>
                    <button className={`toggle ${prefs.piggySound!==false?'on':''}`} onClick={()=>persistPrefs({...prefs,piggySound:prefs.piggySound===false})} title="Toggle piggy bank sounds"/>
                  </div>
                  {prefs.piggySound!==false&&(
                    <div style={{marginTop:12}}>
                      <div className="field-label" style={{marginBottom:8}}>Sound</div>
                      <div className="drawer-tabs">
                        <button className={`drawer-tab ${(prefs.piggySoundId||'coin')==='coin'?'active':''}`} onClick={()=>persistPrefs({...prefs,piggySoundId:'coin'})}>Coin</button>
                        <button className={`drawer-tab ${(prefs.piggySoundId||'coin')==='chime'?'active':''}`} onClick={()=>persistPrefs({...prefs,piggySoundId:'chime'})}>Chime</button>
                        <button className={`drawer-tab ${(prefs.piggySoundId||'coin')==='custom'?'active':''}`} onClick={()=>{if(prefs.piggySoundCustom){persistPrefs({...prefs,piggySoundId:'custom'})}else{showToast("Upload a custom sound first.","info");triggerPiggySoundUpload()}}}>Custom</button>
                      </div>
                      <div className="backup-grid" style={{marginTop:8}}>
                        <button className="btn btn-secondary" onClick={triggerPiggySoundUpload}><I.Upload/> Upload sound</button>
                        {prefs.piggySoundCustom&&<button className="btn btn-ghost" onClick={()=>persistPrefs({...prefs,piggySoundCustom:null,piggySoundId:prefs.piggySoundId==='custom'?'coin':prefs.piggySoundId})}><I.Trash/> Remove</button>}
                      </div>
                      <button className="link-btn" style={{marginTop:8}} onClick={()=>playPiggySound(prefs.piggySoundId||'coin',prefs.piggySoundCustom)}>Preview sound</button>
                    </div>
                  )}
                </>
              ):(
                <div className="section-desc">Turn the bank balance system on to use the piggy bank.</div>
              )}

              <div className="section-title">Currency</div>
              <select className="input" value={cur} onChange={e=>persistPrefs({...prefs,currency:e.target.value})}>
                {Object.values(CURRENCIES).map(c=><option key={c.code} value={c.code}>{c.symbol} {c.code} — {c.label}</option>)}
              </select>

              <div className="section-title">Cloud sync</div>
              {FIREBASE_CONFIGURED?(
                <>
                  {authUser?(
                    <div style={{display:'flex',alignItems:'center',gap:10,marginBottom:12}}>
                      {authUser.photo
                        ? <img src={authUser.photo} alt="" style={{width:36,height:36,borderRadius:'50%',flexShrink:0}}/>
                        : <div className="brand-mark" style={{width:36,height:36}}>{authUser.name?authUser.name[0].toUpperCase():'G'}</div>
                      }
                      <div style={{flex:1,minWidth:0}}>
                        <div style={{fontSize:13,fontWeight:600,color:'var(--text)',overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>{authUser.name||authUser.email||'Signed in'}</div>
                        <div style={{fontSize:11.5,color:syncError?'var(--negative)':'var(--text-muted)',overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap'}}>
                          {syncError?(syncErrorMsg||'Sync error — will retry automatically'):(authUser.email||'')}
                        </div>
                        {!syncError&&authUser.uid&&(
                          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:1,fontFamily:'var(--font-mono,monospace)'}}>Account ID {String(authUser.uid).slice(0,8)}…</div>
                        )}
                        {syncError&&syncErrorMsg&&(
                          <div style={{fontSize:10.5,color:'var(--negative)',marginTop:2,wordBreak:'break-word'}}>{syncErrorMsg}</div>
                        )}
                        {!syncError&&lastSyncedAt>0&&(
                          <div style={{fontSize:10.5,color:'var(--text-muted)',marginTop:2}}>Last synced {new Date(lastSyncedAt).toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit'})}</div>
                        )}
                      </div>
                      <button className="btn btn-ghost btn-sm" onClick={signOutGoogle}>Sign out</button>
                    </div>
                  ):(
                    <button className="btn btn-secondary btn-block" onClick={signInGoogle}><I.Cloud style={{width:14,height:14}}/> Sign in with Google</button>
                  )}
                  <div className="section-desc" style={{marginTop:8}}>
                    {authUser
                      ? "Your budget, balance, expenses, transfers, categories, theme and preferences sync automatically. The newest copy wins — edits from any device appear here."
                      : "Sign in to back up and sync your budget across devices with your Google account. Your data stays private — only you can read your copy."}
                  </div>
                </>
              ):(
                <div className="section-desc">
                  Sync is ready but needs a Firebase project. Open this file, find the <span className="mono">FIREBASE_CONFIG</span> block, and paste your Firebase web app config. Then enable Google sign-in, authorize this site's domain (Authentication → Settings → Authorized domains), and create a Firestore database (rules are documented in the file). The app must be served over http(s), not opened as a plain file.
                </div>
              )}

              <div className="section-title">Keyboard shortcuts</div>
              <div style={{display:'flex',flexDirection:'column',gap:10}}>
                <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>Command palette</span><span className="kbd">⌘K</span></div>
                <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>Add new spend</span><span className="kbd">⌘N</span></div>
                <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>Open customization</span><span className="kbd">⌘,</span></div>
                <div style={{display:'flex',justifyContent:'space-between',fontSize:13}}><span>Cancel edit</span><span className="kbd">Esc</span></div>
              </div>
            </>
          )}
        </div>
        <div className="drawer-footer">
          <button className="btn btn-ghost btn-block" onClick={resetTheme}>Reset theme to default</button>
        </div>
      </div>
    </>
  );
}
