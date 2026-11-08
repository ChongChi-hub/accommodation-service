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

function runSqlBatch(sql) {
  return execSync('docker exec -i cozynest_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4 -N', {
    input: sql,
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024
  });
}

function main() {
  console.log('Querying all text columns from information_schema...');
  const colSql = `
    SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_SET_NAME, COLLATION_NAME
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA IN ('${dbs.join("','")}')
      AND DATA_TYPE IN ('varchar', 'text', 'mediumtext', 'longtext', 'char')
      AND TABLE_NAME != 'flyway_schema_history';
  `;
  const cols = runSqlBatch(colSql).trim().split('\n').filter(Boolean).map(line => {
    const [db, table, col, dtype, cset, coll] = line.split('\t');
    return { db, table, col, dtype, cset, coll };
  });

  console.log(`Found ${cols.length} text columns. Generating batch check query...`);

  // Build batch SELECTs using UNION ALL
  // We check for:
  // 1. Literal '??'
  // 2. Replacement char U+FFFD (0xEFBFBD)
  // 3. Question mark '?' followed by '?' or surrounded by non-ascii question patterns
  const batchQueries = cols.map((c, i) => {
    return `SELECT '${c.db}', '${c.table}', '${c.col}', '${c.cset}', '${c.coll}', COUNT(*) FROM \`${c.db}\`.\`${c.table}\` WHERE \`${c.col}\` LIKE '%??%' OR \`${c.col}\` LIKE '%\\uFFFD%' OR \`${c.col}\` LIKE '%?%';`;
  }).join('\n');

  console.log('Executing batch queries...');
  const out = runSqlBatch(batchQueries).trim().split('\n').filter(Boolean);

  const brokenCols = [];
  for (const line of out) {
    const parts = line.split('\t');
    if (parts.length >= 6) {
      const [db, table, col, cset, coll, countStr] = parts;
      const count = parseInt(countStr, 10);
      if (count > 0) {
        brokenCols.push({ db, table, col, cset, coll, count });
      }
    }
  }

  console.log(`\n=== COLUMNS WITH '?' OR U+FFFD (${brokenCols.length} columns) ===`);
  
  // Now fetch samples and hex for the broken columns
  for (const b of brokenCols) {
    const sampleSql = `SELECT \`${b.col}\`, HEX(\`${b.col}\`) FROM \`${b.db}\`.\`${b.table}\` WHERE \`${b.col}\` LIKE '%?%' OR \`${b.col}\` LIKE '%\\uFFFD%' LIMIT 2;`;
    const sampleOut = runSqlBatch(sampleSql).trim();
    b.sample = sampleOut;
    console.log(`| ${b.db}.${b.table} | ${b.col} | ${b.count} rows | Charset: ${b.cset} | Collation: ${b.coll} |`);
    console.log(`  Sample & Hex:\n${sampleOut.split('\n').map(l => '    ' + l).join('\n')}`);
  }

  const outDir = path.join('D:/RIKKEI/THUC_TAP/PROJECT/accommodation-service/docs/audit');
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'encoding_scan_raw.json'), JSON.stringify(brokenCols, null, 2));
}

main();

