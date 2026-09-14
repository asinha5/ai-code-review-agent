import FindingCard from './FindingCard';

export default function ReviewSummary({ result }) {
  if (!result) return null;
  const findings = result.findings || [];
  return (
    <section className="panel review-summary">
      <div className="panel-heading">
        <div><p className="eyebrow">Completed review</p><h2>Review Summary</h2></div>
        <span className="count">{findings.length} findings</span>
      </div>
      <p className="summary-text">{result.summary}</p>
      {findings.length === 0 ? <p className="empty-state">No findings were reported for this review.</p> : (
        <div className="findings-list">{findings.map((finding, index) => <FindingCard key={`${finding.file}-${finding.line}-${index}`} finding={finding} />)}</div>
      )}
    </section>
  );
}
