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

export async function confirmBooking(bookingId) {
  const response = await api.put(`/bookings/${bookingId}/confirm`);
  return response.data;
}
