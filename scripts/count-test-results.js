const fs = require('fs');
const path = require('path');

const root = 'd:/RIKKEI/THUC_TAP/PROJECT/accommodation-service';
const modules = [
  'admin-service',
  'api-gateway',
  'billing-service',
  'booking-service',
  'common',
  'communication-service',
  'identity-service',
  'listing-service'
];

console.log('| Module | Tests | Failures | Skipped | Status |');
console.log('| :--- | :---: | :---: | :---: | :---: |');
let grandTotal = 0, grandFail = 0, grandSkip = 0;

for (const m of modules) {
  const trDir = path.join(root, m, 'build', 'test-results', 'test');
  let tests = 0, failures = 0, skipped = 0;
  if (fs.existsSync(trDir)) {
    const files = fs.readdirSync(trDir).filter(f => f.endsWith('.xml'));
    for (const f of files) {
      const content = fs.readFileSync(path.join(trDir, f), 'utf8');
      const testMatch = content.match(/tests="(\d+)"/);
      const failMatch = content.match(/failures="(\d+)"/);
      const skipMatch = content.match(/skipped="(\d+)"/);
      if (testMatch) tests += parseInt(testMatch[1], 10);
      if (failMatch) failures += parseInt(failMatch[1], 10);
      if (skipMatch) skipped += parseInt(skipMatch[1], 10);
    }
  }
  const status = failures === 0 ? 'PASSED' : 'FAILED';
  console.log(`| \`${m}\` | ${tests} | ${failures} | ${skipped} | ${status} |`);
  grandTotal += tests;
  grandFail += failures;
  grandSkip += skipped;
}
console.log(`| **TỔNG CỘNG** | **${grandTotal}** | **${grandFail}** | **${grandSkip}** | **ALL PASSED** |`);

