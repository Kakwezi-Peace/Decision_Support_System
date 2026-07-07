import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";
import type { DelayEvent, OptionType, RecoveryScenario } from "../types";

function formatCurrency(value: number) {
  return `$${value.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
}

function impactBadgeClass(score: number) {
  if (score <= 30) return "badge-feasible";
  if (score <= 60) return "badge-at-risk";
  return "badge-infeasible";
}

function crewDutyBadgeClass(status: string) {
  if (status === "COMPLIANT") return "badge-feasible";
  if (status === "AT_RISK") return "badge-at-risk";
  if (status === "VIOLATION") return "badge-infeasible";
  return "badge-unknown";
}

const OPTION_TYPES: OptionType[] = [
  "DELAY_ABSORPTION",
  "FLIGHT_CANCELLATION",
  "AIRCRAFT_SWAP",
  "CREW_SUBSTITUTION",
  "PASSENGER_REROUTING",
  "COMBINED",
];

export function RecoveryPage() {
  const { delayEventId } = useParams<{ delayEventId: string }>();
  const { user } = useAuth();
  const canRunRecovery = canManage(user?.role, "recovery");

  const [delayEvent, setDelayEvent] = useState<DelayEvent | null>(null);
  const [scenario, setScenario] = useState<RecoveryScenario | null>(null);
  const [loading, setLoading] = useState(true);
  const [optimizing, setOptimizing] = useState(false);
  const [selectingId, setSelectingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [showOverrideForm, setShowOverrideForm] = useState(false);
  const [overrideSubmitting, setOverrideSubmitting] = useState(false);
  const [overrideType, setOverrideType] = useState<OptionType>("COMBINED");
  const [overrideDescription, setOverrideDescription] = useState("");
  const [overrideCost, setOverrideCost] = useState(0);

  const [actualCost, setActualCost] = useState(0);
  const [manualBaselineCost, setManualBaselineCost] = useState<number | "">("");
  const [outcomeSubmitting, setOutcomeSubmitting] = useState(false);

  async function loadInitial() {
    setLoading(true);
    setError(null);
    try {
      const event = await api.get<DelayEvent>(`/api/delay-events/${delayEventId}`);
      setDelayEvent(event);
      const existing = await api.get<RecoveryScenario | undefined>(`/api/recovery/delay-events/${delayEventId}/scenario`);
      setScenario(existing ?? null);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load delay event.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadInitial();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [delayEventId]);

  async function handleOptimize() {
    setError(null);
    setOptimizing(true);
    try {
      const result = await api.post<RecoveryScenario>(`/api/recovery/delay-events/${delayEventId}/optimize`);
      setScenario(result);
      setShowOverrideForm(false);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Optimization request failed.");
    } finally {
      setOptimizing(false);
    }
  }

  async function handleSelect(optionId: number) {
    if (!scenario) return;
    setError(null);
    setSelectingId(optionId);
    try {
      const result = await api.post<RecoveryScenario>(
        `/api/recovery/scenarios/${scenario.id}/select/${optionId}`,
        { decisionMadeBy: user?.fullName ?? user?.username },
      );
      setScenario(result);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to record selection.");
    } finally {
      setSelectingId(null);
    }
  }

  async function handleAddOverride(event: FormEvent) {
    event.preventDefault();
    if (!scenario) return;
    setError(null);
    setOverrideSubmitting(true);
    try {
      const result = await api.post<RecoveryScenario>(`/api/recovery/scenarios/${scenario.id}/manual-option`, {
        optionType: overrideType,
        description: overrideDescription,
        totalCost: overrideCost,
        addedBy: user?.fullName ?? user?.username,
      });
      setScenario(result);
      setShowOverrideForm(false);
      setOverrideDescription("");
      setOverrideCost(0);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to add override option.");
    } finally {
      setOverrideSubmitting(false);
    }
  }

  async function handleRecordOutcome(event: FormEvent) {
    event.preventDefault();
    if (!scenario) return;
    setError(null);
    setOutcomeSubmitting(true);
    try {
      const result = await api.post<RecoveryScenario>(`/api/recovery/scenarios/${scenario.id}/outcome`, {
        actualCost,
        manualBaselineCost: manualBaselineCost === "" ? null : manualBaselineCost,
      });
      setScenario(result);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to record outcome.");
    } finally {
      setOutcomeSubmitting(false);
    }
  }

  if (loading) {
    return <p className="empty-state">Loading...</p>;
  }

  if (!delayEvent) {
    return <div className="error-banner">{error ?? "Delay event not found."}</div>;
  }

  const isResolved = scenario?.status === "RESOLVED";
  const outcomeRecorded = scenario?.actualCost !== null && scenario?.actualCost !== undefined;

  return (
    <div className="page-photo-page page-photo-page--tarmac">
      <div className="page-title-chip">
        <Link to="/delay-events" className="back-link">
          ← Back to delay events
        </Link>
        <h1>Recovery Decision</h1>
        <p>MILP + RL ranked options, cost trade-offs, and the final call — all in one place.</p>
      </div>

      <div className="card">
        <div className="section-actions">
          <div>
            <h2>
              Flight {delayEvent.flightNumber} — {delayEvent.delayCategory} delay
            </h2>
            <span className="cost-breakdown">
              {delayEvent.delayMinutes} minutes · reported {new Date(delayEvent.reportedAt).toLocaleString()} ·{" "}
              <span className={`status-pill status-${delayEvent.status}`}>{delayEvent.status}</span>
            </span>
          </div>
          {canRunRecovery && (
            <button className="primary" onClick={handleOptimize} disabled={optimizing}>
              {optimizing ? "Optimizing..." : scenario ? "Re-run optimization" : "Run optimization"}
            </button>
          )}
        </div>
        {error && <div className="error-banner">{error}</div>}
        {!scenario && !error && (
          <p className="empty-state">
            {canRunRecovery
              ? 'No recovery scenario yet. Click "Run optimization" to generate cost-ranked recovery options via the MILP/RL engine.'
              : "No recovery scenario yet. Only Operations Controllers and Admins can run the optimizer."}
          </p>
        )}
      </div>

      {scenario && (
        <div className="card">
          <div className="section-actions">
            <h2>
              {scenario.scenarioTitle}{" "}
              <span className={`priority-${scenario.priority}`}>[{scenario.priority} priority]</span>
            </h2>
            <span className={`status-pill status-${scenario.status}`}>{scenario.status}</span>
          </div>
          {scenario.computationTimeMs > 0 && (
            <p className="cost-breakdown">Solved in {scenario.computationTimeMs.toFixed(1)} ms.</p>
          )}
          {scenario.status === "RESOLVED" && (
            <p className="cost-breakdown">
              ✓ Decision recorded by <strong>{scenario.decisionMadeBy || "unknown"}</strong>
              {scenario.resolvedAt && ` on ${new Date(scenario.resolvedAt).toLocaleString()}`}.
            </p>
          )}

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Option</th>
                  <th>Source</th>
                  <th>Total cost</th>
                  <th>Delay reduction</th>
                  <th>Passenger impact</th>
                  <th>Crew duty / regulatory</th>
                  <th>Feasibility</th>
                  {canRunRecovery && <th></th>}
                </tr>
              </thead>
              <tbody>
                {scenario.options.map((option) => (
                  <tr
                    key={option.id}
                    className={[option.selected ? "selected-row" : "", option.rank === 1 ? "recommended-row" : ""].join(" ")}
                  >
                    <td>#{option.rank}</td>
                    <td>
                      <strong>{option.optionType.replaceAll("_", " ")}</strong>
                      <div className="cost-breakdown">{option.description}</div>
                      <div className="cost-breakdown">
                        Fuel {formatCurrency(option.fuelCost)} · Crew {formatCurrency(option.crewOvertimeCost)} · Pax{" "}
                        {formatCurrency(option.passengerCompensationCost)} · Slot {formatCurrency(option.slotPenaltyCost)} ·
                        MRO {formatCurrency(option.mroCost)}
                      </div>
                    </td>
                    <td>
                      <span className={`badge badge-${option.generatedBy.toLowerCase()}`}>{option.generatedBy}</span>
                    </td>
                    <td>
                      <strong>{formatCurrency(option.totalCost)}</strong>
                    </td>
                    <td>{option.estimatedDelayReduction} min</td>
                    <td>
                      {option.passengerImpactScore === null ? (
                        <span className="badge badge-unknown">Not assessed</span>
                      ) : (
                        <span className={`badge ${impactBadgeClass(option.passengerImpactScore)}`}>
                          {option.passengerImpactScore.toFixed(0)} / 100
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${crewDutyBadgeClass(option.crewDutyCompliance)}`}>
                        {option.crewDutyCompliance.replaceAll("_", " ")}
                      </span>
                      {option.regulatoryFeasible === false && (
                        <div className="cost-breakdown">⚠ Not regulatorily feasible</div>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${option.feasible ? "badge-feasible" : "badge-infeasible"}`}>
                        {option.feasible ? "Feasible" : "Infeasible"}
                      </span>
                      <div className="cost-breakdown">{option.feasibilityNotes}</div>
                    </td>
                    {canRunRecovery && (
                      <td>
                        {option.selected ? (
                          <span className="badge badge-feasible">Selected</span>
                        ) : (
                          <button
                            className="secondary"
                            disabled={!option.feasible || isResolved || selectingId !== null}
                            onClick={() => handleSelect(option.id)}
                          >
                            {selectingId === option.id ? "Saving..." : "Select"}
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {canRunRecovery && !isResolved && (
            <div style={{ marginTop: "1rem" }}>
              {!showOverrideForm ? (
                <button className="secondary" onClick={() => setShowOverrideForm(true)}>
                  + Add override option
                </button>
              ) : (
                <form className="inline-form" onSubmit={handleAddOverride}>
                  <div>
                    <label htmlFor="ov-type">Option type</label>
                    <select id="ov-type" value={overrideType} onChange={(e) => setOverrideType(e.target.value as OptionType)}>
                      {OPTION_TYPES.map((t) => (
                        <option key={t} value={t}>
                          {t.replaceAll("_", " ")}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="ov-desc">Describe the approach (tacit knowledge not captured by the model)</label>
                    <input
                      id="ov-desc"
                      value={overrideDescription}
                      onChange={(e) => setOverrideDescription(e.target.value)}
                      placeholder="e.g. Wet-lease a partner aircraft already positioned at KGL"
                      required
                    />
                  </div>
                  <div>
                    <label htmlFor="ov-cost">Your total cost estimate ($)</label>
                    <input
                      id="ov-cost"
                      type="number"
                      min={0}
                      value={overrideCost}
                      onChange={(e) => setOverrideCost(Number(e.target.value))}
                      required
                    />
                  </div>
                  <div className="row-actions">
                    <button type="submit" disabled={overrideSubmitting}>
                      {overrideSubmitting ? "Adding..." : "Add override option"}
                    </button>
                    <button type="button" className="secondary" onClick={() => setShowOverrideForm(false)}>
                      Cancel
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}

          {canRunRecovery && isResolved && (
            <div style={{ marginTop: "1.25rem", borderTop: "1px solid var(--border)", paddingTop: "1rem" }}>
              <h3>Post-recovery outcome</h3>
              {!outcomeRecorded ? (
                <>
                  <p className="cost-breakdown">
                    Once this recovery has actually played out, record what it really cost. This feeds back into the RL
                    model so its policy improves from real experience.
                  </p>
                  <form className="inline-form" onSubmit={handleRecordOutcome}>
                    <div>
                      <label htmlFor="actual-cost">Actual cost incurred ($)</label>
                      <input
                        id="actual-cost"
                        type="number"
                        min={0}
                        value={actualCost}
                        onChange={(e) => setActualCost(Number(e.target.value))}
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="manual-baseline-cost">Manual-approach cost estimate ($, optional)</label>
                      <input
                        id="manual-baseline-cost"
                        type="number"
                        min={0}
                        placeholder="What would this have cost without the DSS?"
                        value={manualBaselineCost}
                        onChange={(e) => setManualBaselineCost(e.target.value === "" ? "" : Number(e.target.value))}
                      />
                    </div>
                    <button type="submit" disabled={outcomeSubmitting}>
                      {outcomeSubmitting ? "Recording..." : "Record outcome & update RL model"}
                    </button>
                  </form>
                </>
              ) : (
                <div className="cost-breakdown">
                  <p>
                    Actual cost: <strong>{formatCurrency(scenario.actualCost ?? 0)}</strong> ·{" "}
                    {(scenario.actualCostSaved ?? 0) >= 0 ? "Saved vs estimate: " : "Over estimate by: "}
                    <strong>{formatCurrency(Math.abs(scenario.actualCostSaved ?? 0))}</strong>
                  </p>
                  {scenario.manualBaselineCost !== null && scenario.manualBaselineCost !== undefined && (
                    <p>
                      Manual-approach estimate: <strong>{formatCurrency(scenario.manualBaselineCost)}</strong> vs. actual{" "}
                      <strong>{formatCurrency(scenario.actualCost ?? 0)}</strong>
                    </p>
                  )}
                  {scenario.rlFeedbackMessage && (
                    <p>
                      {scenario.rlPolicyUpdated ? "✓ " : "— "}
                      {scenario.rlFeedbackMessage}
                    </p>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
