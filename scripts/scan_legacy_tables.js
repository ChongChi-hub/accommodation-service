const fs = require('fs');
const sql = fs.readFileSync('d:/RIKKEI/THUC_TAP/Rented-Accommodation-Project/server/database/schema.sql', 'utf8');
const tables = [...sql.matchAll(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([a-zA-Z0-9_]+)`?/gi)].map(m => m[1]);
console.log('Legacy tables count:', tables.length);
console.log(tables.join('\n'));
