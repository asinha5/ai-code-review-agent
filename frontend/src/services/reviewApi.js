const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

function apiUrl(path) {
  return `${API_BASE_URL}${path}`;
}

async function request(path, options) {
  const response = await fetch(apiUrl(path), options);
  if (!response.ok) {
    let detail = `Request failed with status ${response.status}.`;
    try {
      const body = await response.json();
      detail = body.message || body.error || detail;
    } catch {
      // A non-JSON error response still has a useful HTTP status.
    }
    throw new Error(detail);
  }
  return response.json();
}

export function startReview(repositoryPath) {
  return request('/api/reviews', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repositoryPath }),
  });
}

export function getActivities(reviewId) {
  return request(`/api/reviews/${encodeURIComponent(reviewId)}/activities`);
}

export function getReviewResult(reviewId) {
  return request(`/api/reviews/${encodeURIComponent(reviewId)}/result`);
}

export function createActivityStream(reviewId) {
  return new EventSource(apiUrl(`/api/reviews/${encodeURIComponent(reviewId)}/stream`));
}
