const { execSync } = require('child_process');

const dbs = [
  'cozynest_identity',
  'cozynest_listing',
  'cozynest_booking',
  'cozynest_billing',
  'cozynest_communication',
  'cozynest_admin'
];

for (const db of dbs) {
  const out = execSync(`docker exec -i cozynest_mysql mysql -uroot -prootpassword123 -N -e "SHOW TABLES FROM ${db};"`).toString();
  const tables = out.trim().split('\n').map(t => t.trim()).filter(Boolean);
  console.log(`=== ${db} (${tables.length} tables) ===`);
  console.log(tables.join(', '));
}

