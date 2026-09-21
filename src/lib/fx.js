/* ─── Exchange rates ───
   Rates come from the European Central Bank's daily reference set, served by
   frankfurter.app. It needs no API key, allows browser CORS, and covers every
   currency this app offers. The request carries only the two currency codes —
   no amounts, notes or identifiers ever leave the device.

   A fetched rate is only ever used for *new* entries: every logged spend stores
   the home-currency amount it was converted at, so refreshing a rate can never
   rewrite history. */

const ENDPOINT = 'https://api.frankfurter.app/latest';

/**
 * Home-currency units per 1 unit of `from`.
 * Resolves to `{rate, date}` where `date` is the reference date the ECB used
 * (rates are not published on weekends, so it can lag "today" slightly).
 */
export async function fetchRate(from, to) {
  if (!from || !to) throw new Error('missing currency');
  if (from === to) return { rate: 1, date: '' };
  const res = await fetch(`${ENDPOINT}?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
  if (!res.ok) throw new Error(`rate unavailable (${res.status})`);
  const data = await res.json();
  const rate = data && data.rates ? data.rates[to] : undefined;
  if (typeof rate !== 'number' || !isFinite(rate) || rate <= 0) throw new Error('rate unavailable');
  return { rate, date: typeof data.date === 'string' ? data.date : '' };
}

/** Decimals needed to show a rate without collapsing it to "0.03". */
export const rateDecimals = rate => {
  const n = Math.abs(Number(rate) || 0);
  if (n === 0) return 2;
  if (n >= 100) return 2;
  if (n >= 1) return 4;
  if (n >= 0.01) return 5;
  return 6;
};
