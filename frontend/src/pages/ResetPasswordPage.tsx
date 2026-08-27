import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!token) {
      setError("This reset link is missing its token. Please request a new one.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await api.post<void>("/api/auth/reset-password", { token, newPassword });
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Could not reset your password. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-photo">
        <div className="login-photo-content">
          <h2>Set a new password.</h2>
          <p>Choose a strong password to get back into the Operations Control Centre.</p>
        </div>
      </div>
      <div className="login-form-side">
        <div className="login-card">
          <h1>Reset your password</h1>
          <div className="subtitle">Operations Control Centre access</div>
          {error && <div className="error-banner">{error}</div>}

          {done ? (
            <div className="success-banner">
              Your password has been reset. <Link to="/login">Sign in</Link> with your new password.
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <div>
                <label htmlFor="newPassword">New password</label>
                <input
                  id="newPassword"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  autoFocus
                  required
                  minLength={8}
                />
              </div>
              <div>
                <label htmlFor="confirmPassword">Confirm new password</label>
                <input
                  id="confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
              <button className="primary" type="submit" disabled={loading}>
                {loading ? "Resetting..." : "Reset password"}
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
