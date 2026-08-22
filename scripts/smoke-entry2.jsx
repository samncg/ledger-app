// SSR smoke entry #2 — renders every overlay/drawer/standalone component with
// representative props so transcription typos surface as exceptions.
import { renderToString } from 'react-dom/server';
import { DEFAULT_THEME, PREF_DEFAULTS, DEFAULT_CATS, FONT_OPTIONS } from '../src/lib/constants';
import { I } from '../src/lib/icons';
import CustomizeDrawer from '../src/components/CustomizeDrawer';
import MoneyDrawer from '../src/components/MoneyDrawer';
import BudgetDrawer from '../src/components/BudgetDrawer';
import CommandPalette from '../src/components/CommandPalette';
import Confirm from '../src/components/Confirm';
import Toast from '../src/components/Toast';
import TopBar from '../src/components/TopBar';
import Hero from '../src/components/Hero';
import SetupCard from '../src/components/SetupCard';
import LogCard from '../src/components/cards/LogCard';
import BreakdownCard from '../src/components/cards/BreakdownCard';
import TrendCard from '../src/components/cards/TrendCard';
import HistoryCard from '../src/components/cards/HistoryCard';
import PiggyCard from '../src/components/cards/PiggyCard';
import AutoCard from '../src/components/cards/AutoCard';
import BackupCard from '../src/components/cards/BackupCard';

const noop = () => {};
const set = noop;
const MYR = n => `RM ${n.toFixed(2)}`;
const theme = DEFAULT_THEME;
const prefs = PREF_DEFAULTS;
const cats = DEFAULT_CATS.map(c => ({ ...c, color: theme.catColors[c.id] }));

const common = {
  today: '2026-08-17',
  MYR,
  cur: 'MYR',
  cats,
  theme,
  prefs,
  setShowDrawer: set, setDrawerTab: set, setShowCmd: set, setShowSetup: set,
  setShowTopUp: set, setMoveMode: set,
  toggleLightDark: noop, persistPrefs: noop, saveSetup: noop, submitMoney: noop,
  removeTopUp: noop, removeExpense: noop, startEdit: noop, duplicateExpense: noop,
  setShowFilters: set, setFilterCats: set, toggleFilterCat: noop, resetFilters: noop,
  triggerImport: noop, exportData: noop, exportCSV: noop, handleClearAll: noop,
  applyFrequent: noop, addExpense: noop, updateExpense: noop, cancelEdit: noop,
  toggleSelCat: noop, toggleTrendSeries: noop, setTrendHover: set,
  addCategory: noop, removeCategory: noop, addCustomFont: noop, removeCustomFont: noop,
  signInGoogle: noop, signOutGoogle: noop, showToast: noop,
  triggerWallpaperUpload: noop, clearWallpaper: noop, triggerCardPanelUpload: noop,
  clearCardPanel: noop, resetCardOrder: noop,
  triggerPiggyTextureUpload: noop, triggerPiggySoundUpload: noop,
  applyPreset: noop, updateColor: noop, updateCatColor: noop, resetTheme: noop,
  relativeDate: (d) => d,
  setDraftBudget: set, setDraftDays: set, setDraftStartDate: set, setDraftBalance: set,
  setPiggyTargetDraft: set, setPiggyTargetEdit: set, savePiggyTarget: noop, depositPiggy: noop,
  setPiggyOpen: set, breakPiggy: noop, setPiggyAmount: set,
  setAutoType: set, setAutoAmount: set, setAutoCat: set, setAutoFreq: set,
  setAutoStart: set, setAutoNote: set, addAutomation: noop, runRecurring: noop,
  removeAutomation: noop, toggleAutomation: noop, nextRun: () => 'Due today',
  startCatBudgetEdit: noop, setCatBudgetField: noop, saveCatBudgets: noop,
  catBarWidth: () => 50, catBarColor: c => c.color,
  healthBadge: () => null,
  setAmount: set, setNote: set, setEntryDate: set,
};

export default () => {
  const drawers = [
    <CustomizeDrawer key="theme" drawerTab="theme" setDrawerTab={set} onClose={noop} prefs={prefs}
      persistPrefs={noop} isVideoWallpaper={false} triggerWallpaperUpload={noop} clearWallpaper={noop}
      theme={theme} activePresetKey="mono" applyPreset={noop} updateColor={noop} updateCatColor={noop}
      allFontOptions={FONT_OPTIONS} draftFontName="" setDraftFontName={set} addCustomFont={noop} removeCustomFont={noop}
      triggerCardPanelUpload={noop} clearCardPanel={noop} resetCardOrder={noop}
      cats={cats} categories={DEFAULT_CATS} removeCategory={noop} addCategory={noop}
      newCatName="" setNewCatName={set} newCatGlyph="★" setNewCatGlyph={set}
      heatColors={prefs.heatColors} cur="MYR" balancesOn heroMode="daily"
      triggerPiggyTextureUpload={noop} triggerPiggySoundUpload={noop} showToast={noop}
      authUser={null} signInGoogle={noop} signOutGoogle={noop} syncError={false} syncErrorMsg="" lastSyncedAt={0} resetTheme={noop}/>,
    <CustomizeDrawer key="chart" drawerTab="chart" setDrawerTab={set} onClose={noop} prefs={prefs}
      persistPrefs={noop} isVideoWallpaper={false} triggerWallpaperUpload={noop} clearWallpaper={noop}
      theme={theme} activePresetKey="mono" applyPreset={noop} updateColor={noop} updateCatColor={noop}
      allFontOptions={FONT_OPTIONS} draftFontName="" setDraftFontName={set} addCustomFont={noop} removeCustomFont={noop}
      triggerCardPanelUpload={noop} clearCardPanel={noop} resetCardOrder={noop}
      cats={cats} categories={DEFAULT_CATS} removeCategory={noop} addCategory={noop}
      newCatName="" setNewCatName={set} newCatGlyph="★" setNewCatGlyph={set}
      heatColors={prefs.heatColors} cur="MYR" balancesOn heroMode="daily"
      triggerPiggyTextureUpload={noop} triggerPiggySoundUpload={noop} showToast={noop}
      authUser={null} signInGoogle={noop} signOutGoogle={noop} syncError={false} syncErrorMsg="" lastSyncedAt={0} resetTheme={noop}/>,
    <CustomizeDrawer key="cats" drawerTab="cats" setDrawerTab={set} onClose={noop} prefs={prefs}
      persistPrefs={noop} isVideoWallpaper={false} triggerWallpaperUpload={noop} clearWallpaper={noop}
      theme={theme} activePresetKey="mono" applyPreset={noop} updateColor={noop} updateCatColor={noop}
      allFontOptions={FONT_OPTIONS} draftFontName="" setDraftFontName={set} addCustomFont={noop} removeCustomFont={noop}
      triggerCardPanelUpload={noop} clearCardPanel={noop} resetCardOrder={noop}
      cats={cats} categories={DEFAULT_CATS} removeCategory={noop} addCategory={noop}
      newCatName="" setNewCatName={set} newCatGlyph="★" setNewCatGlyph={set}
      heatColors={prefs.heatColors} cur="MYR" balancesOn heroMode="daily"
      triggerPiggyTextureUpload={noop} triggerPiggySoundUpload={noop} showToast={noop}
      authUser={null} signInGoogle={noop} signOutGoogle={noop} syncError={false} syncErrorMsg="" lastSyncedAt={0} resetTheme={noop}/>,
    <CustomizeDrawer key="prefs" drawerTab="prefs" setDrawerTab={set} onClose={noop} prefs={prefs}
      persistPrefs={noop} isVideoWallpaper={false} triggerWallpaperUpload={noop} clearWallpaper={noop}
      theme={theme} activePresetKey="mono" applyPreset={noop} updateColor={noop} updateCatColor={noop}
      allFontOptions={FONT_OPTIONS} draftFontName="" setDraftFontName={set} addCustomFont={noop} removeCustomFont={noop}
      triggerCardPanelUpload={noop} clearCardPanel={noop} resetCardOrder={noop}
      cats={cats} categories={DEFAULT_CATS} removeCategory={noop} addCategory={noop}
      newCatName="" setNewCatName={set} newCatGlyph="★" setNewCatGlyph={set}
      heatColors={prefs.heatColors} cur="MYR" balancesOn heroMode="daily"
      triggerPiggyTextureUpload={noop} triggerPiggySoundUpload={noop} showToast={noop}
      authUser={null} signInGoogle={noop} signOutGoogle={noop} syncError={false} syncErrorMsg="" lastSyncedAt={0} resetTheme={noop}/>,
    <MoneyDrawer key="money" balancesOn moveMode="budget" setMoveMode={set} bankBalance={1000}
      topUpAmount="50" setTopUpAmount={set} topUpNote="" setTopUpNote={set} submitMoney={noop}
      topUps={[{ id: 't1', date: '2026-08-16', amount: 100, note: '' }]} MYR={MYR} today="2026-08-17"
      relativeDate={d => d} removeTopUp={noop} cur="MYR" onClose={noop}/>,
    <BudgetDrawer key="budget" cur="MYR" persistPrefs={noop} prefs={prefs} balancesOn
      draftBudget="600" setDraftBudget={set} draftDays="30" setDraftDays={set}
      draftStartDate="2026-08-01" setDraftStartDate={set} today="2026-08-17"
      draftBalance="1000" setDraftBalance={set} saveSetup={noop} onClose={noop}/>,
    <CommandPalette key="cmd" actions={[{ id: 'x', title: 'Add expense', icon: I.Plus, kbd: '⌘N', run: noop }]} onClose={noop}/>,
    <Confirm key="confirm" title="Delete?" msg="Really?" onConfirm={noop} onCancel={noop}/>,
    <Toast key="toast" toast={{ msg: 'hi', type: 'success', id: 1, action: null }} onDismiss={noop}/>,
    <TopBar key="topbar" scrolled={false} isDark toggleLightDark={noop} showDrawer={false}
      setShowDrawer={set} setDrawerTab={set} setShowCmd={set}/>,
    <SetupCard key="setup" balancesOn draftBudget="" setDraftBudget={set} draftCurrency="MYR"
      setDraftCurrency={set} draftDays="30" setDraftDays={set} draftStartDate="2026-08-01"
      setDraftStartDate={set} today="2026-08-17" draftBalance="" setDraftBalance={set}
      saveSetup={noop} triggerImport={noop}/>,
    <Hero key="hero" heroLabel="Available today" heroValue={12.34} MYR={MYR} healthBadge={() => null}
      streak={2} balancesOn todaySaved={5} topUpTotal={100} setMoveMode={set} setShowTopUp={set}
      todayRemaining={12.34} dailyBudget={20} effectiveMonthlyBudget={600} settings={{ periodDays: 30 }}
      runningBalance={40} avgDailySpend={15} daysOver={1} projectedTotal={450} projectedDelta={150}
      budgetPctFull={40} periodSpent={240} dayCells={[]} theme={theme} today="2026-08-17"
      relativeDate={d => d} elapsedDays={17} bankedSoFar={50}/>,
    <LogCard key="log" cats={cats} cur="MYR" editingId={null} amount="" setAmount={set} note=""
      setNote={set} entryDate="2026-08-17" setEntryDate={set} today="2026-08-17" selCats={['food']}
      toggleSelCat={noop} frequentEntries={[]} applyFrequent={noop} addExpense={noop}
      updateExpense={noop} cancelEdit={noop} addFormRef={null}/>,
    <BreakdownCard key="brk" cats={cats} catBudgets={{}} catBudgetEdit={false} catBudgetDraft={{}}
      startCatBudgetEdit={noop} setCatBudgetField={noop} saveCatBudgets={noop} overviewRange="period"
      setOverviewRange={set} ovFrom="" setOvFrom={set} ovTo="" setOvTo={set} today="2026-08-17" MYR={MYR}
      totalSpent={52.5} rangeLabel="this budget period" budgetPct={25} rangeBudget={200} rangeDays={10}
      avgPerDayInRange={5.25} topCategory={cats[0]} categoryTotals={{ food: 30, transport: 22.5, social: 0, shopping: 0, other: 0 }}
      biggestInRange={{ amount: 40 }} overviewExpenses={[]} pieSlices={[]} prefs={prefs}
      catBarWidth={() => 50} catBarColor={c => c.color}/>,
    <TrendCard key="trend" prefs={prefs} trendRange={14} setTrendRange={set} trendSeries={['__total__']}
      toggleTrendSeries={noop} cats={cats} theme={theme} trendData={[]} trendSeriesList={[]}
      trendMax={1} trendHover={null} setTrendHover={set} heatData={{ weeks: 2, total: 0, cells: [] }}
      heatColors={prefs.heatColors} heatCell={12} heatWrapRef={null} MYR={MYR} today="2026-08-17"
      relativeDate={d => d} dailyBudget={20}/>,
    <HistoryCard key="hist" expenses={[]} topUps={[]} cats={cats} filterCats={[]} toggleFilterCat={noop}
      setFilterCats={set} showFilters setShowFilters={set} historySearch="" setHistorySearch={set}
      historySort="date-desc" setHistorySort={set} dateFrom="" setDateFrom={set} dateTo="" setDateTo={set}
      activeFilterCount={0} resetFilters={noop} historyList={[]} historySpentTotal={0}
      historyToppedTotal={0} groupedHistory={[{ label: null, items: [] }]} MYR={MYR} today="2026-08-17"
      balancesOn startEdit={noop} duplicateExpense={noop} removeExpense={noop} removeTopUp={noop}/>,
    <PiggyCard key="piggy" piggies={[{ id: 'p1', name: 'Piggy bank', target: 200, saved: 50, texture: null, soundId: 'coin', soundCustom: null }]}
      activePiggyId="p1" setActivePiggyId={set} addPiggy={noop} renamePiggy={noop}
      savePiggyTarget={noop} depositPiggy={noop} breakPiggy={noop} deletePiggy={noop}
      updatePiggyTexture={noop} updatePiggySound={noop} MYR={MYR} showToast={noop}/>,
    <AutoCard key="auto" autoType="expense" setAutoType={set} autoAmount="" setAutoAmount={set}
      autoCat="food" setAutoCat={set} autoFreq="monthly" setAutoFreq={set} autoStart="2026-08-01"
      setAutoStart={set} autoNote="" setAutoNote={set} addAutomation={noop} cats={cats} balancesOn
      recurring={[]} runRecurring={noop} removeAutomation={noop} toggleAutomation={noop}
      nextRun={() => 'Due today'} MYR={MYR} today="2026-08-17"/>,
    <BackupCard key="backup" exportData={noop} exportCSV={noop} triggerImport={noop}
      settings={{ monthlyBudget: 600, periodDays: 30, startDate: '2026-08-01' }}
      setDraftBudget={set} setDraftDays={set} setDraftStartDate={set} setDraftBalance={set}
      setShowSetup={set} setMoveMode={set} setShowTopUp={set} balancesOn handleClearAll={noop}
      balance={{ start: 1000 }}/>,
  ];
  return drawers.map(el => renderToString(el)).join('');
};
