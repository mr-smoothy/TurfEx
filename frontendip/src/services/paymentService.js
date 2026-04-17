import api from './api';

export async function createPaymentSession(bookingId) {
  const response = await api.post('/payment/create-bkash-payment', { bookingId });
  return {
    url: response.data && response.data.bkashURL ? response.data.bkashURL : null,
    paymentId: response.data && response.data.paymentID ? response.data.paymentID : null,
    bkashURL: response.data && response.data.bkashURL ? response.data.bkashURL : null,
  };
}

export async function executeBkashPayment(paymentID) {
  const response = await api.post('/payment/execute-bkash-payment', { paymentID });
  return response.data;
}
