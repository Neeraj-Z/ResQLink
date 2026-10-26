/**
 * ResQLink — Expo Go QR Launcher Helper
 * 
 * To test and run ResQLink instantly on your Android device:
 * 1. Install 'Expo Go' from Google Play Store on your phone.
 * 2. Run: npm start (or npx expo start --tunnel)
 * 3. Open Expo Go -> Scan QR code -> Scan the terminal QR code.
 */

const { spawn } = require('child_process');

console.log('====================================================');
console.log('⚡ Starting ResQLink Mobile with Expo Go Tunnel...');
console.log('📱 Scan the QR code displayed below using Expo Go on your phone.');
console.log('====================================================\n');

const expo = spawn('npx', ['expo', 'start', '--tunnel'], {
  stdio: 'inherit',
  shell: true
});

expo.on('error', (err) => {
  console.error('Failed to start Expo:', err.message);
});
