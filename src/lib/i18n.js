import es from './locales/es';
import zh from './locales/zh';
import ru from './locales/ru';
import th from './locales/th';
import ja from './locales/ja';
import ko from './locales/ko';
import cards from './strings/cards';
import shell from './strings/shell';

/* ─── UI strings ───
   `EN` is the source of truth: every other dictionary is a partial override, and any
   missing key falls back to English. Add a key here first, then translate it.

   Interpolation uses {name} placeholders, e.g. t('hero.overBy', {amount:'RM 12'}). */

const EN = {
  /* Top bar */
  'top.quickActions':'Quick actions',
  'top.toggleTheme':'Toggle light/dark',
  'top.customize':'Customize',

  /* Log card */
  'log.title':'Log a spend',
  'log.edit':'Edit spend',
  'log.editing':'Editing',
  'log.frequent':'Frequent',
  'log.amount':'Amount',
  'log.notePlaceholder':'Note (optional)',
  'log.date':'Date',
  'log.receipt':'Receipt',
  'log.optional':'optional',
  'log.attachPhoto':'Attach photo',
  'log.remove':'Remove',
  'log.tags':'Tags',
  'log.tagPlaceholder':'Add a tag and press Enter',
  'log.removeTag':'Remove tag',
  'log.categories':'Categories',
  'log.pickOne':'pick one',
  'log.addSpend':'Add spend',
  'log.update':'Update',
  'log.cancel':'Cancel',

  /* History */
  'history.title':'History',
  'history.resetFilters':'Reset filters',
  'history.all':'All',
  'history.sortFilter':'Sort & filter',
  'history.hide':'Hide',
  'history.search':'Search notes, categories or tags…',
  'history.newest':'Newest first',
  'history.oldest':'Oldest first',
  'history.amountDown':'Amount ↓',
  'history.amountUp':'Amount ↑',
  'history.from':'From',
  'history.to':'To',
  'history.tags':'Tags',
  'history.any':'Any',
  'history.entry':'entry',
  'history.entries':'entries',
  'history.spent':'spent',
  'history.movedToBudget':'moved to budget',
  'history.toppedUp':'topped up',
  'history.categoryFilter':'category filter',
  'history.categoryFilters':'category filters',
  'history.tagFilter':'tag filter',
  'history.tagFilters':'tag filters',
  'history.noSpends':'No spends yet. Add your first one above!',
  'history.noMatch':'No entries match your filters.',
  'history.dataStays':'Data stays on your device.',
  'history.tryAdjust':'Try adjusting search or dates.',
  'history.transfersHidden':'Transfers are hidden while a category filter is active.',
  'history.moveToBudget':'Move to budget',
  'history.topUp':'Top up',
  'history.returnToBalance':'Return to balance',

  /* Hero */
  'hero.dailyAllowance':'Daily allowance',
  'hero.savedToBalance':'Saved to balance',
  'hero.leftoverBanked':'Leftover allowance banked so far',
  'hero.rollover':'Rollover',
  'hero.totalOver':'Total over',
  'hero.unspentCarries':'Unspent allowance carries over',
  'hero.spentOverAllowance':'Spent over allowance',
  'hero.avgPerDay':'Avg spending / day',
  'hero.underAllowance':'Under allowance',
  'hero.overAllowance':'Over allowance',
  'hero.projectedTotal':'Projected total',
  'hero.overIfPace':'{amount} over if pace holds',
  'hero.leftIfPace':'{amount} left if pace holds',
  'hero.budgetProgress':'Budget progress',
  'hero.left':'{amount} left',
  'hero.over':'{amount} over',
  'hero.dailySpendPeriod':'Daily spend · this period',
  'hero.dayOf':'Day {day} / {total}',
  'hero.moveMoney':'Move money',
  'hero.topUp':'Top up',
  'hero.savedToday':'{amount} saved today',
  'hero.daysUnder':'{days}d under budget',
  'hero.overTodayBy':'Over today’s allowance by {amount}',
  'hero.under':'Under',
  'hero.near':'Near',
  'hero.over2':'Over',
  'hero.today':'Today',

  /* Drawer tabs */
  'tab.theme':'Theme',
  'tab.chart':'Chart',
  'tab.cats':'Categories',
  'tab.prefs':'Prefs',

  /* Drawer sections */
  'sec.wallpaper':'Wallpaper (Local)',
  'sec.weather':'Weather effects',
  'sec.glass':'Glass & transparency',
  'sec.presets':'Presets',
  'sec.interface':'Interface',
  'sec.status':'Status',
  'sec.typography':'Typography',
  'sec.cardPanels':'Card panels',
  'sec.cardLayout':'Card layout',
  'sec.desktopCat':'Desktop cat',
  'sec.preferences':'Preferences',
  'sec.balance':'Balance',
  'sec.currency':'Currency',
  'sec.alerts':'Alerts',
  'sec.appLock':'App lock',
  'sec.cloudSync':'Cloud sync',
  'sec.shortcuts':'Keyboard shortcuts',
  'sec.streaks':'Streaks',
  'sec.language':'Language',
  'sec.travel':'Travel mode',

  /* Preferences */
  'pref.compact':'Compact density',
  'pref.compactDesc':'Tighter spacing throughout the app.',
  'pref.groupHistory':'Group history by date',
  'pref.groupHistoryDesc':'Show Today, Yesterday, This week, and monthly headers.',
  'pref.tilt':'3D tilt panels',
  'pref.tiltDesc':'Cards and the hero lean toward your cursor. Mouse only.',
  'pref.balances':'Bank balance system',
  'pref.balancesOn':'On — keep a balance, move money to your budget, and bank leftover allowance at the end of each day.',
  'pref.balancesOff':'Off — plain budgeting without a balance or transfers.',
  'pref.heroShows':'Hero shows',
  'pref.heroDaily':'Daily allowance',
  'pref.heroBalance':'Balance',
  'pref.budgetAlerts':'Budget alerts',
  'pref.budgetAlertsDesc':'Warn once at 80% and once at 100% of each category budget and the monthly budget.',
  'pref.enableNotifications':'Enable system notifications',
  'pref.streakGrace':'Streak grace days',
  'pref.streakGraceDesc':'Missed days forgiven inside a streak — a skipped day keeps the run alive.',
  'pref.graceOff':'Off',
  'pref.edgeBlur':'Edge blur & fade',
  'pref.edgeBlurDesc':'Softens the top and bottom of the screen as content scrolls under them.',
  'pref.language':'Language',

  /* Travel mode */
  'travel.title':'Travel mode',
  'travel.desc':'Log spends in a foreign currency. Each entry is converted to your home currency and tagged “travel”.',
  'travel.toggle':'Travel mode',
  'travel.tripName':'Trip name',
  'travel.tripNamePlaceholder':'e.g. Tokyo',
  'travel.currency':'Foreign currency',
  'travel.rate':'Exchange rate',
  'travel.rateHint':'1 {foreign} = {home}',
  'travel.start':'Start travel mode',
  'travel.end':'End trip',
  'travel.active':'Travelling',
  'travel.spent':'Spent abroad',
  'travel.equivalent':'≈ {amount} in {home}',
  'travel.homeEquivalent':'In home currency',
  'travel.logSpend':'Log a travel spend',
  'travel.foreignAmount':'Amount in {currency}',
  'travel.noSpends':'No travel spends yet.',
  'travel.entry':'travel entry',
  'travel.entries':'travel entries',
  'travel.needsSetup':'Set a currency and rate to start travel mode.',

  /* Setup */
  'setup.welcome':'Welcome to Ledger',
  'setup.sub':'Set your budget to get started. Everything stays on your device.',
  'setup.monthlyBudget':'Monthly budget',
  'setup.periodDays':'Period length (days)',
  'setup.startDate':'Start date',
  'setup.startingBalance':'Starting balance',
  'setup.currency':'Currency',
  'setup.getStarted':'Get started',

  /* Area fragments — kept in their own modules so each surface can be translated
     in isolation. Later spreads win, so a fragment may override a core key. */
  ...cards,
  ...shell,
};

const DICTS = { en: EN, es: es || {}, zh: zh || {}, ru: ru || {}, th: th || {}, ja: ja || {}, ko: ko || {} };

export const LANGS = [
  { id: 'en', label: 'English' },
  { id: 'es', label: 'Español' },
  { id: 'zh', label: '简体中文' },
  { id: 'ru', label: 'Русский' },
  { id: 'th', label: 'ไทย' },
  { id: 'ja', label: '日本語' },
  { id: 'ko', label: '한국어' },
];

/* The active language lives in a module variable rather than React state: `App`
   calls setLang() at the top of its render, so every child that calls t() during
   the same render pass sees the new language. */
let current = 'en';

export const setLang = lang => { current = DICTS[lang] ? lang : 'en'; };
export const getLang = () => current;

/* Translate `key`, falling back to English and finally to the key itself.
   `vars` fills {placeholders}. */
export function t(key, vars) {
  const dict = DICTS[current] || EN;
  let s = dict[key];
  if (s === undefined) s = EN[key];
  if (s === undefined) return key;
  if (vars) {
    s = s.replace(/\{(\w+)\}/g, (m, k) => (vars[k] === undefined ? m : String(vars[k])));
  }
  return s;
}

export default t;
