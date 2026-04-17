import React from 'react';
import './Notification.css';

function Notification({ id, type, message, onClose }) {
  const notificationType = type || 'info';

  return (
    <div className={`notification notification-${notificationType}`} role="status" aria-live="polite">
      <div className="notification-content">{message}</div>
      <button
        type="button"
        className="notification-close"
        onClick={function() { onClose(id); }}
        aria-label="Close notification"
      >
        ×
      </button>
    </div>
  );
}

export default Notification;