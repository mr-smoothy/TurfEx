import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { verifyPaymentFail } from '../../services/bookingService';
import '../PaymentSuccess/PaymentResult.css';

const PaymentFail = () => {
  const navigate = useNavigate();
  const params = new URLSearchParams(window.location.search);
  const transactionId = params.get('tran_id');

  useEffect(function () {
    if (!transactionId) {
      return;
    }

    verifyPaymentFail(transactionId).catch(function () {
      // Ignore UI-side fallback failures; backend callback may have already processed it.
    });
  }, [transactionId]);

  return (
    <div className="payment-result-page">
      <div className="payment-result-card failed">
        <h2>Payment Failed</h2>
        <p>Your payment did not complete. No booking was confirmed.</p>
        {transactionId && <p><strong>Transaction ID:</strong> {transactionId}</p>}

        <div className="payment-result-actions">
          <button className="btn btn-primary" onClick={function () { navigate('/turfs'); }}>
            Try Again
          </button>
          <button className="btn btn-view" onClick={function () { navigate('/my-bookings'); }}>
            View My Bookings
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentFail;
