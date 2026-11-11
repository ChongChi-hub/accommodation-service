const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const mappingPath = path.resolve(__dirname, '../../accommodation-web/scripts/cloudinary-mapping.json');
if (!fs.existsSync(mappingPath)) {
  console.error('Cannot find cloudinary-mapping.json at:', mappingPath);
  process.exit(1);
}
const mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'));

console.log(`Loaded ${Object.keys(mapping).length} Cloudinary mapping entries.`);

function runSql(query) {
  try {
    return execSync('docker exec -i cozynest_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4', {
      input: query,
      encoding: 'utf8'
    });
  } catch (err) {
    console.error('SQL Error:', err.message);
    throw err;
  }
}

let sqlStatements = [];

// 1. Update room_images
for (const [localPath, cloudUrl] of Object.entries(mapping)) {
  if (localPath.startsWith('/uploads/rooms/')) {
    const filename = path.basename(localPath);
    sqlStatements.push(`UPDATE cozynest_listing.room_images SET image_url = '${cloudUrl}' WHERE image_url = '${localPath}' OR image_url LIKE '%/${filename}';`);
  }
}

// 2. Update users avatars
for (const [localPath, cloudUrl] of Object.entries(mapping)) {
  if (localPath.startsWith('/uploads/avatars/')) {
    const filename = path.basename(localPath);
    sqlStatements.push(`UPDATE cozynest_identity.users SET avatar_url = '${cloudUrl}' WHERE avatar_url = '${localPath}' OR avatar_url LIKE '%/${filename}';`);
    sqlStatements.push(`UPDATE cozynest_identity.user_profiles SET avatar_key = '${cloudUrl}' WHERE avatar_key = '${localPath}' OR avatar_key LIKE '%/${filename}';`);
    sqlStatements.push(`UPDATE cozynest_listing.landlord_snapshots SET avatar_url = '${cloudUrl}' WHERE avatar_url = '${localPath}' OR avatar_url LIKE '%/${filename}';`);
  }
}

// Fallbacks for remaining user avatars that don't have separate files
const adminAvatar = mapping['/uploads/avatars/admin1.jpg'];
const landlordAvatar = mapping['/uploads/avatars/landlord1.jpg'];
const modAvatar = mapping['/uploads/avatars/mod1.jpg'];
const defaultAvatar = mapping['/uploads/avatars/tenant1.jpg'];

if (adminAvatar) {
  sqlStatements.push(`UPDATE cozynest_identity.users SET avatar_url = '${adminAvatar}' WHERE avatar_url LIKE '%admin%';`);
  sqlStatements.push(`UPDATE cozynest_identity.user_profiles SET avatar_key = '${adminAvatar}' WHERE avatar_key LIKE '%admin%';`);
}
if (landlordAvatar) {
  sqlStatements.push(`UPDATE cozynest_identity.users SET avatar_url = '${landlordAvatar}' WHERE avatar_url LIKE '%landlord%';`);
  sqlStatements.push(`UPDATE cozynest_identity.user_profiles SET avatar_key = '${landlordAvatar}' WHERE avatar_key LIKE '%landlord%';`);
  sqlStatements.push(`UPDATE cozynest_listing.landlord_snapshots SET avatar_url = '${landlordAvatar}' WHERE avatar_url NOT LIKE 'https://res.cloudinary.com/%';`);
}
if (modAvatar) {
  sqlStatements.push(`UPDATE cozynest_identity.users SET avatar_url = '${modAvatar}' WHERE avatar_url LIKE '%mod%';`);
  sqlStatements.push(`UPDATE cozynest_identity.user_profiles SET avatar_key = '${modAvatar}' WHERE avatar_key LIKE '%mod%';`);
}
if (defaultAvatar) {
  sqlStatements.push(`UPDATE cozynest_identity.users SET avatar_url = '${defaultAvatar}' WHERE avatar_url NOT LIKE 'https://res.cloudinary.com/%';`);
  sqlStatements.push(`UPDATE cozynest_identity.user_profiles SET avatar_key = '${defaultAvatar}' WHERE avatar_key NOT LIKE 'https://res.cloudinary.com/%';`);
  sqlStatements.push(`UPDATE cozynest_listing.reviews SET tenant_avatar = '${defaultAvatar}' WHERE tenant_avatar NOT LIKE 'https://res.cloudinary.com/%';`);
}

// 3. Update banners
const banner1 = mapping['/uploads/banners/banner_1.webp'];
const banner2 = mapping['/uploads/banners/banner_2.webp'];
const banner3 = mapping['/uploads/banners/banner_3.webp'];
if (banner1) sqlStatements.push(`UPDATE cozynest_listing.banners SET image_url = '${banner1}' WHERE id = 1 OR image_url LIKE '%banner_1%' OR sort_order = 1;`);
if (banner2) sqlStatements.push(`UPDATE cozynest_listing.banners SET image_url = '${banner2}' WHERE id = 2 OR image_url LIKE '%banner_2%' OR sort_order = 2;`);
if (banner3) sqlStatements.push(`UPDATE cozynest_listing.banners SET image_url = '${banner3}' WHERE id = 3 OR image_url LIKE '%banner_3%' OR sort_order = 3;`);

// 4. Update provinces
const provHanoi = mapping['/uploads/locations/ha-noi.webp'];
const provHcm = mapping['/uploads/locations/ho-chi-minh.webp'];
const provDanang = mapping['/uploads/locations/da-nang.webp'];
const provHaiphong = mapping['/uploads/locations/hai-phong.webp'];
const provCantho = mapping['/uploads/locations/can-tho.webp'];
const provFallback = mapping['/uploads/locations/fallback.webp'];

if (provHanoi) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provHanoi}' WHERE code = 'HN' OR slug = 'ha-noi';`);
if (provHcm) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provHcm}' WHERE code = 'HCM' OR slug = 'ho-chi-minh';`);
if (provDanang) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provDanang}' WHERE code = 'DNG' OR slug = 'da-nang';`);
if (provHaiphong) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provHaiphong}' WHERE code = 'HP' OR slug = 'hai-phong';`);
if (provCantho) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provCantho}' WHERE code = 'CT' OR slug = 'can-tho';`);
if (provFallback) sqlStatements.push(`UPDATE cozynest_listing.provinces SET image_url = '${provFallback}' WHERE image_url NOT LIKE 'https://res.cloudinary.com/%';`);

console.log(`Executing ${sqlStatements.length} SQL statements...`);
const fullSql = sqlStatements.join('\n');
runSql(fullSql);

console.log('Database updated successfully with Cloudinary URLs!');

