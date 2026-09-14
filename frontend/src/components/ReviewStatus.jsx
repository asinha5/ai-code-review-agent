const labels = {
  PENDING: 'Queued',
  RUNNING: 'Review in progress',
  COMPLETED: 'Review complete',
  FAILED: 'Review failed',
};

export default function ReviewStatus({ reviewId, status, errorMessage, isPolling }) {
  if (!reviewId) return null;

  return (
    <section className="status-card" aria-live="polite">
      <div>
        <p className="eyebrow">Review status</p>
        <h2>{labels[status] || status}</h2>
        <p className="review-id">ID: {reviewId}</p>
      </div>
      <div className={`status-badge status-${status?.toLowerCase()}`}>{status}</div>
      {isPolling && status !== 'COMPLETED' && status !== 'FAILED' && (
        <p className="connection-note">Live connection unavailable. Checking for updates every 5 seconds.</p>
      )}
      {errorMessage && <p className="error-message">{errorMessage}</p>}
    </section>
  );
}
