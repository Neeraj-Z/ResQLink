const fs = require('fs');
const https = require('https');
const path = require('path');

// Direct APK release link & Expo tunnel URL
const apkUrl = 'https://github.com/Neeraj-Z/ResQLink/releases/latest/download/ResQLink.apk';
const expoUrl = 'exp://io2waue-anonymous-8081.exp.direct';

const apkQrApiUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(apkUrl)}`;
const targetPath = path.join(__dirname, 'assets', 'install_qr.png');

console.log('Downloading latest APK Install QR Code...');
const file = fs.createWriteStream(targetPath);

https.get(apkQrApiUrl, function(response) {
  response.pipe(file);
  file.on('finish', function() {
    file.close(() => {
      console.log('✅ QR Code downloaded successfully to ' + targetPath);
      console.log('📱 Scan with any Android camera to install ResQLink directly!');
    });
  });
}).on('error', function(err) {
  console.error('❌ Error generating QR code: ' + err.message);
});
