import React, { useEffect } from 'react';
import './ConfirmModal.css';

function ConfirmModal({ isOpen, message, onConfirm, onCancel }) {
  useEffect(function() {
    if (!isOpen) {
      return undefined;
    }

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return function() {
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  function handleOverlayClick(event) {
    if (event.target === event.currentTarget) {
      onCancel();
    }
  }

  return (
    <div className="confirm-modal-overlay" onClick={handleOverlayClick}>
      <div className="confirm-modal-box" role="dialog" aria-modal="true" aria-labelledby="confirm-cancel-title">
        <h3 id="confirm-cancel-title" className="confirm-modal-title">Confirm Cancellation</h3>
        <p className="confirm-modal-message">{message || 'Are you sure you want to cancel this booking?'}</p>

        <div className="confirm-modal-actions">
          <button type="button" className="confirm-modal-btn cancel" onClick={onCancel}>
            No
          </button>
          <button type="button" className="confirm-modal-btn confirm" onClick={onConfirm}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmModal;