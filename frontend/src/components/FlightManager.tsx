import { useEffect, useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Aircraft, Flight } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "./Pagination";
import { sortByIdDesc } from "../utils/sort";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";

const PAGE_SIZE = 5;

function defaultDateTime(hoursFromNow: number) {
  const d = new Date(Date.now() + hoursFromNow * 3600_000);
  d.setSeconds(0, 0);
  return d.toISOString().slice(0, 16);
}

const DEFAULTS = {
  flightNumber: "",
  origin: "KGL",
  destination: "",
  aircraftId: "" as number | "",
  passengerCount: 120,
  availableSeats: 150,
};

interface FlightManagerProps {
  aircraftOptions: Aircraft[];
}

export function FlightManager({ aircraftOptions }: FlightManagerProps) {
  const { user } = useAuth();
  const canEdit = canManage(user?.role, "flights");
  const [flights, setFlights] = useState<Flight[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(flights, PAGE_SIZE);

  const [flightNumber, setFlightNumber] = useState(DEFAULTS.flightNumber);
  const [origin, setOrigin] = useState(DEFAULTS.origin);
  const [destination, setDestination] = useState(DEFAULTS.destination);
  const [scheduledDeparture, setScheduledDeparture] = useState(defaultDateTime(2));
  const [scheduledArrival, setScheduledArrival] = useState(defaultDateTime(4));
  const [aircraftId, setAircraftId] = useState<number | "">(DEFAULTS.aircraftId);
  const [passengerCount, setPassengerCount] = useState(DEFAULTS.passengerCount);
  const [availableSeats, setAvailableSeats] = useState(DEFAULTS.availableSeats);

  async function loadFlights() {
    try {
      setFlights(sortByIdDesc(await api.get<Flight[]>("/api/flights")));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load flights.");
    }
  }

  useEffect(() => {
    loadFlights();
  }, []);

  function resetForm() {
    setFlightNumber(DEFAULTS.flightNumber);
    setOrigin(DEFAULTS.origin);
    setDestination(DEFAULTS.destination);
    setScheduledDeparture(defaultDateTime(2));
    setScheduledArrival(defaultDateTime(4));
    setAircraftId(DEFAULTS.aircraftId);
    setPassengerCount(DEFAULTS.passengerCount);
    setAvailableSeats(DEFAULTS.availableSeats);
  }

  function startEdit(f: Flight) {
    setEditingId(f.id);
    setFlightNumber(f.flightNumber);
    setOrigin(f.origin);
    setDestination(f.destination);
    setScheduledDeparture(f.scheduledDeparture.slice(0, 16));
    setScheduledArrival(f.scheduledArrival.slice(0, 16));
    setAircraftId(f.aircraftId);
    setPassengerCount(f.passengerCount);
    setAvailableSeats(f.availableSeats);
    setError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    resetForm();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!aircraftId) {
      setError("Select an aircraft for this flight.");
      return;
    }
    setError(null);
    setSubmitting(true);
    const payload = {
      flightNumber,
      origin,
      destination,
      scheduledDeparture: `${scheduledDeparture}:00`,
      scheduledArrival: `${scheduledArrival}:00`,
      aircraftId,
      passengerCount,
      availableSeats,
    };
    try {
      if (editingId) {
        await api.put<Flight>(`/api/flights/${editingId}`, payload);
        cancelEdit();
      } else {
        await api.post<Flight>("/api/flights", payload);
        resetForm();
      }
      await loadFlights();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save flight.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(f: Flight) {
    if (!window.confirm(`Delete flight ${f.flightNumber}? This cannot be undone.`)) return;
    setError(null);
    setDeletingId(f.id);
    try {
      await api.delete(`/api/flights/${f.id}`);
      if (editingId === f.id) cancelEdit();
      await loadFlights();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to delete flight.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="card">
      <h2>Flights</h2>
      {error && <div className="error-banner">{error}</div>}
      {!canEdit && (
        <p className="cost-breakdown">
          You have read-only access to flight data. Only Operations Controllers and Admins can add or edit flights.
        </p>
      )}
      {canEdit && (
      <form className="inline-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="fl-num">Flight number</label>
          <input id="fl-num" placeholder="WB301" value={flightNumber} onChange={(e) => setFlightNumber(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="fl-origin">Origin (IATA code)</label>
          <input
            id="fl-origin"
            value={origin}
            onChange={(e) => setOrigin(e.target.value.toUpperCase())}
            maxLength={3}
            pattern="[A-Z]{3}"
            title="3-letter IATA airport code, e.g. KGL"
            placeholder="KGL"
            required
          />
        </div>
        <div>
          <label htmlFor="fl-dest">Destination (IATA code)</label>
          <input
            id="fl-dest"
            value={destination}
            onChange={(e) => setDestination(e.target.value.toUpperCase())}
            maxLength={3}
            pattern="[A-Z]{3}"
            title="3-letter IATA airport code, e.g. NBO"
            placeholder="NBO"
            required
          />
        </div>
        <div>
          <label htmlFor="fl-aircraft">Aircraft</label>
          <select
            id="fl-aircraft"
            value={aircraftId}
            onChange={(e) => setAircraftId(e.target.value ? Number(e.target.value) : "")}
            required
          >
            <option value="">Select...</option>
            {aircraftOptions.map((a) => (
              <option key={a.id} value={a.id}>
                {a.registrationNumber} ({a.currentLocation})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="fl-dep">Scheduled departure</label>
          <input
            id="fl-dep"
            type="datetime-local"
            value={scheduledDeparture}
            onChange={(e) => setScheduledDeparture(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="fl-arr">Scheduled arrival</label>
          <input
            id="fl-arr"
            type="datetime-local"
            value={scheduledArrival}
            onChange={(e) => setScheduledArrival(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="fl-pax">Passengers</label>
          <input
            id="fl-pax"
            type="number"
            min={0}
            value={passengerCount}
            onChange={(e) => setPassengerCount(Number(e.target.value))}
          />
        </div>
        <div>
          <label htmlFor="fl-seats">Available seats</label>
          <input
            id="fl-seats"
            type="number"
            min={0}
            value={availableSeats}
            onChange={(e) => setAvailableSeats(Number(e.target.value))}
          />
        </div>
        <div className="row-actions">
          <button type="submit" disabled={submitting}>
            {editingId ? "Save changes" : "Add flight"}
          </button>
          {editingId && (
            <button type="button" className="secondary" onClick={cancelEdit}>
              Cancel
            </button>
          )}
        </div>
      </form>
      )}

      {flights.length === 0 ? (
        <p className="empty-state">No flights recorded yet.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Flight</th>
                <th>Route</th>
                <th>Aircraft</th>
                <th>Status</th>
                <th>Pax</th>
                {canEdit && <th></th>}
              </tr>
            </thead>
            <tbody>
              {pageItems.map((f) => (
                <tr key={f.id}>
                  <td>{f.flightNumber}</td>
                  <td>
                    {f.origin} → {f.destination}
                  </td>
                  <td>{f.aircraftRegistration}</td>
                  <td>{f.status}</td>
                  <td>{f.passengerCount}</td>
                  {canEdit && (
                    <td>
                      <div className="row-actions">
                        <button className="secondary" onClick={() => startEdit(f)}>
                          Edit
                        </button>
                        <button className="danger" disabled={deletingId === f.id} onClick={() => handleDelete(f)}>
                          {deletingId === f.id ? "Deleting..." : "Delete"}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
