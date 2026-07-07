import { useEffect, useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Crew, CrewRole, CrewStatus } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "./Pagination";
import { sortByIdDesc } from "../utils/sort";
import { useAuth } from "../auth/AuthContext";
import { canManage } from "../auth/permissions";

const CREW_ROLES: CrewRole[] = ["CAPTAIN", "FIRST_OFFICER", "SENIOR_FA", "FLIGHT_ATTENDANT"];
const CREW_STATUSES: CrewStatus[] = ["AVAILABLE", "ON_DUTY", "ON_REST", "ON_LEAVE", "SICK"];
const PAGE_SIZE = 5;

const DEFAULTS = {
  firstName: "",
  lastName: "",
  employeeId: "",
  role: "CAPTAIN" as CrewRole,
  qualifications: "B737,A330",
  dutyHoursUsed: 0,
  maxDutyHours: 14,
  currentLocation: "KGL",
  status: "AVAILABLE" as CrewStatus,
  overtimeRate: 90,
};

export function CrewManager() {
  const { user } = useAuth();
  const canEdit = canManage(user?.role, "crew");
  const [crew, setCrew] = useState<Crew[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(crew, PAGE_SIZE);

  const [firstName, setFirstName] = useState(DEFAULTS.firstName);
  const [lastName, setLastName] = useState(DEFAULTS.lastName);
  const [employeeId, setEmployeeId] = useState(DEFAULTS.employeeId);
  const [role, setRole] = useState<CrewRole>(DEFAULTS.role);
  const [qualifications, setQualifications] = useState(DEFAULTS.qualifications);
  const [dutyHoursUsed, setDutyHoursUsed] = useState(DEFAULTS.dutyHoursUsed);
  const [maxDutyHours, setMaxDutyHours] = useState(DEFAULTS.maxDutyHours);
  const [currentLocation, setCurrentLocation] = useState(DEFAULTS.currentLocation);
  const [status, setStatus] = useState<CrewStatus>(DEFAULTS.status);
  const [overtimeRate, setOvertimeRate] = useState(DEFAULTS.overtimeRate);

  async function loadCrew() {
    try {
      setCrew(sortByIdDesc(await api.get<Crew[]>("/api/crew")));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load crew.");
    }
  }

  useEffect(() => {
    loadCrew();
  }, []);

  function resetForm() {
    setFirstName(DEFAULTS.firstName);
    setLastName(DEFAULTS.lastName);
    setEmployeeId(DEFAULTS.employeeId);
    setRole(DEFAULTS.role);
    setQualifications(DEFAULTS.qualifications);
    setDutyHoursUsed(DEFAULTS.dutyHoursUsed);
    setMaxDutyHours(DEFAULTS.maxDutyHours);
    setCurrentLocation(DEFAULTS.currentLocation);
    setStatus(DEFAULTS.status);
    setOvertimeRate(DEFAULTS.overtimeRate);
  }

  function startEdit(c: Crew) {
    setEditingId(c.id);
    setFirstName(c.firstName);
    setLastName(c.lastName);
    setEmployeeId(c.employeeId);
    setRole(c.role);
    setQualifications(c.qualifications);
    setDutyHoursUsed(c.dutyHoursUsed);
    setMaxDutyHours(c.maxDutyHours);
    setCurrentLocation(c.currentLocation || DEFAULTS.currentLocation);
    setStatus(c.status);
    setOvertimeRate(c.overtimeRate);
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
    const payload = {
      firstName,
      lastName,
      employeeId,
      role,
      qualifications,
      dutyHoursUsed,
      maxDutyHours,
      currentLocation,
      status,
      overtimeRate,
    };
    try {
      if (editingId) {
        await api.put<Crew>(`/api/crew/${editingId}`, payload);
        cancelEdit();
      } else {
        await api.post<Crew>("/api/crew", payload);
        resetForm();
      }
      await loadCrew();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save crew member.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(c: Crew) {
    if (!window.confirm(`Delete crew member ${c.firstName} ${c.lastName}? This cannot be undone.`)) return;
    setError(null);
    setDeletingId(c.id);
    try {
      await api.delete(`/api/crew/${c.id}`);
      if (editingId === c.id) cancelEdit();
      await loadCrew();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to delete crew member.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="card">
      <h2>Crew</h2>
      {error && <div className="error-banner">{error}</div>}
      {!canEdit && (
        <p className="cost-breakdown">
          You have read-only access to crew data. Only Crew Schedulers and Admins can add or edit crew members.
        </p>
      )}
      {canEdit && (
      <form className="inline-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="cr-first">First name</label>
          <input id="cr-first" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="cr-last">Last name</label>
          <input id="cr-last" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="cr-emp">Employee ID</label>
          <input id="cr-emp" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="cr-role">Role</label>
          <select id="cr-role" value={role} onChange={(e) => setRole(e.target.value as CrewRole)}>
            {CREW_ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="cr-qual">Qualifications</label>
          <input id="cr-qual" value={qualifications} onChange={(e) => setQualifications(e.target.value)} />
        </div>
        <div>
          <label htmlFor="cr-location">Base location (IATA code)</label>
          <input
            id="cr-location"
            value={currentLocation}
            onChange={(e) => setCurrentLocation(e.target.value.toUpperCase())}
            maxLength={3}
            pattern="[A-Z]{3}"
            title="3-letter IATA airport code, e.g. KGL"
            placeholder="KGL"
          />
        </div>
        <div>
          <label htmlFor="cr-duty-used">Duty hrs used</label>
          <input
            id="cr-duty-used"
            type="number"
            step="0.5"
            value={dutyHoursUsed}
            onChange={(e) => setDutyHoursUsed(Number(e.target.value))}
          />
        </div>
        <div>
          <label htmlFor="cr-duty-max">Max duty hrs</label>
          <input
            id="cr-duty-max"
            type="number"
            step="0.5"
            value={maxDutyHours}
            onChange={(e) => setMaxDutyHours(Number(e.target.value))}
          />
        </div>
        <div>
          <label htmlFor="cr-status">Status</label>
          <select id="cr-status" value={status} onChange={(e) => setStatus(e.target.value as CrewStatus)}>
            {CREW_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="cr-ot">Overtime rate ($/h)</label>
          <input
            id="cr-ot"
            type="number"
            value={overtimeRate}
            onChange={(e) => setOvertimeRate(Number(e.target.value))}
          />
        </div>
        <div className="row-actions">
          <button type="submit" disabled={submitting}>
            {editingId ? "Save changes" : "Add crew member"}
          </button>
          {editingId && (
            <button type="button" className="secondary" onClick={cancelEdit}>
              Cancel
            </button>
          )}
        </div>
      </form>
      )}

      {crew.length === 0 ? (
        <p className="empty-state">No crew recorded yet.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Role</th>
                <th>Qualifications</th>
                <th>Duty hours</th>
                <th>Status</th>
                {canEdit && <th></th>}
              </tr>
            </thead>
            <tbody>
              {pageItems.map((c) => (
                <tr key={c.id}>
                  <td>
                    {c.firstName} {c.lastName} <span className="cost-breakdown">({c.employeeId})</span>
                  </td>
                  <td>{c.role}</td>
                  <td>{c.qualifications}</td>
                  <td>
                    {c.dutyHoursUsed} / {c.maxDutyHours}
                  </td>
                  <td>{c.status}</td>
                  {canEdit && (
                    <td>
                      <div className="row-actions">
                        <button className="secondary" onClick={() => startEdit(c)}>
                          Edit
                        </button>
                        <button className="danger" disabled={deletingId === c.id} onClick={() => handleDelete(c)}>
                          {deletingId === c.id ? "Deleting..." : "Delete"}
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
