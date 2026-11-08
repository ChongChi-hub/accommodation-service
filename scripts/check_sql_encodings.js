const fs = require('fs');
const path = require('path');

function checkFileEncoding(filePath) {
  const buf = fs.readFileSync(filePath);
  const hasBOM = buf[0] === 0xEF && buf[1] === 0xBB && buf[2] === 0xBF;
  const str = buf.toString('utf8');
  const hasLiteralQuestionMarkTriple = str.includes('???') || str.includes('??');
  return { hasBOM, hasLiteralQuestionMarkTriple, size: buf.length };
}

function scanDir(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory() && entry.name !== 'build' && entry.name !== '.gradle') {
      scanDir(full);
    } else if (entry.isFile() && entry.name.endsWith('.sql')) {
      const res = checkFileEncoding(full);
      if (res.hasBOM || res.hasLiteralQuestionMarkTriple) {
        console.log(`FILE: ${full} | BOM: ${res.hasBOM} | Has '??': ${res.hasLiteralQuestionMarkTriple}`);
      }
    }
  }
}

console.log('Scanning all SQL migration and seed files...');
scanDir('D:/RIKKEI/THUC_TAP/PROJECT/accommodation-service');
console.log('Done scanning SQL files.');

