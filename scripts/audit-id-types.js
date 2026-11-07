const fs = require('fs');
const path = require('path');

const services = ['identity-service', 'listing-service', 'booking-service', 'billing-service', 'communication-service', 'admin-service'];
const result = [];

services.forEach(svc => {
  const migDir = path.join(process.cwd(), svc, 'src/main/resources/db/migration');
  if (!fs.existsSync(migDir)) return;
  const files = fs.readdirSync(migDir).filter(f => f.endsWith('.sql'));
  files.forEach(f => {
    const content = fs.readFileSync(path.join(migDir, f), 'utf8');
    const tableRegex = /CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+`?([a-zA-Z0-9_]+)`?\s*\(([\s\S]*?)\)\s*[^;]*;/gi;
    let match;
    while ((match = tableRegex.exec(content)) !== null) {
      const tableName = match[1];
      const tableBody = match[2];
      const lines = tableBody.split('\n').map(l => l.trim()).filter(Boolean);
      let idCol = lines.find(l => /^`?id`?\s+/i.test(l));
      let pkCol = lines.find(l => /PRIMARY\s+KEY/i.test(l));
      result.push({
        service: svc,
        table: tableName,
        idCol: idCol ? idCol.split(',')[0].replace(/--.*$/, '').trim() : 'None',
        pk: pkCol ? pkCol.replace(/--.*$/, '').trim() : 'None',
        file: f
      });
    }
  });
});

console.log(JSON.stringify(result, null, 2));
