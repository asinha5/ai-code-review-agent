export default function RepositoryForm({ repositoryPath, onPathChange, onSubmit, disabled }) {
  return (
    <form className="repository-form" onSubmit={onSubmit}>
      <label htmlFor="repositoryPath">Repository path</label>
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
        <button type="submit" disabled={disabled || !repositoryPath.trim()}>
          {disabled ? 'Starting review…' : 'Start Review'}
        </button>
      </div>
      <p className="field-help">Enter a repository path that is accessible to the backend service.</p>
    </form>
  );
}
