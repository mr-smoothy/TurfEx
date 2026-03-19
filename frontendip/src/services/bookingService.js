import api from './api';

export async function createBooking(turfId, slotId, bookingDate) {
  const response = await api.post('/bookings', { turfId, slotId, bookingDate });
  return response.data;
}

export async function getMyBookings() {
  const response = await api.get('/bookings/my-bookings');
  return response.data;
}

export async function cancelBooking(bookingId) {
  const response = await api.delete(`/bookings/${bookingId}`);
  return response.data;
}

export async function initPayment(turfId, slotId, amount, bookingDate) {
  const response = await api.post('/payment/init', {
    turfId,
    slotId,
    amount,
    bookingDate,
  });
  return response.data;
}

export async function verifyPaymentSuccess(transactionId, valId) {
  const response = await api.post('/payment/verify-success', null, {
    params: {
      tran_id: transactionId,
      val_id: valId,
    },
  });
  return response.data;
}

export async function verifyPaymentFail(transactionId) {
  const response = await api.post('/payment/verify-fail', null, {
    params: {
      tran_id: transactionId,
    },
  });
  return response.data;
}

export async function verifyPaymentCancel(transactionId) {
  const response = await api.post('/payment/verify-cancel', null, {
    params: {
      tran_id: transactionId,
    },
  });
  return response.data;
}

export async function initRemainingPayment(bookingId) {
  const response = await api.post('/payment/init-remaining', null, {
    params: {
      booking_id: bookingId,
    },
  });
  return response.data;
}
