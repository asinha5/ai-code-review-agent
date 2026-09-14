function formatTime(timestamp) {
  if (!timestamp) return 'Just now';
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(timestamp));
}

export default function ActivityTimeline({ activities }) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Live activity</p>
          <h2>Review timeline</h2>
        </div>
        <span className="count">{activities.length}</span>
      </div>
      {activities.length === 0 ? (
        <p className="empty-state">Waiting for the review service to report activity…</p>
      ) : (
        <ol className="timeline">
          {activities.map((activity, index) => (
            <li key={`${activity.timestamp || 'activity'}-${activity.type}-${index}`}>
              <span className="timeline-dot" aria-hidden="true" />
              <div>
                <div className="activity-meta">
                  <span>{activity.type?.replaceAll('_', ' ')}</span>
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
