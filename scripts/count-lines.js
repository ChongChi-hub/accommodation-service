const fs = require('fs');
const path = require('path');
const cp = require('child_process');

function check(dir, commits) {
  const out = cp.execSync('git show --pretty="" --name-only ' + commits.join(' '), { cwd: dir }).toString();
  const files = [...new Set(out.split('\n').map(s => s.trim()).filter(Boolean))];
  const list = [];
  const over250 = [];
  for (const f of files) {
    const full = path.join(dir, f);
    if (fs.existsSync(full) && fs.statSync(full).isFile()) {
      const cnt = fs.readFileSync(full, 'utf8').split('\n').length;
      list.push({ f, cnt });
      if (cnt > 250) {
        over250.push({ f, cnt });
      }
    }
  }
  return { list, over250 };
}

console.log('--- BACKEND ---');
const b = check('d:/RIKKEI/THUC_TAP/PROJECT/accommodation-service', ['caeb2de', '658d310']);
console.log('Total files checked:', b.list.length);
console.log('Files > 250 lines:', JSON.stringify(b.over250, null, 2));

console.log('--- FRONTEND ---');
const f = check('d:/RIKKEI/THUC_TAP/PROJECT/accommodation-web', ['3c53d54']);
console.log('Total files checked:', f.list.length);
console.log('Files > 250 lines:', JSON.stringify(f.over250, null, 2));

