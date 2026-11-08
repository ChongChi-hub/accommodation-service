const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

// Find spring-boot-autoconfigure-*.jar
const base = path.join(os.homedir(), '.gradle/caches/modules-2/files-2.1/org.springframework.boot/spring-boot-autoconfigure');
function findJar(dir) {
  for (const f of fs.readdirSync(dir)) {
    const full = path.join(dir, f);
    if (fs.statSync(full).isDirectory()) {
      const res = findJar(full);
      if (res) return res;
    } else if (f.endsWith('.jar') && !f.endsWith('-sources.jar')) {
      return full;
    }
  }
}

const jar = findJar(base);
console.log('Found jar:', jar);
execSync(`jar -xf "${jar}" META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`);
const lines = fs.readFileSync('META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports', 'utf8').split('\n');
console.log('Flyway entries:', lines.filter(l => l.includes('Flyway')));
fs.unlinkSync('META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports');
fs.rmdirSync('META-INF/spring');
fs.rmdirSync('META-INF');

