import { useEffect, useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Flight, Passenger, PassengerStatus, TicketClass } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "./Pagination";
import { sortByIdDesc } from "../utils/sort";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";

const PAGE_SIZE = 5;

const DEFAULTS = {
  firstName: "",
  lastName: "",
  bookingReference: "",
  seatNumber: "",
  ticketClass: "ECONOMY" as TicketClass,
  flightId: "" as number | "",
  hasConnection: false,
  connectionFlight: "",
  connectionDeadlineMinutes: 60,
  reAccommodated: false,
  compensationPaid: 0,
  status: "CHECKED_IN" as PassengerStatus,
};

export function PassengerManager() {
  const { user } = useAuth();
  const canEdit = canManage(user?.role, "passengers");
  const [passengers, setPassengers] = useState<Passenger[]>([]);
  const [flights, setFlights] = useState<Flight[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(passengers, PAGE_SIZE);

  const [firstName, setFirstName] = useState(DEFAULTS.firstName);
  const [lastName, setLastName] = useState(DEFAULTS.lastName);
  const [bookingReference, setBookingReference] = useState(DEFAULTS.bookingReference);
  const [seatNumber, setSeatNumber] = useState(DEFAULTS.seatNumber);
  const [ticketClass, setTicketClass] = useState<TicketClass>(DEFAULTS.ticketClass);
  const [flightId, setFlightId] = useState<number | "">(DEFAULTS.flightId);
  const [hasConnection, setHasConnection] = useState(DEFAULTS.hasConnection);
  const [connectionFlight, setConnectionFlight] = useState(DEFAULTS.connectionFlight);
  const [connectionDeadlineMinutes, setConnectionDeadlineMinutes] = useState(DEFAULTS.connectionDeadlineMinutes);
  const [reAccommodated, setReAccommodated] = useState(DEFAULTS.reAccommodated);
  const [compensationPaid, setCompensationPaid] = useState(DEFAULTS.compensationPaid);
  const [status, setStatus] = useState<PassengerStatus>(DEFAULTS.status);

  async function loadPassengers() {
    try {
      setPassengers(sortByIdDesc(await api.get<Passenger[]>("/api/passengers")));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load passengers.");
    }
  }

  async function loadFlights() {
    try {
      setFlights(await api.get<Flight[]>("/api/flights"));
    } catch {
      // flight dropdown just stays empty; the passenger list itself still loads
    }
  }

  useEffect(() => {
    loadPassengers();
    loadFlights();
  }, []);

  function resetForm() {
    setFirstName(DEFAULTS.firstName);
    setLastName(DEFAULTS.lastName);
    setBookingReference(DEFAULTS.bookingReference);
    setSeatNumber(DEFAULTS.seatNumber);
    setTicketClass(DEFAULTS.ticketClass);
    setFlightId(DEFAULTS.flightId);
    setHasConnection(DEFAULTS.hasConnection);
    setConnectionFlight(DEFAULTS.connectionFlight);
    setConnectionDeadlineMinutes(DEFAULTS.connectionDeadlineMinutes);
    setReAccommodated(DEFAULTS.reAccommodated);
    setCompensationPaid(DEFAULTS.compensationPaid);
    setStatus(DEFAULTS.status);
  }

  function startEdit(p: Passenger) {
    setEditingId(p.id);
    setFirstName(p.firstName);
    setLastName(p.lastName);
    setBookingReference(p.bookingReference);
    setSeatNumber(p.seatNumber ?? "");
    setTicketClass(p.ticketClass);
    setFlightId(p.flightId);
    setHasConnection(p.hasConnection);
    setConnectionFlight(p.connectionFlight ?? "");
    setConnectionDeadlineMinutes(p.connectionDeadlineMinutes);
    setReAccommodated(p.reAccommodated);
    setCompensationPaid(p.compensationPaid);
    setStatus(p.status);
    setError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    resetForm();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!flightId) {
      setError("Select a flight for this passenger.");
      return;
    }
    setError(null);
    setSubmitting(true);
    const payload = {
      flightId,
      firstName,
      lastName,
      bookingReference,
      seatNumber,
      ticketClass,
      hasConnection,
      connectionFlight: hasConnection ? connectionFlight : "",
      connectionDeadlineMinutes: hasConnection ? connectionDeadlineMinutes : 0,
      reAccommodated,
      compensationPaid,
      status,
    };
    try {
      if (editingId) {
        await api.put<Passenger>(`/api/passengers/${editingId}`, payload);
        cancelEdit();
      } else {
        await api.post<Passenger>("/api/passengers", payload);
        resetForm();
      }
      await loadPassengers();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save passenger.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(p: Passenger) {
    if (!window.confirm(`Remove passenger ${p.firstName} ${p.lastName} (${p.bookingReference})?`)) return;
    setError(null);
    setDeletingId(p.id);
    try {
      await api.delete(`/api/passengers/${p.id}`);
      if (editingId === p.id) cancelEdit();
      await loadPassengers();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to remove passenger.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="card">
      <h2>Passengers</h2>
      {error && <div className="error-banner">{error}</div>}
      {!canEdit && (
        <p className="cost-breakdown">
          You have read-only access to passenger records. Only Commercial Services and Admins can add or edit them.
        </p>
      )}
      {canEdit && (
        <form className="inline-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="pax-first">First name</label>
            <input id="pax-first" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
          </div>
          <div>
            <label htmlFor="pax-last">Last name</label>
            <input id="pax-last" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
          </div>
          <div>
            <label htmlFor="pax-booking">Booking ref</label>
            <input
              id="pax-booking"
              value={bookingReference}
              onChange={(e) => setBookingReference(e.target.value.toUpperCase())}
              placeholder="WB7X9K"
              required
            />
          </div>
          <div>
            <label htmlFor="pax-flight">Flight</label>
            <select id="pax-flight" value={flightId} onChange={(e) => setFlightId(e.target.value ? Number(e.target.value) : "")} required>
              <option value="">Select...</option>
              {flights.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.flightNumber} ({f.origin} → {f.destination})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="pax-seat">Seat</label>
            <input id="pax-seat" value={seatNumber} onChange={(e) => setSeatNumber(e.target.value)} placeholder="14C" />
          </div>
          <div>
            <label htmlFor="pax-class">Ticket class</label>
            <select id="pax-class" value={ticketClass} onChange={(e) => setTicketClass(e.target.value as TicketClass)}>
              <option value="ECONOMY">Economy</option>
              <option value="BUSINESS">Business</option>
              <option value="FIRST">First</option>
            </select>
          </div>
          <div>
            <label htmlFor="pax-status">Status</label>
            <select id="pax-status" value={status} onChange={(e) => setStatus(e.target.value as PassengerStatus)}>
              <option value="CHECKED_IN">Checked in</option>
              <option value="BOARDED">Boarded</option>
              <option value="DISRUPTED">Disrupted</option>
              <option value="REACCOMMODATED">Re-accommodated</option>
            </select>
          </div>
          <div>
            <label htmlFor="pax-connection-toggle">Has connection?</label>
            <select
              id="pax-connection-toggle"
              value={hasConnection ? "yes" : "no"}
              onChange={(e) => setHasConnection(e.target.value === "yes")}
            >
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </select>
          </div>
          {hasConnection && (
            <>
              <div>
                <label htmlFor="pax-conn-flight">Connection flight</label>
                <input
                  id="pax-conn-flight"
                  value={connectionFlight}
                  onChange={(e) => setConnectionFlight(e.target.value.toUpperCase())}
                  placeholder="KQ102"
                />
              </div>
              <div>
                <label htmlFor="pax-conn-deadline">Connection deadline (min)</label>
                <input
                  id="pax-conn-deadline"
                  type="number"
                  min={0}
                  value={connectionDeadlineMinutes}
                  onChange={(e) => setConnectionDeadlineMinutes(Number(e.target.value))}
                />
              </div>
            </>
          )}
          <div>
            <label htmlFor="pax-comp">Compensation paid ($)</label>
            <input
              id="pax-comp"
              type="number"
              min={0}
              step="0.01"
              value={compensationPaid}
              onChange={(e) => setCompensationPaid(Number(e.target.value))}
            />
          </div>
          <div>
            <label htmlFor="pax-reaccom">Re-accommodated?</label>
            <select id="pax-reaccom" value={reAccommodated ? "yes" : "no"} onChange={(e) => setReAccommodated(e.target.value === "yes")}>
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </select>
          </div>
          <div className="row-actions">
            <button type="submit" disabled={submitting}>
              {editingId ? "Save changes" : "Add passenger"}
            </button>
            {editingId && (
              <button type="button" className="secondary" onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
      )}

      {passengers.length === 0 ? (
        <p className="empty-state">No passenger records yet.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Booking</th>
                <th>Flight</th>
                <th>Class</th>
                <th>Connection</th>
                <th>Compensation</th>
                <th>Status</th>
                {canEdit && <th></th>}
              </tr>
            </thead>
            <tbody>
              {pageItems.map((p) => (
                <tr key={p.id}>
                  <td>
                    {p.firstName} {p.lastName}
                  </td>
                  <td>{p.bookingReference}</td>
                  <td>{p.flightNumber}</td>
                  <td>{p.ticketClass}</td>
                  <td>
                    {p.hasConnection ? `${p.connectionFlight || "—"} · ${p.connectionDeadlineMinutes} min` : "—"}
                  </td>
                  <td>${p.compensationPaid.toFixed(2)}</td>
                  <td>{p.status}</td>
                  {canEdit && (
                    <td>
                      <div className="row-actions">
                        <button className="secondary" onClick={() => startEdit(p)}>
                          Edit
                        </button>
                        <button className="danger" disabled={deletingId === p.id} onClick={() => handleDelete(p)}>
                          {deletingId === p.id ? "Removing..." : "Remove"}
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
