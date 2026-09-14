export default function RepositoryForm({ repositoryPath, onPathChange, onSubmit, isStarting, isReviewActive }) {
  const disabled = isStarting || isReviewActive;
  const buttonLabel = isStarting ? 'Starting Review...' : isReviewActive ? 'Review Running' : 'Start Review';

  return (
    <form className="repository-form" onSubmit={onSubmit}>
      <div className="form-label-row">
        <label htmlFor="repositoryPath">Repository</label>
        <span>Local path</span>
      </div>
      <div className="form-row">
        <input
          id="repositoryPath"
          name="repositoryPath"
          type="text"
          value={repositoryPath}
          onChange={(event) => onPathChange(event.target.value)}
          placeholder="C:\\work\\my-project"
          disabled={disabled}
          required
          autoComplete="off"
        />
        <button type="submit" disabled={disabled || !repositoryPath.trim()}>{buttonLabel}</button>
      </div>
      <p className="field-help">Select a local source-code repository for AI-assisted analysis.</p>
    </form>
  );
}
