const { execSync } = require('child_process');

function sql(query) {
  try {
    return execSync('docker exec -i cozynest_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4', {
      input: query,
      encoding: 'utf8'
    });
  } catch (err) {
    return 'ERROR: ' + err.message;
  }
}

console.log('=== DESC room_rules ===');
console.log(sql('DESC cozynest_listing.room_rules;'));

console.log('=== room_rules content ===');
console.log(sql('SELECT * FROM cozynest_listing.room_rules LIMIT 5;'));

console.log('=== room_types ===');
console.log(sql('SELECT id, code, name, HEX(name) FROM cozynest_listing.room_types;'));

console.log('=== amenities ===');
console.log(sql('SELECT id, code, name, HEX(name) FROM cozynest_listing.amenities LIMIT 5;'));

console.log('=== users in identity-service ===');
console.log(sql('SELECT id, full_name, HEX(full_name) FROM cozynest_identity.users LIMIT 5;'));

console.log('=== banners in listing-service ===');
console.log(sql('SELECT id, title, HEX(title), subtitle, HEX(subtitle) FROM cozynest_listing.banners;'));

console.log('=== landlord_snapshots in listing-service ===');
console.log(sql('SELECT id, name, HEX(name) FROM cozynest_listing.landlord_snapshots;'));

console.log('=== rooms curfew in listing-service ===');
console.log(sql('SELECT id, title, curfew, HEX(curfew) FROM cozynest_listing.rooms LIMIT 5;'));
