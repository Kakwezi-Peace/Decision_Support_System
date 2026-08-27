import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiRequestError } from "../api/client";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username, password);
      navigate("/delay-events");
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-shell">
      <div className="login-photo">
        <div className="login-photo-content">
          <h2>Fly the dream of Africa.</h2>
          <p>
            The Cost-Effective Aircraft Delay Recovery System. Helping RwandAir's Operations
            Control Centre turn disruptions into fast, evidence-based decisions.
          </p>
          <p>
            Every delay is met with a ranked set of recovery options, costed out in seconds using a hybrid MILP and
            Reinforcement Learning engine. Fuel, crew, passenger, slot, and maintenance costs are weighed together so
            dispatchers can choose with confidence, backed by the real constraints of RwandAir's fleet, crew, and
            network.
          </p>
        </div>
      </div>
      <div className="login-form-side">
        <div className="login-card">
          <h1>Sign in</h1>
          <div className="subtitle">Operations Control Centre access</div>
          {error && <div className="error-banner">{error}</div>}
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
            <div>
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button className="primary" type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign in"}
            </button>
          </form>
          <div className="login-back-link">
            <Link to="/forgot-password">Forgot your password?</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
