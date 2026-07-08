import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";
import type { DelayCategory, RecoveryScenarioSummary, ScenarioStatus } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "../components/Pagination";
import { sortByIdDesc } from "../utils/sort";

const PAGE_SIZE = 8;

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined) return "—";
  return `$${value.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : "—";
}

const VALID_STATUSES: ScenarioStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

export function RecoveryHistoryPage() {
  const [searchParams] = useSearchParams();
  const initialStatus = searchParams.get("status");
  const validInitialStatus = VALID_STATUSES.includes(initialStatus as ScenarioStatus)
    ? (initialStatus as ScenarioStatus)
    : "";

  const [scenarios, setScenarios] = useState<RecoveryScenarioSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState<DelayCategory | "">("");
  const [statusFilter, setStatusFilter] = useState<ScenarioStatus | "">(validInitialStatus);

  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    async function load() {
      try {
        setScenarios(sortByIdDesc(await api.get<RecoveryScenarioSummary[]>("/api/recovery/scenarios")));
      } catch (err) {
        setError(err instanceof ApiRequestError ? err.message : "Failed to load recovery history.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return scenarios.filter((s) => {
      if (categoryFilter && s.delayCategory !== categoryFilter) return false;
      if (statusFilter && s.status !== statusFilter) return false;
      if (q && !`${s.flightNumber ?? ""} ${s.decisionMadeBy ?? ""}`.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [scenarios, search, categoryFilter, statusFilter]);

  const { page, setPage, totalPages, pageItems } = usePagination(filtered, PAGE_SIZE);

  function toggleSelected(id: number) {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  const compareScenarios = scenarios.filter((s) => selectedIds.includes(s.id));

  return (
    <div className="page-photo-page page-photo-page--tarmac">
      <div className="page-title-chip">
        <h1>Recovery History</h1>
        <p>
          A searchable record of every recovery decision and its outcome, for post-operational analysis and
          controller training. Select two or more rows to compare them side by side.
        </p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card">
        <div className="inline-form" style={{ marginBottom: "0.5rem" }}>
          <div>
            <label htmlFor="hist-search">Search flight or decision-maker</label>
            <input
              id="hist-search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="e.g. WB612 or Default Administrator"
            />
          </div>
          <div>
            <label htmlFor="hist-category">Delay category</label>
            <select id="hist-category" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value as DelayCategory | "")}>
              <option value="">All</option>
              {["TECHNICAL", "WEATHER", "ATC", "CREW", "COMMERCIAL", "REACTIONARY", "OTHER"].map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="hist-status">Status</label>
            <select id="hist-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as ScenarioStatus | "")}>
              <option value="">All</option>
              {VALID_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <p className="empty-state">Loading recovery history...</p>
        ) : filtered.length === 0 ? (
          <p className="empty-state">No recovery scenarios match these filters.</p>
        ) : (
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th></th>
                  <th>Flight</th>
                  <th>Status / priority</th>
                  <th>Decision</th>
                  <th>Recovery option</th>
                  <th>Cost</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {pageItems.map((s) => (
                  <tr key={s.id} className={selectedIds.includes(s.id) ? "selected-row" : ""}>
                    <td>
                      <input type="checkbox" checked={selectedIds.includes(s.id)} onChange={() => toggleSelected(s.id)} />
                    </td>
                    <td>
                      <strong>{s.flightNumber}</strong> {s.origin && s.destination ? `(${s.origin}→${s.destination})` : ""}
                      <div className="cost-breakdown">
                        {s.delayCategory ?? "—"} · {s.delayMinutes} min delay
                      </div>
                    </td>
                    <td>
                      <span className={`status-pill status-${s.status}`}>{s.status}</span>
                      <div className={`cost-breakdown priority-${s.priority}`}>{s.priority} priority</div>
                    </td>
                    <td>
                      {s.decisionMadeBy ?? "—"}
                      <div className="cost-breakdown">{formatDate(s.resolvedAt)}</div>
                    </td>
                    <td>{s.selectedOptionType ? s.selectedOptionType.replaceAll("_", " ") : "—"}</td>
                    <td>
                      <div className="cost-breakdown">Est {formatCurrency(s.selectedOptionCost)}</div>
                      <div className="cost-breakdown">
                        Actual {s.outcomeRecorded ? formatCurrency(s.actualCost) : "—"}
                      </div>
                      <div className="cost-breakdown">
                        Saved {s.outcomeRecorded ? formatCurrency(s.actualCostSaved) : "—"}
                      </div>
                    </td>
                    <td>
                      <Link to={`/delay-events/${s.delayEventId}`} className="secondary-link">
                        View →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>

      {compareScenarios.length >= 2 && (
        <div className="card">
          <div className="section-actions">
            <h2>Comparing {compareScenarios.length} scenarios</h2>
            <button className="secondary" onClick={() => setSelectedIds([])}>
              Clear selection
            </button>
          </div>
          <div className="history-table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Flight</th>
                  <th>Cause / delay</th>
                  <th>Recovery option</th>
                  <th>Cost</th>
                  <th>Decision time</th>
                </tr>
              </thead>
              <tbody>
                {compareScenarios.map((s) => {
                  const decisionMinutes =
                    s.optimisationRunAt && s.resolvedAt
                      ? Math.round((new Date(s.resolvedAt).getTime() - new Date(s.optimisationRunAt).getTime()) / 60000)
                      : null;
                  return (
                    <tr key={s.id}>
                      <td>
                        <strong>{s.flightNumber}</strong>
                      </td>
                      <td>
                        {s.delayCategory} · {s.delayMinutes} min
                      </td>
                      <td>{s.selectedOptionType ? s.selectedOptionType.replaceAll("_", " ") : "—"}</td>
                      <td>
                        <div className="cost-breakdown">Est {formatCurrency(s.selectedOptionCost)}</div>
                        <div className="cost-breakdown">
                          Actual {s.outcomeRecorded ? formatCurrency(s.actualCost) : "Not yet recorded"}
                        </div>
                        <div className="cost-breakdown">Saved {s.outcomeRecorded ? formatCurrency(s.actualCostSaved) : "—"}</div>
                        <div className="cost-breakdown">Manual baseline {formatCurrency(s.manualBaselineCost)}</div>
                      </td>
                      <td>{decisionMinutes !== null ? `${decisionMinutes} min` : "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
