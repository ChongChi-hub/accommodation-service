const http = require('http');

const endpoints = [
  { name: 'Gateway', url: 'http://localhost:8080/actuator/health', port: 8080 },
  { name: 'Identity Service', url: 'http://localhost:8081/actuator/health', port: 8081 },
  { name: 'Listing Service', url: 'http://localhost:8082/actuator/health', port: 8082 },
  { name: 'Booking Service', url: 'http://localhost:8083/actuator/health', port: 8083 },
  { name: 'Billing Service', url: 'http://localhost:8084/actuator/health', port: 8084 },
  { name: 'Communication Service', url: 'http://localhost:8085/actuator/health', port: 8085 },
  { name: 'Admin Service', url: 'http://localhost:8086/actuator/health', port: 8086 }
];

async function checkUrl(url) {
  return new Promise((resolve) => {
    http.get(url, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ ok: res.statusCode === 200 && json.status === 'UP', status: json.status, code: res.statusCode });
        } catch {
          resolve({ ok: res.statusCode === 200, status: 'UNKNOWN', code: res.statusCode });
        }
      });
    }).on('error', () => {
      resolve({ ok: false, status: 'DOWN', code: 0 });
    });
  });
}

async function main() {
  console.log('=== Checking Microservices Health Status ===');
  console.log('| Service | Port | Endpoint | Status | HTTP |');
  console.log('| :--- | :--- | :--- | :--- | :--- |');

  let allHealthy = true;
  for (const ep of endpoints) {
    const res = await checkUrl(ep.url);
    const statusText = res.ok ? 'HEALTHY (UP)' : 'UNHEALTHY (' + res.status + ')';
    console.log(`| ${ep.name.padEnd(22)} | ${ep.port} | ${ep.url} | ${statusText.padEnd(16)} | ${res.code} |`);
    if (!res.ok) allHealthy = false;
  }

  if (allHealthy) {
    console.log('\nAll core microservices are UP and HEALTHY!');
    process.exit(0);
  } else {
    console.log('\nSome services are NOT ready yet.');
    process.exit(1);
  }
}

main();

