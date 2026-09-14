import { useEffect, useRef } from 'react';

const activityLabels = {
  REVIEW_STARTED: 'Review Started',
  REPOSITORY_INSPECTION: 'Repository Inspection',
  FILE_READING: 'Reading File',
  CODE_SEARCH: 'Searching Code',
  ANALYZING: 'Analyzing',
  GENERATING_FINDINGS: 'Processing Findings',
  REVIEW_COMPLETED: 'Review Completed',
  REVIEW_FAILED: 'Review Failed',
};

function formatTime(timestamp) {
  if (!timestamp) return 'Just now';
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(timestamp));
}

export default function ActivityTimeline({ activities }) {
  const listRef = useRef(null);

  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTop = list.scrollHeight;
  }, [activities]);

  function markerFor(activity, isLatest) {
    if (activity.type === 'REVIEW_FAILED') return '!';
    if (activity.type === 'REVIEW_COMPLETED' || !isLatest) return '✓';
    return '●';
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Live agent activity</p>
          <h2>Activity timeline</h2>
        </div>
        <span className="count">{activities.length}</span>
      </div>
      {activities.length === 0 ? <p className="empty-state">Waiting for agent activity...</p> : (
        <ol className="timeline" ref={listRef} aria-live="polite">
          {activities.map((activity, index) => (
            <li key={`${activity.timestamp || 'activity'}-${activity.type}-${index}`} className={index === activities.length - 1 ? 'is-latest' : ''}>
              <span className={`timeline-marker marker-${activity.type?.toLowerCase()}`} aria-hidden="true">{markerFor(activity, index === activities.length - 1)}</span>
              <div>
                <div className="activity-meta">
                  <span>{activityLabels[activity.type] || activity.type?.replaceAll('_', ' ')}</span>
                  <time dateTime={activity.timestamp}>{formatTime(activity.timestamp)}</time>
                </div>
                <p>{activity.message}</p>
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
