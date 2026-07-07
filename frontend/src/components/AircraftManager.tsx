import { useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Aircraft, AircraftStatus, AircraftType } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "./Pagination";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";

const AIRCRAFT_TYPES: AircraftType[] = ["BOEING_737", "AIRBUS_A330", "TURBOPROP_Q400"];
const AIRCRAFT_STATUSES: AircraftStatus[] = ["SERVICEABLE", "UNSERVICEABLE", "IN_MAINTENANCE"];
const PAGE_SIZE = 5;

const DEFAULTS = {
  registrationNumber: "",
  aircraftType: "BOEING_737" as AircraftType,
  status: "SERVICEABLE" as AircraftStatus,
  currentLocation: "KGL",
  totalSeats: 150,
  yearManufactured: 2018,
};

interface AircraftManagerProps {
  aircraft: Aircraft[];
  onChanged: () => void;
}

export function AircraftManager({ aircraft, onChanged }: AircraftManagerProps) {
  const { user } = useAuth();
  const canEdit = canManage(user?.role, "aircraft");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(aircraft, PAGE_SIZE);

  const [registrationNumber, setRegistrationNumber] = useState(DEFAULTS.registrationNumber);
  const [aircraftType, setAircraftType] = useState<AircraftType>(DEFAULTS.aircraftType);
  const [status, setStatus] = useState<AircraftStatus>(DEFAULTS.status);
  const [currentLocation, setCurrentLocation] = useState(DEFAULTS.currentLocation);
  const [totalSeats, setTotalSeats] = useState(DEFAULTS.totalSeats);
  const [yearManufactured, setYearManufactured] = useState(DEFAULTS.yearManufactured);

  function resetForm() {
    setRegistrationNumber(DEFAULTS.registrationNumber);
    setAircraftType(DEFAULTS.aircraftType);
    setStatus(DEFAULTS.status);
    setCurrentLocation(DEFAULTS.currentLocation);
    setTotalSeats(DEFAULTS.totalSeats);
    setYearManufactured(DEFAULTS.yearManufactured);
  }

  function startEdit(a: Aircraft) {
    setEditingId(a.id);
    setRegistrationNumber(a.registrationNumber);
    setAircraftType(a.aircraftType);
    setStatus(a.status);
    setCurrentLocation(a.currentLocation);
    setTotalSeats(a.totalSeats);
    setYearManufactured(a.yearManufactured);
    setError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    resetForm();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    const payload = { registrationNumber, aircraftType, status, currentLocation, totalSeats, yearManufactured };
    try {
      if (editingId) {
        await api.put<Aircraft>(`/api/aircraft/${editingId}`, payload);
        cancelEdit();
      } else {
        await api.post<Aircraft>("/api/aircraft", payload);
        setRegistrationNumber(DEFAULTS.registrationNumber);
      }
      onChanged();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save aircraft.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(a: Aircraft) {
    if (!window.confirm(`Delete aircraft ${a.registrationNumber}? This cannot be undone.`)) return;
    setError(null);
    setDeletingId(a.id);
    try {
      await api.delete(`/api/aircraft/${a.id}`);
      if (editingId === a.id) cancelEdit();
      onChanged();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to delete aircraft.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="card">
      <h2>Aircraft</h2>
      {error && <div className="error-banner">{error}</div>}
      {!canEdit && (
        <p className="cost-breakdown">
          You have read-only access to aircraft data. Only Maintenance Controllers and Admins can add or edit aircraft.
        </p>
      )}
      {canEdit && (
      <form className="inline-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="reg">Registration</label>
          <input
            id="reg"
            placeholder="9XR-WA1"
            value={registrationNumber}
            onChange={(e) => setRegistrationNumber(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="ac-type">Type</label>
          <select id="ac-type" value={aircraftType} onChange={(e) => setAircraftType(e.target.value as AircraftType)}>
            {AIRCRAFT_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="ac-status">Status</label>
          <select id="ac-status" value={status} onChange={(e) => setStatus(e.target.value as AircraftStatus)}>
            {AIRCRAFT_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="ac-location">Location (IATA code)</label>
          <input
            id="ac-location"
            value={currentLocation}
            onChange={(e) => setCurrentLocation(e.target.value.toUpperCase())}
            maxLength={3}
            pattern="[A-Z]{3}"
            title="3-letter IATA airport code, e.g. KGL"
            placeholder="KGL"
            required
          />
        </div>
        <div>
          <label htmlFor="ac-seats">Seats</label>
          <input
            id="ac-seats"
            type="number"
            min={1}
            value={totalSeats}
            onChange={(e) => setTotalSeats(Number(e.target.value))}
          />
        </div>
        <div>
          <label htmlFor="ac-year">Year built</label>
          <input
            id="ac-year"
            type="number"
            value={yearManufactured}
            onChange={(e) => setYearManufactured(Number(e.target.value))}
          />
        </div>
        <div className="row-actions">
          <button type="submit" disabled={submitting}>
            {editingId ? "Save changes" : "Add aircraft"}
          </button>
          {editingId && (
            <button type="button" className="secondary" onClick={cancelEdit}>
              Cancel
            </button>
          )}
        </div>
      </form>
      )}

      {aircraft.length === 0 ? (
        <p className="empty-state">No aircraft recorded yet.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Registration</th>
                <th>Type</th>
                <th>Status</th>
                <th>Location</th>
                <th>Seats</th>
                {canEdit && <th></th>}
              </tr>
            </thead>
            <tbody>
              {pageItems.map((a) => (
                <tr key={a.id}>
                  <td>{a.registrationNumber}</td>
                  <td>{a.aircraftType}</td>
                  <td>{a.status}</td>
                  <td>{a.currentLocation}</td>
                  <td>{a.totalSeats}</td>
                  {canEdit && (
                    <td>
                      <div className="row-actions">
                        <button className="secondary" onClick={() => startEdit(a)}>
                          Edit
                        </button>
                        <button className="danger" disabled={deletingId === a.id} onClick={() => handleDelete(a)}>
                          {deletingId === a.id ? "Deleting..." : "Delete"}
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
