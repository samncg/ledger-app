/* Independent check: every locale must cover every EN key, with identical {placeholders}. */
const fs = require('fs');
const read = f => fs.readFileSync(f, 'utf8');

/* Values may be single- OR double-quoted (English prose with apostrophes uses " "). */
const jsPairs = txt => [...txt.matchAll(/^\s*['"]([A-Za-z0-9_.]+)['"]\s*:\s*(['"])((?:\\[\s\S]|(?!\2)[^\\])*)\2/gm)].map(m => [m[1], m[3]]);

function kotlinMap(src, marker) {
  const i = src.indexOf(marker);
  if (i < 0) return null;
  const start = src.indexOf('mapOf(', i) + 'mapOf('.length;
  let depth = 1, j = start;
  while (j < src.length && depth > 0) {
    const c = src[j];
    if (c === '(') depth++;
    else if (c === ')') depth--;
    j++;
  }
  return src.slice(start, j - 1);
}
const ktPairs = txt => [...txt.matchAll(/^\s*"([A-Za-z0-9_.]+)"\s+to\s+"((?:[^"\\]|\\.)*)"/gm)].map(m => [m[1], m[2]]);

const toks = v => (v.match(/\{\w+\}/g) || []).sort().join(',');
const D = f => f.split('/').pop();

function report(label, en, dict) {
  const missing = Object.keys(en).filter(k => !(k in dict));
  const extra = Object.keys(dict).filter(k => !(k in en));
  const badTok = Object.keys(en).filter(k => k in dict && toks(en[k]) !== toks(dict[k]));
  const same = Object.keys(en).filter(k => k in dict && en[k] === dict[k]);
  const flag = missing.length || extra.length || badTok.length;
  console.log(`${flag ? 'FAIL' : 'ok  '} ${label.padEnd(10)} keys=${String(Object.keys(dict).length).padStart(3)}/${Object.keys(en).length}` +
    (missing.length ? ` missing=${missing.length} [${missing.slice(0, 6).join(' ')}]` : '') +
    (extra.length ? ` extra=${extra.length} [${extra.slice(0, 6).join(' ')}]` : '') +
    (badTok.length ? ` placeholder-mismatch=${badTok.length} [${badTok.slice(0, 6).join(' ')}]` : '') +
    ` | identical-to-EN=${same.length ? same.length + ' [' + same.slice(0, 5).join(' ') + ']' : 0}`);
  return flag;
}

const webSrc = ['src/lib/i18n.js', 'src/lib/strings/cards.js', 'src/lib/strings/shell.js'];
const enWeb = Object.fromEntries(webSrc.flatMap(f => jsPairs(read(f))));
console.log(`WEB EN keys: ${Object.keys(enWeb).length}`);
let bad = 0;
for (const l of ['es', 'zh', 'ru', 'th', 'ja', 'ko']) {
  bad += report(`web ${l}`, enWeb, Object.fromEntries(jsPairs(read(`src/lib/locales/${l}.js`))));
}

const dir = 'android/app/src/main/java/com/ledger/app/ui/';
const atSrc = [
  ['Strings.kt', 'private val EN: Map<String, String> = mapOf('],
  ['strings/CardStrings.kt', 'val CARD_STRINGS'],
  ['strings/ShellStrings.kt', 'val SHELL_STRINGS'],
  ['strings/AppStrings.kt', 'val APP_STRINGS'],
];
const enAt = {};
for (const [f, marker] of atSrc) {
  const blk = kotlinMap(read(dir + f), marker);
  if (!blk) { console.log('FAIL could not locate map in', f); bad++; continue; }
  for (const [k, v] of ktPairs(blk)) enAt[k] = v;
}
console.log(`\nANDROID EN keys: ${Object.keys(enAt).length}`);
for (const [l, f] of [['es', 'Es'], ['zh', 'Zh'], ['ru', 'Ru'], ['th', 'Th'], ['ja', 'Ja'], ['ko', 'Ko']]) {
  const blk = kotlinMap(read(dir + `locales/${f}.kt`), `val ${f.toUpperCase()}_STRINGS`);
  if (!blk) { console.log('FAIL could not locate map in', f); bad++; continue; }
  bad += report(`at ${l}`, enAt, Object.fromEntries(ktPairs(blk)));
}
console.log(bad ? `\n${bad} locale file(s) need attention` : '\nALL LOCALES COMPLETE');
