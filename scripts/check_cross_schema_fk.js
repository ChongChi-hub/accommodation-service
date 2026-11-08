const { execSync } = require('child_process');

const query = `
SELECT 
  CONSTRAINT_NAME, 
  TABLE_SCHEMA, 
  TABLE_NAME, 
  COLUMN_NAME, 
  REFERENCED_TABLE_SCHEMA, 
  REFERENCED_TABLE_NAME, 
  REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_SCHEMA IS NOT NULL
  AND TABLE_SCHEMA LIKE 'cozynest_%'
  AND (REFERENCED_TABLE_SCHEMA != TABLE_SCHEMA);
`;

const res = execSync(`docker exec -i cozynest_mysql mysql -uroot -prootpassword123 -e "${query.trim().replace(/\n/g, ' ')}"`).toString();
console.log('Cross-schema foreign keys:');
console.log(res.trim() || 'NONE (Zero cross-schema foreign keys found!)');

