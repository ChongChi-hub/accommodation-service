const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const profile = process.env.SPRING_PROFILES_ACTIVE || process.env.APP_PROFILE || 'dev';
if (profile === 'prod' || profile === 'production') {
  console.error('ERROR: Refusing to reset database in PRODUCTION profile!');
  process.exit(1);
}

console.log(`=== RUNNING DEV RESET DB (Profile: ${profile}) ===`);

const dbs = [
  'cozynest_identity',
  'cozynest_listing',
  'cozynest_booking',
  'cozynest_billing',
  'cozynest_communication',
  'cozynest_admin'
];

function execMysql(sql) {
  return execSync('docker exec -i cozynest_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4', {
    input: sql,
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024
  });
}

// 1. Drop & recreate schemas
console.log('1. Recreating clean schemas...');
let resetSql = '';
for (const db of dbs) {
  resetSql += `DROP DATABASE IF EXISTS \`${db}\`; CREATE DATABASE \`${db}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n`;
}
execMysql(resetSql);

function sortFlywayFiles(fileList) {
  return fileList.sort((a, b) => {
    // V1__... vs V10__...
    const matchA = a.match(/^V(\d+)/i);
    const matchB = b.match(/^V(\d+)/i);
    if (matchA && matchB) {
      return parseInt(matchA[1], 10) - parseInt(matchB[1], 10);
    }
    // R__01... vs R__02...
    const matchRA = a.match(/^R__(\d+)/i);
    const matchRB = b.match(/^R__(\d+)/i);
    if (matchRA && matchRB) {
      return parseInt(matchRA[1], 10) - parseInt(matchRB[1], 10);
    }
    return a.localeCompare(b);
  });
}

// 2. Helper to run all migrations and seeds for a service in exact Flyway order
function applyServiceSql(serviceName, dbName) {
  console.log(`\nApplying migrations & seeds for ${serviceName} -> ${dbName}...`);
  const baseDir = path.join(__dirname, '..', serviceName, 'src', 'main', 'resources', 'db');
  const migDir = path.join(baseDir, 'migration');
  const seedDir = path.join(baseDir, 'seed');

  const files = [];
  if (fs.existsSync(migDir)) {
    const migs = sortFlywayFiles(fs.readdirSync(migDir).filter(f => f.endsWith('.sql')));
    migs.forEach(f => files.push({ path: path.join(migDir, f), name: f, isMigration: true }));
  }
  if (fs.existsSync(seedDir)) {
    const seeds = sortFlywayFiles(fs.readdirSync(seedDir).filter(f => f.endsWith('.sql')));
    seeds.forEach(f => files.push({ path: path.join(seedDir, f), name: f, isMigration: false }));
  }

  for (const f of files) {
    console.log(`  Executing: ${f.name}`);
    const sqlContent = fs.readFileSync(f.path, 'utf8');
    const fullSql = `USE \`${dbName}\`;\n${sqlContent}`;
    execMysql(fullSql);
  }
}

applyServiceSql('identity-service', 'cozynest_identity');
applyServiceSql('listing-service', 'cozynest_listing');
applyServiceSql('booking-service', 'cozynest_booking');
applyServiceSql('billing-service', 'cozynest_billing');
applyServiceSql('communication-service', 'cozynest_communication');
applyServiceSql('admin-service', 'cozynest_admin');

// 3. Re-apply isolated DB users
console.log('\n3. Re-applying isolated DB users and permissions...');
const usersSql = fs.readFileSync(path.join(__dirname, 'setup_isolated_db_users.sql'), 'utf8');
execMysql(usersSql);

console.log('\n=== DEV DATABASE RESET COMPLETED CLEANLY WITH FULL UTF-8! ===');
