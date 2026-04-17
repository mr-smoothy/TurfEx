import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getMyBookings } from '../../services/bookingService';
import { executeBkashPayment } from '../../services/paymentService';
import { useNotification } from '../../context/NotificationContext';
import './PaymentResult.css';

const PaymentSuccess = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showError, showSuccess } = useNotification();
  const executeStartedRef = useRef(false);
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [confirmationError, setConfirmationError] = useState(null);
  const [confirmationSuccess, setConfirmationSuccess] = useState(false);
  const [paidAmount, setPaidAmount] = useState(null);

  useEffect(function() {
    let isMounted = true;

    async function confirmPaymentAndLoadBooking() {
      setLoading(true);
      setConfirmationError(null);
      setConfirmationSuccess(false);

      try {
        const urlParams = new URLSearchParams(window.location.search);
        const paymentId = urlParams.get('paymentID');
        const status = urlParams.get('status');
        const pendingBookingId = localStorage.getItem('pendingPaymentBookingId');
        console.log('PaymentSuccess: Received paymentID from URL:', paymentId);

        if (!paymentId) {
          console.error('PaymentSuccess: paymentID is missing from URL');
          setConfirmationError('Payment ID is missing. Please contact support.');
          showError('Payment ID is missing. Please contact support.');
          setLoading(false);
          return;
        }

        if (status && status.toLowerCase() !== 'success') {
          const query = new URLSearchParams();
          query.set('status', status);
          if (paymentId) {
            query.set('paymentID', paymentId);
          }
          if (pendingBookingId) {
            query.set('bookingId', pendingBookingId);
          }
          navigate('/payment-failed?' + query.toString(), { replace: true });
          return;
        }

        if (executeStartedRef.current) {
          console.log('PaymentSuccess: execution already started for paymentID:', paymentId);
          return;
        }

        executeStartedRef.current = true;

        // Step 1: Execute payment in backend
        console.log('PaymentSuccess: Calling backend to execute payment for paymentID:', paymentId);
        const response = await executeBkashPayment(paymentId);
        console.log('PaymentSuccess: Backend confirmation payload:', response);
        setPaidAmount(response && response.amount != null ? Number(response.amount) : null);
        console.log('PaymentSuccess: Backend confirmation successful');
        setConfirmationSuccess(true);
        showSuccess('Payment successful and confirmed.');

        // Step 2: Load booking from localStorage or fetch all bookings
        console.log('PaymentSuccess: pendingBookingId from localStorage:', pendingBookingId);

        if (!pendingBookingId) {
          console.error('PaymentSuccess: pendingPaymentBookingId not found in localStorage');
          setConfirmationError('Could not identify booking. Please check your bookings.');
          showError('Could not identify booking. Please check your bookings.');
          setLoading(false);
          return;
        }

        const bookingId = Number(pendingBookingId);
        if (!bookingId) {
          console.error('PaymentSuccess: Invalid bookingId');
          setLoading(false);
          return;
        }

        // Step 3: Fetch all bookings and find the one we just paid for
        console.log('PaymentSuccess: Fetching bookings for bookingId:', bookingId);
        const bookings = await getMyBookings();
        console.log('PaymentSuccess: Retrieved bookings:', bookings);

        const matchedBooking = bookings.find(function(item) {
          return item.id === bookingId;
        });

        if (isMounted) {
          if (matchedBooking) {
            console.log('PaymentSuccess: Found matching booking:', matchedBooking);
            setBooking(matchedBooking);
            
            // Only clear the pending booking ID if payment is confirmed
            if ((matchedBooking.status || '').toUpperCase() === 'CONFIRMED') {
              console.log('PaymentSuccess: Booking confirmed, clearing pendingPaymentBookingId');
              localStorage.removeItem('pendingPaymentBookingId');
            } else {
              console.log('PaymentSuccess: Booking status is', matchedBooking.status, '(not CONFIRMED yet)');
            }
          } else {
            console.error('PaymentSuccess: Booking not found in bookings list');
            setConfirmationError('Could not load booking details.');
            showError('Could not load booking details.');
          }
          setLoading(false);
        }
      } catch (error) {
        console.error('PaymentSuccess: Error during payment confirmation:', error);
        if (isMounted) {
          const errorMsg = error.response?.data?.message || error.message || 'An error occurred while confirming your payment.';
          setConfirmationError(errorMsg);
          showError(errorMsg);
          setLoading(false);
          executeStartedRef.current = false;
        }
      }
    }

    confirmPaymentAndLoadBooking();

    return function() {
      isMounted = false;
    };
  }, [searchParams, showError, showSuccess]);

  function formatStatus(value) {
    if (!value) {
      return 'Pending';
    }
    return value.charAt(0) + value.slice(1).toLowerCase();
  }

  const paymentId = searchParams.get('paymentID');

  return (
    <div className="payment-result-page">
      <div className="payment-result-card success">
        <h1>Payment Successful</h1>
        <p>Your payment was completed in bKash.</p>

        {paymentId && (
          <p className="payment-session-text">bKash Payment ID: {paymentId}</p>
        )}

        {confirmationError && (
          <div style={{ color: 'red', padding: '10px', border: '1px solid red', borderRadius: '4px', marginBottom: '10px' }}>
            <strong>Error:</strong> {confirmationError}
          </div>
        )}

        {confirmationSuccess && (
          <div style={{ color: 'green', padding: '10px', border: '1px solid green', borderRadius: '4px', marginBottom: '10px' }}>
            <strong>Success:</strong> Payment confirmed with backend
          </div>
        )}

        {loading ? (
          <p className="payment-status-text">Confirming payment and loading booking status...</p>
        ) : (
          <div className="payment-status-box">
            <div className="payment-status-row">
              <span>Amount Paid</span>
              <strong>{paidAmount != null ? `৳${paidAmount}` : 'N/A'}</strong>
            </div>
            {booking && (
              <>
                <div className="payment-status-row">
                  <span>Booking Status</span>
                  <strong>{formatStatus(booking.status)}</strong>
                </div>
              </>
            )}
          </div>
        )}

        <button className="payment-result-btn" onClick={function() { navigate('/my-bookings'); }}>
          Go to My Bookings
        </button>
      </div>
    </div>
  );
};

export default PaymentSuccess;
