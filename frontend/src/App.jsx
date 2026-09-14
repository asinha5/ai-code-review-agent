import { useCallback, useEffect, useRef, useState } from 'react';
import ActivityTimeline from './components/ActivityTimeline';
import RepositoryForm from './components/RepositoryForm';
import ReviewStatus from './components/ReviewStatus';
import ReviewSummary from './components/ReviewSummary';
import { createActivityStream, getActivities, getReviewResult, startReview } from './services/reviewApi';

const terminalStatuses = new Set(['COMPLETED', 'FAILED']);
const activityKey = (activity) => `${activity.timestamp}|${activity.type}|${activity.message}`;

export default function App() {
  const [repositoryPath, setRepositoryPath] = useState('');
  const [reviewId, setReviewId] = useState(null);
  const [status, setStatus] = useState(null);
  const [activities, setActivities] = useState([]);
  const [result, setResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [isStarting, setIsStarting] = useState(false);
  const [isPolling, setIsPolling] = useState(false);
  const [isStreamConnected, setIsStreamConnected] = useState(false);
  const [reviewStartedAt, setReviewStartedAt] = useState(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const streamRef = useRef(null);
  const pollingRef = useRef(null);

  const stopMonitoring = useCallback(() => {
    streamRef.current?.close();
    streamRef.current = null;
    window.clearInterval(pollingRef.current);
    pollingRef.current = null;
  }, []);

  const addActivities = useCallback((nextActivities) => {
    setActivities((current) => {
      const seen = new Set(current.map(activityKey));
      const additions = nextActivities.filter((activity) => !seen.has(activityKey(activity)));
      return additions.length ? [...current, ...additions] : current;
    });
  }, []);

  const refreshReview = useCallback(async (id) => {
    const [nextActivities, execution] = await Promise.all([getActivities(id), getReviewResult(id)]);
    addActivities(nextActivities);
    setStatus(execution.status);
    if (execution.status === 'COMPLETED') setResult(execution.result);
    if (execution.status === 'FAILED') setErrorMessage(execution.errorMessage || 'The review could not be completed.');
    return execution.status;
  }, [addActivities]);

  const beginPolling = useCallback((id) => {
    if (pollingRef.current) return;
    setIsPolling(true);
    const poll = async () => {
      try {
        const nextStatus = await refreshReview(id);
        if (terminalStatuses.has(nextStatus)) stopMonitoring();
      } catch (error) {
        setErrorMessage(error.message);
      }
    };
    poll();
    pollingRef.current = window.setInterval(poll, 5000);
  }, [refreshReview, stopMonitoring]);

  const monitorReview = useCallback((id) => {
    const stream = createActivityStream(id);
    streamRef.current = stream;
    stream.onopen = () => setIsStreamConnected(true);
    stream.addEventListener('review-activity', (event) => {
      try {
        const activity = JSON.parse(event.data);
        addActivities([activity]);
        if (activity.type === 'REVIEW_COMPLETED' || activity.type === 'REVIEW_FAILED') {
          refreshReview(id).catch((error) => setErrorMessage(error.message));
        }
      } catch {
        setErrorMessage('Received an unreadable activity update.');
      }
    });
    stream.onerror = () => {
      setIsStreamConnected(false);
      stream.close();
      beginPolling(id);
    };
  }, [addActivities, beginPolling, refreshReview]);

  useEffect(() => stopMonitoring, [stopMonitoring]);

  useEffect(() => {
    if (!reviewId || terminalStatuses.has(status) || !reviewStartedAt) return undefined;
    const updateElapsedTime = () => setElapsedSeconds(Math.floor((Date.now() - reviewStartedAt) / 1000));
    updateElapsedTime();
    const intervalId = window.setInterval(updateElapsedTime, 1000);
    return () => window.clearInterval(intervalId);
  }, [reviewId, reviewStartedAt, status]);

  async function handleSubmit(event) {
    event.preventDefault();
    stopMonitoring();
    setIsStarting(true);
    setActivities([]);
    setResult(null);
    setErrorMessage('');
    setIsPolling(false);
    setIsStreamConnected(false);
    setElapsedSeconds(0);
    try {
      const started = await startReview(repositoryPath.trim());
      if (!started.reviewId) throw new Error('The server accepted the review but did not return a review ID.');
      setReviewId(started.reviewId);
      setStatus(started.status || 'PENDING');
      setReviewStartedAt(Date.now());
      monitorReview(started.reviewId);
    } catch (error) {
      setReviewId(null);
      setStatus(null);
      setErrorMessage(error.message);
    } finally {
      setIsStarting(false);
    }
  }

  function handleNewReview() {
    stopMonitoring();
    setReviewId(null);
    setStatus(null);
    setActivities([]);
    setResult(null);
    setErrorMessage('');
    setIsPolling(false);
    setIsStreamConnected(false);
    setReviewStartedAt(null);
    setElapsedSeconds(0);
  }

  const activeReview = reviewId && !terminalStatuses.has(status);
  const elapsedTime = `${String(Math.floor(elapsedSeconds / 60)).padStart(2, '0')}:${String(elapsedSeconds % 60).padStart(2, '0')}`;
  return (
    <main className="app-shell">
      <header className="app-header">
        <div><p className="eyebrow">Agentic repository review</p><h1>AI Code Review Agent</h1><p>Agentic repository review powered by Spring AI</p></div>
        <span className="header-label">Agentic Review</span>
      </header>
      <section className="panel repository-panel">
        <RepositoryForm repositoryPath={repositoryPath} onPathChange={setRepositoryPath} onSubmit={handleSubmit} isStarting={isStarting} isReviewActive={activeReview} />
        {!reviewId && errorMessage && <p className="error-message" role="alert">{errorMessage}</p>}
      </section>
      {reviewId && <>
        <ReviewStatus reviewId={reviewId} status={status} elapsedTime={elapsedTime} errorMessage={errorMessage} isPolling={isPolling} isStreamConnected={isStreamConnected} />
        <ActivityTimeline activities={activities} />
        {status === 'COMPLETED' && <ReviewSummary result={result} />}
        {terminalStatuses.has(status) && <button className="new-review-button" type="button" onClick={handleNewReview}>Start New Review</button>}
      </>}
    </main>
  );
}
