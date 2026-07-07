import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function HomePage() {
  const { user } = useAuth();

  return (
    <div className="home-hero">
      <div className="home-hero-overlay" />

      <header className="home-hero-topbar">
        <div className="home-hero-brand">
          <img src="/images/img1.png" alt="RwandAir" />
          <span>RwandAir OCC</span>
        </div>
        <Link className="home-hero-signin" to={user ? "/delay-events" : "/login"}>
          {user ? "Go to Dashboard" : "Sign in"}
        </Link>
      </header>

      <div className="home-hero-content">
        <span className="home-hero-eyebrow">Decision Support System</span>
        <h1>Every delay meets a plan, in seconds.</h1>
        <p>
          When a flight disrupts, RwandAir's Operations Control Centre has minutes to decide, not hours. This
          system turns that pressure into confidence: absorb, cancel, swap the aircraft, swap the crew, or reroute
          passengers, every option costed, ranked, and ready before the decision has to be made.
        </p>
        <p>
          A hybrid engine pairs Mixed-Integer Linear Programming with Reinforcement Learning that keeps improving
          from every real outcome, so the recommendation gets sharper the more RwandAir flies. Built for the
          Operations Control Centre, Crew Scheduling, Maintenance, and Commercial Services teams who keep Rwanda's
          flag carrier moving.
        </p>
        <div className="home-hero-ctas">
          <Link className="home-hero-cta-primary" to={user ? "/delay-events" : "/login"}>
            {user ? "Open the Dashboard" : "Sign in to the OCC"}
          </Link>
        </div>

        <div className="home-hero-stats">
          <div>
            <strong>5</strong>
            <span>recovery strategies costed and ranked automatically</span>
          </div>
          <div>
            <strong>MILP + RL</strong>
            <span>hybrid engine that learns from every real outcome</span>
          </div>
          <div>
            <strong>Fuel · Crew · Pax · Slot · MRO</strong>
            <span>full cost breakdown behind every recommendation</span>
          </div>
        </div>
      </div>

      <footer className="home-hero-footer">
        Cost-optimal aircraft delay recovery for RwandAir. Connecting Africa, one decision at a time.
      </footer>
    </div>
  );
}
