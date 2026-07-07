import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";
import type { DelayCategory, DelayEvent, Flight } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "../components/Pagination";
import { sortByIdDesc } from "../utils/sort";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";

const DELAY_CATEGORIES: DelayCategory[] = [
  "TECHNICAL",
  "WEATHER",
  "ATC",
  "CREW",
  "COMMERCIAL",
  "REACTIONARY",
  "OTHER",
];
const PAGE_SIZE = 5;

const DEFAULTS = {
  flightId: "" as number | "",
  delayCategory: "TECHNICAL" as DelayCategory,
  delayMinutes: 60,
  delayCause: "",
  reportedBy: "",
};

export function DelayEventsPage() {
  const { user } = useAuth();
  const canEdit = canManage(user?.role, "delayEvents");
  const [delayEvents, setDelayEvents] = useState<DelayEvent[]>([]);
  const [flights, setFlights] = useState<Flight[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(delayEvents, PAGE_SIZE);

  const [flightId, setFlightId] = useState<number | "">(DEFAULTS.flightId);
  const [delayCategory, setDelayCategory] = useState<DelayCategory>(DEFAULTS.delayCategory);
  const [delayMinutes, setDelayMinutes] = useState(DEFAULTS.delayMinutes);
  const [delayCause, setDelayCause] = useState(DEFAULTS.delayCause);
  const [reportedBy, setReportedBy] = useState(DEFAULTS.reportedBy);

  async function loadData() {
    try {
      const [events, flightData] = await Promise.all([
        api.get<DelayEvent[]>("/api/delay-events"),
        api.get<Flight[]>("/api/flights"),
      ]);
      setDelayEvents(sortByIdDesc(events));
      setFlights(sortByIdDesc(flightData));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load delay events.");
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  function resetForm() {
    setFlightId(DEFAULTS.flightId);
    setDelayCategory(DEFAULTS.delayCategory);
    setDelayMinutes(DEFAULTS.delayMinutes);
    setDelayCause(DEFAULTS.delayCause);
    setReportedBy(DEFAULTS.reportedBy);
  }

  function startEdit(event: DelayEvent) {
    setEditingId(event.id);
    setFlightId(event.flightId);
    setDelayCategory(event.delayCategory);
    setDelayMinutes(event.delayMinutes);
    setDelayCause(event.delayCause || "");
    setReportedBy(event.reportedBy || "");
    setError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    resetForm();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!flightId) {
      setError("Select a flight for this delay event.");
      return;
    }
    setError(null);
    setSubmitting(true);
    const payload = { flightId, delayCategory, delayMinutes, delayCause, reportedBy };
    try {
      if (editingId) {
        await api.put<DelayEvent>(`/api/delay-events/${editingId}`, payload);
        cancelEdit();
      } else {
        await api.post<DelayEvent>("/api/delay-events", payload);
        setDelayCause(DEFAULTS.delayCause);
      }
      await loadData();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save delay event.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(event: DelayEvent) {
    if (
      !window.confirm(
        `Delete this delay event for flight ${event.flightNumber}? Any recovery scenario/options generated for it will be deleted too.`,
      )
    )
      return;
    setError(null);
    setDeletingId(event.id);
    try {
      await api.delete(`/api/delay-events/${event.id}`);
      if (editingId === event.id) cancelEdit();
      await loadData();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to delete delay event.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="page-photo-page page-photo-page--tarmac">
      <div className="page-title-chip">
        <h1>Delay Events</h1>
        <p>Report a disruption and track every active, in-progress, and resolved delay across the network.</p>
      </div>

      <div className="card">
        <h2>{editingId ? "Edit Delay Event" : "Report a Delay"}</h2>
        {error && <div className="error-banner">{error}</div>}
        {!canEdit && (
          <p className="cost-breakdown">
            You have read-only access to delay events. Only Operations Controllers and Admins can report or edit them.
          </p>
        )}
        {canEdit && (
        <form className="inline-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="de-flight">Flight</label>
            <select
              id="de-flight"
              value={flightId}
              onChange={(e) => setFlightId(e.target.value ? Number(e.target.value) : "")}
              required
            >
              <option value="">Select a flight...</option>
              {flights.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.flightNumber}: {f.origin} → {f.destination} ({new Date(f.scheduledDeparture).toLocaleString()})
                </option>
              ))}
            </select>
            {flights.length === 0 && (
              <div className="cost-breakdown">
                No flights yet — add one under "Operational Data" first, then it'll show up here.
              </div>
            )}
          </div>
          <div>
            <label htmlFor="de-category">Category</label>
            <select id="de-category" value={delayCategory} onChange={(e) => setDelayCategory(e.target.value as DelayCategory)}>
              {DELAY_CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="de-minutes">Delay (minutes)</label>
            <input
              id="de-minutes"
              type="number"
              min={1}
              value={delayMinutes}
              onChange={(e) => setDelayMinutes(Number(e.target.value))}
            />
          </div>
          <div>
            <label htmlFor="de-cause">Cause</label>
            <input id="de-cause" value={delayCause} onChange={(e) => setDelayCause(e.target.value)} />
          </div>
          <div>
            <label htmlFor="de-reporter">Reported by</label>
            <input id="de-reporter" value={reportedBy} onChange={(e) => setReportedBy(e.target.value)} />
          </div>
          <div className="row-actions">
            <button type="submit" disabled={submitting}>
              {editingId ? "Save changes" : "Report delay"}
            </button>
            {editingId && (
              <button type="button" className="secondary" onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
        )}
      </div>

      <div className="card">
        <h2>Delay Events</h2>
        {delayEvents.length === 0 ? (
          <p className="empty-state">No delay events reported yet.</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Flight</th>
                  <th>Category</th>
                  <th>Delay</th>
                  <th>Status</th>
                  <th>Reported</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {pageItems.map((event) => (
                  <tr key={event.id}>
                    <td>{event.flightNumber}</td>
                    <td>{event.delayCategory}</td>
                    <td>{event.delayMinutes} min</td>
                    <td>
                      <span className={`status-pill status-${event.status}`}>{event.status}</span>
                    </td>
                    <td>{new Date(event.reportedAt).toLocaleString()}</td>
                    <td>
                      <div className="row-actions">
                        <Link to={`/delay-events/${event.id}`}>
                          <button className="secondary">Recovery options →</button>
                        </Link>
                        {canEdit && (
                          <>
                            <button className="secondary" onClick={() => startEdit(event)}>
                              Edit
                            </button>
                            <button
                              className="danger"
                              disabled={deletingId === event.id}
                              onClick={() => handleDelete(event)}
                            >
                              {deletingId === event.id ? "Deleting..." : "Delete"}
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </div>
  );
}
