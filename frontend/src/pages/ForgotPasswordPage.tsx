import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";

export function ForgotPasswordPage() {
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [resetLink, setResetLink] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const result = await api.post<{ resetToken: string; expiresAt: string }>("/api/auth/forgot-password", {
        username,
      });
      setResetLink(`${window.location.origin}/reset-password?token=${result.resetToken}`);
      setExpiresAt(result.expiresAt);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Could not generate a reset link. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-photo">
        <div className="login-photo-content">
          <h2>Forgot your password?</h2>
          <p>Enter your username and we'll generate a link you can use to set a new password.</p>
        </div>
      </div>
      <div className="login-form-side">
        <div className="login-card">
          <h1>Reset your password</h1>
          <div className="subtitle">Operations Control Centre access</div>
          {error && <div className="error-banner">{error}</div>}

          {resetLink ? (
            <div className="reset-link-box">
              <p>
                This project has no email service configured, so here is your reset link directly. It expires at{" "}
                <strong>{expiresAt ? new Date(expiresAt).toLocaleString() : ""}</strong>.
              </p>
              <Link className="reset-link-box-url" to={resetLink.replace(window.location.origin, "")}>
                {resetLink}
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div>
                <label htmlFor="username">Username</label>
                <input
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  required
                />
              </div>
              <button className="primary" type="submit" disabled={loading}>
                {loading ? "Generating link..." : "Send reset link"}
              </button>
            </form>
          )}

          <div className="login-back-link">
            <Link to="/login">Back to sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
