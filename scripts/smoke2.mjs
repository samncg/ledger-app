// Runs the second smoke bundle (drawers + standalone components) in jsdom.
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://localhost/' });

global.window = dom.window;
global.document = dom.window.document;
try { global.navigator = dom.window.navigator; } catch { Object.defineProperty(global, 'navigator', { value: dom.window.navigator, configurable: true }); }
global.localStorage = dom.window.localStorage;
global.CustomEvent = dom.window.CustomEvent;
global.self = dom.window;

const { default: render } = await import('../.smoke2/smoke-entry2.js');

try {
  const html = render();
  for (const needle of ['Customize', 'Budget settings', 'Move to budget', 'Log a spend', 'Category breakdown',
    'Spending trend', 'History', 'Piggy bank', 'Automations', 'Data &amp; backup', 'Available today',
    'Wallpaper (Local)', 'Weather effects', 'Presets', 'Typography', 'Card panels', 'Desktop cat',
    'Preview', 'Ring thickness', 'Trend style', 'Category colors', 'Your categories', 'Add category',
    'Preferences', 'Bank balance system', 'Cloud sync', 'Keyboard shortcuts', 'Search commands…']) {
    if (!html.includes(needle)) throw new Error('missing: ' + needle);
  }
  console.log('OK components render:', html.length, 'chars,', 'all drawers/cards present');
} catch (err) {
  console.error('SMOKE2 FAILED:', err);
  process.exit(1);
}
