const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const dbs = [
  'cozynest_identity',
  'cozynest_listing',
  'cozynest_booking',
  'cozynest_billing',
  'cozynest_communication',
  'cozynest_admin'
];

function runSql(query) {
  try {
    return execSync('docker exec -i cozynest_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4 -N', {
      input: query,
      encoding: 'utf8'
    });
  } catch (err) {
    return '';
  }
}

async function diagnose() {
  console.log('--- Scanning text columns across all cozynest schemas ---');
  
  const results = [];
  
  for (const db of dbs) {
    const colQuery = `
      SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_SET_NAME, COLLATION_NAME
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = '${db}'
        AND DATA_TYPE IN ('varchar', 'text', 'mediumtext', 'longtext', 'char')
        AND TABLE_NAME != 'flyway_schema_history';
    `;
    const lines = runSql(colQuery).trim().split('\n').filter(Boolean);
    
    for (const line of lines) {
      const [table, col, dtype, cset, coll] = line.split('\t');
      if (!table || !col) continue;
      
      // Look for literal '?' character or replacement character
      const checkQuery = `SELECT COUNT(*) FROM \`${db}\`.\`${table}\` WHERE \`${col}\` LIKE '%?%' OR \`${col}\` LIKE '%\\uFFFD%';`;
      const countStr = runSql(checkQuery).trim();
      const count = parseInt(countStr, 10) || 0;
      
      if (count > 0) {
        const sampleQuery = `SELECT \`${col}\`, HEX(\`${col}\`) FROM \`${db}\`.\`${table}\` WHERE \`${col}\` LIKE '%?%' OR \`${col}\` LIKE '%\\uFFFD%' LIMIT 3;`;
        const sampleOut = runSql(sampleQuery).trim();
        results.push({
          db,
          table,
          col,
          count,
          cset,
          coll,
          sample: sampleOut
        });
      }
    }
  }

  console.log(`Found ${results.length} columns containing '?' or replacement chars:`);
  for (const r of results) {
    console.log(`\nSchema: ${r.db} | Table: ${r.table} | Column: ${r.col} | Charset: ${r.cset} | Collation: ${r.coll} | Rows: ${r.count}`);
    console.log('Samples & HEX:');
    console.log(r.sample);
  }

  const outPath = path.join(__dirname, 'encoding_audit_results.json');
  fs.writeFileSync(outPath, JSON.stringify(results, null, 2));
  console.log(`\nWritten results to ${outPath}`);
}

diagnose().catch(console.error);
