const fs = require('fs');
const file = 'd:/frontendip/frontendip/src/pages/TurfDetails/TurfDetails.js';
let content = fs.readFileSync(file, 'utf8');

const regex = /<div className="details-image-section">[\s\S]*?<div className="details-info-section">/;
const replacement = `<div className="details-image-section">
            <div className="details-image" style={{ width: '100%', height: '100%', minHeight: '300px', backgroundColor: '#f0f0f0', borderRadius: '12px', overflow: 'hidden', position: 'relative' }}>
              {(turf.image || turf.imageUrl) ? (
                <img
                  src={turf.image || turf.imageUrl}
                  alt={turf.name}
                  style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '12px' }}
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.style.display = 'none';
                    e.target.nextSibling.style.display = 'flex';
                  }}
                />
              ) : null}
              <div 
                style={{ width: '100%', height: '100%', display: (turf.image || turf.imageUrl) ? 'none' : 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f0f0f0', borderRadius: '12px', fontSize: '80px', position: 'absolute', top: 0, left: 0 }}
              >
                🟩
              </div>
            </div>
          </div>

          {/* Turf Info */}
          <div className="details-info-section">`;

content = content.replace(regex, replacement);
fs.writeFileSync(file, content, 'utf8');
console.log('Done!');
