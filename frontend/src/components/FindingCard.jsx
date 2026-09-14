export default function FindingCard({ finding }) {
  const location = finding.line ? `${finding.file}:${finding.line}` : finding.file;
  return (
    <article className="finding-card">
      <div className="finding-topline">
        <span className={`severity severity-${finding.severity?.toLowerCase()}`}>{finding.severity}</span>
        <span className="category">{finding.category}</span>
        <span className="confidence">Confidence: {finding.confidence}</span>
      </div>
      <h3>{finding.issue}</h3>
      <p className="location">{location}</p>
      <div className="finding-detail">
        <h4>Evidence</h4>
        <p>{finding.evidence}</p>
      </div>
      <div className="finding-detail">
        <h4>Recommendation</h4>
        <p>{finding.recommendation}</p>
      </div>
    </article>
  );
}
