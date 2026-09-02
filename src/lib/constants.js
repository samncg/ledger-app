/* ═══════════════════════════════════════════
   CONSTANTS
   ═══════════════════════════════════════════ */

export const DEFAULT_CATS=[
  {id:"food",label:"Food",glyph:"◇"},
  {id:"transport",label:"Transport",glyph:"→"},
  {id:"social",label:"Social",glyph:"◎"},
  {id:"shopping",label:"Shopping",glyph:"□"},
  {id:"other",label:"Other",glyph:"·"},
];

export const CURRENCIES={
  MYR:{code:"MYR",symbol:"RM",label:"Malaysian Ringgit",locale:"en-MY"},
  USD:{code:"USD",symbol:"$",label:"US Dollar",locale:"en-US"},
  EUR:{code:"EUR",symbol:"€",label:"Euro",locale:"de-DE"},
  GBP:{code:"GBP",symbol:"£",label:"British Pound",locale:"en-GB"},
  SGD:{code:"SGD",symbol:"S$",label:"Singapore Dollar",locale:"en-SG"},
  JPY:{code:"JPY",symbol:"¥",label:"Japanese Yen",locale:"ja-JP"},
  CNY:{code:"CNY",symbol:"¥",label:"Chinese Yuan",locale:"zh-CN"},
  INR:{code:"INR",symbol:"₹",label:"Indian Rupee",locale:"en-IN"},
  AUD:{code:"AUD",symbol:"A$",label:"Australian Dollar",locale:"en-AU"},
  CAD:{code:"CAD",symbol:"C$",label:"Canadian Dollar",locale:"en-CA"},
};

export const PRESETS={
  mono:{name:'Mono',bg:'#000000',surface:'#000000',surface2:'#0a0a0a',text:'#ffffff',textDim:'#9a9a9a',textMuted:'#5c5c5c',border:'rgba(255,255,255,0.10)',borderStrong:'rgba(255,255,255,0.22)',accent:'#ffffff',accentFg:'#000000',negative:'#ff5c5c',warning:'#e8c15a',positive:'#5bd488',catColors:{food:'#5b9fd4',transport:'#e8c15a',social:'#ff6b8b',shopping:'#a68bfa',other:'#5bd488'}},
  midnight:{name:'Midnight',bg:'#0b0d10',surface:'#131720',surface2:'#1c2130',text:'#eaebf0',textDim:'#8b919e',textMuted:'#525865',border:'rgba(255,255,255,0.06)',borderStrong:'rgba(255,255,255,0.13)',accent:'#7ec6d1',accentFg:'#0b0d10',negative:'#e8644f',warning:'#d9a94f',positive:'#7ec6d1',catColors:{food:'#7ec6d1',transport:'#d9a94f',social:'#e8644f',shopping:'#9b8cc0',other:'#7a8596'}},
  graphite:{name:'Graphite',bg:'#111318',surface:'#1a1d24',surface2:'#23272f',text:'#e4e6ea',textDim:'#8b919e',textMuted:'#555b66',border:'rgba(255,255,255,0.07)',borderStrong:'rgba(255,255,255,0.14)',accent:'#8b9eff',accentFg:'#0d0f16',negative:'#f06b6b',warning:'#e3b341',positive:'#7ee787',catColors:{food:'#8b9eff',transport:'#e3b341',social:'#f06b6b',shopping:'#a371f7',other:'#768390'}},
  forest:{name:'Forest',bg:'#0a100d',surface:'#131f19',surface2:'#1b2b23',text:'#dfeae4',textDim:'#86a092',textMuted:'#4e6458',border:'rgba(255,255,255,0.06)',borderStrong:'rgba(255,255,255,0.14)',accent:'#6caf82',accentFg:'#0a100d',negative:'#d47070',warning:'#d4a55a',positive:'#6caf82',catColors:{food:'#6caf82',transport:'#d4a55a',social:'#d47070',shopping:'#7da29e',other:'#8a8f7d'}},
  paper:{name:'Paper',bg:'#fafaf7',surface:'#ffffff',surface2:'#f0f0eb',text:'#1a1a17',textDim:'#5a5a55',textMuted:'#9a9a95',border:'rgba(0,0,0,0.07)',borderStrong:'rgba(0,0,0,0.14)',accent:'#1a1a17',accentFg:'#fafaf7',negative:'#c04a30',warning:'#c07a20',positive:'#3a7a3a',catColors:{food:'#3a7a3a',transport:'#c07a20',social:'#c04a30',shopping:'#5c4a7a',other:'#6a6a65'}},
  daylight:{name:'Daylight',bg:'#f4f6f9',surface:'#ffffff',surface2:'#eef1f6',text:'#111318',textDim:'#4a5568',textMuted:'#9099a8',border:'rgba(0,0,0,0.07)',borderStrong:'rgba(0,0,0,0.14)',accent:'#2563eb',accentFg:'#ffffff',negative:'#dc2626',warning:'#d97706',positive:'#16a34a',catColors:{food:'#2563eb',transport:'#d97706',social:'#dc2626',shopping:'#7c3aed',other:'#64748b'}},
  cream:{name:'Cream',bg:'#f5eee2',surface:'#fdfaf4',surface2:'#ebe4d4',text:'#2a2118',textDim:'#6b5e4c',textMuted:'#a89878',border:'rgba(42,33,24,0.09)',borderStrong:'rgba(42,33,24,0.18)',accent:'#b87d4a',accentFg:'#fdfaf4',negative:'#b34129',warning:'#b87d4a',positive:'#4e7d4e',catColors:{food:'#b87d4a',transport:'#a07040',social:'#b34129',shopping:'#7d6b91',other:'#6b6557'}},
  sakura:{name:'Sakura',bg:'#1a1118',surface:'#261a22',surface2:'#31222e',text:'#f0dfe8',textDim:'#b0969f',textMuted:'#6e5460',border:'rgba(255,255,255,0.07)',borderStrong:'rgba(255,255,255,0.14)',accent:'#e5849f',accentFg:'#1a1118',negative:'#e5505a',warning:'#e5b76b',positive:'#a5d49f',catColors:{food:'#e5849f',transport:'#e5b76b',social:'#e5505a',shopping:'#c876b9',other:'#8a7170'}},
  arctic:{name:'Arctic',bg:'#0c1018',surface:'#141a24',surface2:'#1c2430',text:'#e0e8f0',textDim:'#7d8fa3',textMuted:'#4a5870',border:'rgba(255,255,255,0.07)',borderStrong:'rgba(255,255,255,0.14)',accent:'#5b9fd4',accentFg:'#0c1018',negative:'#d45b5b',warning:'#d4a05b',positive:'#5bd488',catColors:{food:'#5b9fd4',transport:'#d4a05b',social:'#d45b5b',shopping:'#8b7fd4',other:'#6b7d8a'}},
  ember:{name:'Ember',bg:'#120c0a',surface:'#1e1512',surface2:'#2a1e1a',text:'#f0e0d8',textDim:'#b09888',textMuted:'#6e5848',border:'rgba(255,255,255,0.07)',borderStrong:'rgba(255,255,255,0.14)',accent:'#e08050',accentFg:'#120c0a',negative:'#e04848',warning:'#e0b050',positive:'#a0c078',catColors:{food:'#e08050',transport:'#e0b050',social:'#e04848',shopping:'#c07090',other:'#887868'}},
  linen:{name:'Linen',bg:'#efeae0',surface:'#f8f4ea',surface2:'#e3ddd0',text:'#1f1a12',textDim:'#5a5040',textMuted:'#9a9080',border:'rgba(31,26,18,0.08)',borderStrong:'rgba(31,26,18,0.16)',accent:'#8a5a3a',accentFg:'#f8f4ea',negative:'#a03828',warning:'#a06818',positive:'#4a7248',catColors:{food:'#8a5a3a',transport:'#a06818',social:'#a03828',shopping:'#6a5088',other:'#6a6055'}},
};
export const DEFAULT_THEME=PRESETS.mono;

export const FONT_OPTIONS=[
  {id:'inter',name:'Inter',stack:"'Inter',system-ui,-apple-system,BlinkMacSystemFont,sans-serif"},
  {id:'system',name:'System UI',stack:"system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif"},
  {id:'poppins',name:'Poppins',stack:"'Poppins',system-ui,sans-serif"},
  {id:'grotesk',name:'Space Grotesk',stack:"'Space Grotesk',system-ui,sans-serif"},
  {id:'serif',name:'Source Serif',stack:"'Source Serif 4',Georgia,'Times New Roman',serif"},
  {id:'mono',name:'JetBrains Mono',stack:"'JetBrains Mono',ui-monospace,'Cascadia Code',monospace"},
  {id:'fredoka',name:'Fredoka',stack:"'Fredoka',system-ui,sans-serif"},
];

export const CAT_COLOR_PRESETS=[null,'#ff8a3d','#ff5c93','#ffd93d','#5bd488','#5b9fd4','#a68bfa','#f5f5f5','#2a2a2a'];

/* GitHub-style spending heatmap — base (empty days) + 4 intensity levels */
export const HEAT_DEFAULT_COLORS={l0:'transparent',l1:'#9be9a8',l2:'#40c463',l3:'#30a14e',l4:'#216e39'};
export const HEAT_PRESETS={
  github:{name:'GitHub',colors:{l0:'transparent',l1:'#9be9a8',l2:'#40c463',l3:'#30a14e',l4:'#216e39'}},
  githubDark:{name:'GitHub dark',colors:{l0:'#161b22',l1:'#0e4429',l2:'#006d32',l3:'#26a641',l4:'#39d353'}},
  ocean:{name:'Ocean',colors:{l0:'transparent',l1:'#a8c8f0',l2:'#5b9fd4',l3:'#2f6fb8',l4:'#1e3f8f'}},
  purple:{name:'Purple',colors:{l0:'transparent',l1:'#d8c8f5',l2:'#b08cff',l3:'#8a5cf5',l4:'#5a2db0'}},
  warm:{name:'Warm',colors:{l0:'transparent',l1:'#ffdfb0',l2:'#ffb066',l3:'#f57c3d',l4:'#c2402a'}},
};

/* ─── Piggy bank ─── */
export const PIGGY_GIF="https://terraria.wiki.gg/images/Flying_Piggy_Bank_%28animated%29.gif?63ff5d";
export const FREQ_OPTIONS={daily:"Daily",weekly:"Weekly",monthly:"Monthly"};

export const PREF_DEFAULTS={
  currency:"MYR",compact:false,pieThickness:3.6,pieGap:0,groupHistory:true,
  tilt:true,
  trendStyle:'line', // 'line' | 'heatmap'
  heatColors:{l0:'transparent',l1:'#9be9a8',l2:'#40c463',l3:'#30a14e',l4:'#216e39'},
  wallpaper:null,wallpaperDim:60,
  uiBlur:16,uiOpacity:96,wallBlur:0,
  font:"inter",weather:"none",weatherSpeed:1,
  catEnabled:true,catColor:null,
  customFonts:[],
  cardOrder:['log','breakdown','trend','history','backup'],
  cardPanel:null,cardPanelOpacity:100,
  balancesEnabled:true, // 'true' = banking-style balance, 'false' = plain budgeting (old behavior)
  overspendFromBalance:false, // true = overspends drain the bank balance, false = covered by monthly budget
  heroMode:'daily',     // 'daily' = hero shows today's allowance, 'balance' = hero shows bank balance
  piggyTexture:null,piggySound:true,piggySoundId:'coin',piggySoundCustom:null,
};

export const DEFAULT_CARD_ORDER=['log','breakdown','trend','history','auto','piggy','backup'];
