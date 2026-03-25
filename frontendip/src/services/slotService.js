import api from './api';

export async function addSlot(turfId, slotData) {
  const response = await api.post(`/owner/turfs/${turfId}/slots`, slotData);
  return response.data;
}

export async function getSlotsByTurf(turfId) {
  const response = await api.get(`/turfs/${turfId}/slots`);
  return response.data;
}

export async function updateSlot(slotId, slotData) {
  const response = await api.put(`/timeslots/${slotId}`, slotData);
  return response.data;
}

export async function deleteSlot(slotId) {
  const response = await api.delete(`/timeslots/${slotId}`);
  return response.data;
}
