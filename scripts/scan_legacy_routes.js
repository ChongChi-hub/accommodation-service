const fs = require('fs');
const path = require('path');

const routesDir = 'd:/RIKKEI/THUC_TAP/Rented-Accommodation-Project/server/routes';
const files = fs.readdirSync(routesDir).filter(f => f.endsWith('.routes.js'));

const allRoutes = [];

for (const file of files) {
  const prefix = '/' + file.replace('.routes.js', '').replace(/([A-Z])/g, '-$1').toLowerCase();
  const content = fs.readFileSync(path.join(routesDir, file), 'utf8');
  const lines = content.split('\n');
  
  for (const line of lines) {
    const match = line.match(/router\.(get|post|put|delete|patch)\(\s*['"`]([^'"`]+)['"`]/);
    if (match) {
      const method = match[1].toUpperCase();
      const subpath = match[2];
      const fullPath = '/api/v1' + (prefix === '/room' ? '/rooms' : prefix === '/user' ? '/users' : prefix) + (subpath === '/' ? '' : subpath);
      allRoutes.push({ file, method, subpath, fullPath, line: line.trim() });
    }
  }
}

console.log(`Found ${allRoutes.length} legacy routes:`);
allRoutes.forEach(r => console.log(`${r.method.padEnd(6)} ${r.fullPath.padEnd(45)} (${r.file})`));

