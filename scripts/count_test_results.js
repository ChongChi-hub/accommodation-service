const fs = require('fs');
const path = require('path');

const modules = [
  'api-gateway',
  'common',
  'identity-service',
  'listing-service',
  'booking-service',
  'billing-service',
  'communication-service',
  'admin-service'
];

let grandTotalTests = 0;
let grandTotalFailures = 0;

console.log('=== TEST SUITE RESULTS BY MODULE ===');
for (const mod of modules) {
  const testResultsDir = path.join('d:/RIKKEI/THUC_TAP/PROJECT/accommodation-service', mod, 'build/test-results/test');
  if (!fs.existsSync(testResultsDir)) {
    console.log(`${mod.padEnd(25)}: No test results directory`);
    continue;
  }
  const files = fs.readdirSync(testResultsDir).filter(f => f.endsWith('.xml'));
  let tests = 0;
  let failures = 0;
  let skipped = 0;
  for (const file of files) {
    const content = fs.readFileSync(path.join(testResultsDir, file), 'utf8');
    const mTests = content.match(/tests="(\d+)"/);
    const mFailures = content.match(/failures="(\d+)"/);
    const mSkipped = content.match(/skipped="(\d+)"/);
    if (mTests) tests += parseInt(mTests[1], 10);
    if (mFailures) failures += parseInt(mFailures[1], 10);
    if (mSkipped) skipped += parseInt(mSkipped[1], 10);
  }
  grandTotalTests += tests;
  grandTotalFailures += failures;
  console.log(`${mod.padEnd(25)}: ${String(tests).padStart(3)} tests | ${String(failures).padStart(2)} failures | ${String(skipped).padStart(2)} skipped`);
}

console.log('----------------------------------------------------');
console.log(`TOTAL ACROSS ALL MODULES : ${grandTotalTests} tests | ${grandTotalFailures} failures`);
