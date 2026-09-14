import { useState } from 'react';

const statusDetails = {
  PENDING: { title: 'Review pending', marker: '○' },
  RUNNING: { title: 'Review running', marker: '●' },
  COMPLETED: { title: 'Review completed', marker: '✓' },
  FAILED: { title: 'Review failed', marker: '!' },
};

export default function ReviewStatus({ reviewId, status, elapsedTime, errorMessage, isPolling, isStreamConnected }) {
  const [copyFeedback, setCopyFeedback] = useState('');
  if (!reviewId) return null;
  const detail = statusDetails[status] || { title: status, marker: '•' };

  async function copyReviewId() {
    try {
      await navigator.clipboard.writeText(reviewId);
      setCopyFeedback('Copied');
    } catch {
      setCopyFeedback('Unable to copy');
    }
    window.setTimeout(() => setCopyFeedback(''), 1800);
  }

  return (
    <section className="status-card" aria-live="polite">
      <div>
        <p className="eyebrow">Review</p>
        <h2>{detail.title}</h2>
        <div className="review-id-row">
          <span className="review-id">ID: {reviewId}</span>
          <button className="copy-button" type="button" onClick={copyReviewId}>Copy</button>
          {copyFeedback && <span className="copy-feedback" role="status">{copyFeedback}</span>}
        </div>
      </div>
      <div className="status-details">
        <span className={`status-badge status-${status?.toLowerCase()}`}><span aria-hidden="true">{detail.marker} </span>{status}</span>
        <span className="elapsed-time">Elapsed: {elapsedTime}</span>
      </div>
      {isPolling && status !== 'COMPLETED' && status !== 'FAILED' && <p className="connection-note">Live connection unavailable. Checking for updates every 5 seconds.</p>}
      {!isPolling && !isStreamConnected && status !== 'COMPLETED' && status !== 'FAILED' && <p className="connection-note">Connecting to live agent activity...</p>}
      {errorMessage && <p className="error-message">{errorMessage}</p>}
    </section>
  );
}
