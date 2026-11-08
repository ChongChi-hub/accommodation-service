const fs = require('fs');
const path = require('path');

const serviceDir = path.resolve(__dirname, '..');
const webDir = path.resolve(serviceDir, '../accommodation-web');

console.log('Renaming in:', serviceDir);
console.log('Renaming in:', webDir);

function getAllFiles(dirPath, arrayOfFiles = []) {
  const files = fs.readdirSync(dirPath);

  files.forEach(file => {
    const fullPath = path.join(dirPath, file);
    if (fs.statSync(fullPath).isDirectory()) {
      if (!['.git', 'build', '.gradle', 'node_modules', '.next', 'dist'].includes(file)) {
        getAllFiles(fullPath, arrayOfFiles);
      }
    } else {
      const ext = path.extname(file).toLowerCase();
      if (['.java', '.gradle', '.yml', '.yaml', '.sql', '.xml', '.md', '.properties', '.json', '.ts', '.tsx', '.mjs', '.js'].includes(ext)) {
        arrayOfFiles.push(fullPath);
      }
    }
  });

  return arrayOfFiles;
}

// 1. Move directories in accommodation-service
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

modules.forEach(mod => {
  ['main', 'test'].forEach(scope => {
    const toamPath = path.join(serviceDir, mod, 'src', scope, 'java', 'vn', 'toam');
    const cozynestPath = path.join(serviceDir, mod, 'src', scope, 'java', 'vn', 'cozynest');
    if (fs.existsSync(toamPath)) {
      console.log(`Renaming folder: ${toamPath} -> ${cozynestPath}`);
      fs.renameSync(toamPath, cozynestPath);
    }
  });
});

// 2. Text replacements
const replacements = [
  { from: /vn\.toam/g, to: 'vn.cozynest' },
  { from: /vn\/toam/g, to: 'vn/cozynest' },
  { from: /@toam\.vn/g, to: '@cozynest.vn' },
  { from: /toam\.vn/g, to: 'cozynest.vn' },
  { from: /toam_token/g, to: 'cozynest_token' },
  { from: /toam_role/g, to: 'cozynest_role' },
  { from: /toam_refresh_token/g, to: 'cozynest_refresh_token' },
  { from: /toam_user/g, to: 'cozynest_user' },
  { from: /toam_theme/g, to: 'cozynest_theme' },
  { from: /toam_access_token/g, to: 'cozynest_access_token' },
  { from: /toam_identity/g, to: 'cozynest_identity' },
  { from: /toam_listing/g, to: 'cozynest_listing' },
  { from: /toam_booking/g, to: 'cozynest_booking' },
  { from: /toam_billing/g, to: 'cozynest_billing' },
  { from: /toam_communication/g, to: 'cozynest_communication' },
  { from: /toam_admin/g, to: 'cozynest_admin' },
  { from: /toam_network/g, to: 'cozynest_network' },
  { from: /toam_mysql/g, to: 'cozynest_mysql' },
  { from: /toam_redis/g, to: 'cozynest_redis' },
  { from: /toam_mailpit/g, to: 'cozynest_mailpit' },
  { from: /toam_web/g, to: 'cozynest_web' },
  { from: /toam-identity/g, to: 'cozynest-identity' },
  { from: /toam-key/g, to: 'cozynest-key' },
  { from: /TOAM_/g, to: 'COZYNEST_' },
  { from: /ToAm/g, to: 'CozyNest' }
];

const allFiles = [...getAllFiles(serviceDir), ...getAllFiles(webDir)];

let updatedCount = 0;
allFiles.forEach(filePath => {
  // skip self script
  if (filePath.endsWith('rename-project-to-cozynest.js')) return;
  
  let content = fs.readFileSync(filePath, 'utf8');
  let changed = false;

  replacements.forEach(({ from, to }) => {
    if (from.test(content)) {
      content = content.replace(from, to);
      changed = true;
    }
  });

  if (changed) {
    fs.writeFileSync(filePath, content, 'utf8');
    updatedCount++;
  }
});

console.log(`Updated ${updatedCount} files successfully.`);

