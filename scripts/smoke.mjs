// Runs the SSR smoke bundle in jsdom, simulating the browser globals the app
// expects, then renders <App/> twice: once fresh (setup screen) and once with
// seeded localStorage (main dashboard + all cards).
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body><div id="root"></div></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
});

global.window = dom.window;
global.document = dom.window.document;
try { global.navigator = dom.window.navigator; } catch { Object.defineProperty(global, 'navigator', { value: dom.window.navigator, configurable: true }); }
global.localStorage = dom.window.localStorage;
global.CustomEvent = dom.window.CustomEvent;
global.requestAnimationFrame = dom.window.requestAnimationFrame;
global.cancelAnimationFrame = dom.window.cancelAnimationFrame;
global.self = dom.window;
global.matchMedia = dom.window.matchMedia || (() => ({ matches: false, addEventListener() {}, removeEventListener() {} }));

const { default: render } = await import('../.smoke/smoke-entry.js');

try {
  const fresh = render();
  if (!fresh.includes('Welcome to Ledger')) throw new Error('setup screen not rendered');
  console.log('OK fresh render:', fresh.length, 'chars');

  // Seed data so the dashboard + every card renders (balance mode on).
  dom.window.localStorage.setItem('ledger-settings', JSON.stringify({ monthlyBudget: 600, periodDays: 30, startDate: '2026-08-01' }));
  dom.window.localStorage.setItem('ledger-expenses', JSON.stringify([
    { id: 'a1', date: '2026-08-16', amount: 12.5, categories: ['food'], category: 'food', note: 'lunch' },
    { id: 'a2', date: '2026-08-15', amount: 40, categories: ['transport', 'shopping'], category: 'transport', note: 'grab' },
  ]));
  dom.window.localStorage.setItem('ledger-topups', JSON.stringify([{ id: 't1', date: '2026-08-14', amount: 100, note: 'bonus' }]));
  dom.window.localStorage.setItem('ledger-balance', JSON.stringify({ start: 1000 }));
  dom.window.localStorage.setItem('ledger-piggy', JSON.stringify({ target: 200, saved: 50 }));
  dom.window.localStorage.setItem('ledger-recurring', JSON.stringify([{ id: 'r1', type: 'expense', amount: 5, category: 'food', note: '', freq: 'daily', start: '2026-08-10', last: '2026-08-15', active: true }]));
  dom.window.localStorage.setItem('ledger-catbudgets', JSON.stringify({ food: 200 }));

  const seeded = render();
  for (const needle of ['Log a spend', 'Category breakdown', 'Spending trend', 'History', 'Automations', 'Piggy bank', 'Data &amp; backup', 'Available today']) {
    if (!seeded.includes(needle)) throw new Error('dashboard missing: ' + needle);
  }
  console.log('OK seeded render:', seeded.length, 'chars, all cards present');

  // Balance mode OFF (piggy hidden, top-up labels switch).
  dom.window.localStorage.setItem('ledger-prefs', JSON.stringify({ balancesEnabled: false }));
  const off = render();
  if (off.includes('Piggy bank')) throw new Error('piggy should be hidden when balances are off');
  if (!off.includes('Top up')) throw new Error('expected top-up labels');
  console.log('OK balances-off render:', off.length, 'chars');
} catch (err) {
  console.error('SMOKE FAILED:', err);
  process.exit(1);
}
