const fs = require('fs');
const https = require('https');

const tunnelUrl = 'exp://io2waue-anonymous-8081.exp.direct';
const qrApiUrl = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${encodeURIComponent(tunnelUrl)}`;

const destPath = 'C:\\Users\\now12\\.gemini\\antigravity-ide\\brain\\652f6d92-1cf4-4f5d-a384-fd344c2cc672\\expo_qr.png';
const file = fs.createWriteStream(destPath);

https.get(qrApiUrl, function(response) {
  response.pipe(file);
  file.on('finish', function() {
    file.close(() => {
      console.log('QR Code downloaded successfully to ' + destPath);
    });
  });
}).on('error', function(err) {
  console.error('Error downloading QR code: ' + err.message);
});
