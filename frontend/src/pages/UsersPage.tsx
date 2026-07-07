import { useEffect, useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Role } from "../types";
import { usePagination } from "../hooks/usePagination";
import { Pagination } from "../components/Pagination";
import { sortByIdDesc } from "../utils/sort";
import { ROLE_LABELS } from "../auth/permissions";

const PAGE_SIZE = 5;

const ALL_ROLES: Role[] = [
  "OPERATIONS_CONTROLLER",
  "CREW_SCHEDULER",
  "MAINTENANCE_CONTROLLER",
  "COMMERCIAL_SERVICES",
  "SENIOR_MANAGEMENT",
  "ADMIN",
];

interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  role: Role;
  enabled: boolean;
}

const DEFAULTS = {
  username: "",
  password: "",
  fullName: "",
  role: "OPERATIONS_CONTROLLER" as Role,
  enabled: true,
};

export function UsersPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { page, setPage, totalPages, pageItems } = usePagination(users, PAGE_SIZE);

  const [username, setUsername] = useState(DEFAULTS.username);
  const [password, setPassword] = useState(DEFAULTS.password);
  const [fullName, setFullName] = useState(DEFAULTS.fullName);
  const [role, setRole] = useState<Role>(DEFAULTS.role);
  const [enabled, setEnabled] = useState(DEFAULTS.enabled);

  async function loadUsers() {
    try {
      setUsers(sortByIdDesc(await api.get<UserSummary[]>("/api/auth/users")));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load users.");
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  function resetForm() {
    setUsername(DEFAULTS.username);
    setPassword(DEFAULTS.password);
    setFullName(DEFAULTS.fullName);
    setRole(DEFAULTS.role);
    setEnabled(DEFAULTS.enabled);
  }

  function startEdit(u: UserSummary) {
    setEditingId(u.id);
    setUsername(u.username);
    setPassword("");
    setFullName(u.fullName || "");
    setRole(u.role);
    setEnabled(u.enabled);
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
    try {
      if (editingId) {
        await api.put(`/api/auth/users/${editingId}`, { fullName, role, enabled, password: password || undefined });
        cancelEdit();
      } else {
        await api.post("/api/auth/register", { username, password, fullName, role });
        resetForm();
      }
      await loadUsers();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to save user.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(u: UserSummary) {
    if (!window.confirm(`Delete user account "${u.username}"? This cannot be undone.`)) return;
    setError(null);
    setDeletingId(u.id);
    try {
      await api.delete(`/api/auth/users/${u.id}`);
      if (editingId === u.id) cancelEdit();
      await loadUsers();
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to delete user.");
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="page-photo-page page-photo-page--sky">
      <div className="page-title-chip">
        <h1>OCC Staff Accounts</h1>
        <p>Only administrators can create accounts or change roles - this is where permissions are granted.</p>
      </div>

      <div className="card">
      <h2>OCC Staff Accounts</h2>
      {error && <div className="error-banner">{error}</div>}
      <form className="inline-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="u-username">Username</label>
          <input
            id="u-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={!!editingId}
            required={!editingId}
          />
        </div>
        <div>
          <label htmlFor="u-password">{editingId ? "New password (leave blank to keep current)" : "Password"}</label>
          <input
            id="u-password"
            type="password"
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required={!editingId}
          />
        </div>
        <div>
          <label htmlFor="u-fullname">Full name</label>
          <input id="u-fullname" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </div>
        <div>
          <label htmlFor="u-role">Role</label>
          <select id="u-role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {ALL_ROLES.map((r) => (
              <option key={r} value={r}>
                {ROLE_LABELS[r]}
              </option>
            ))}
          </select>
        </div>
        {editingId && (
          <div>
            <label htmlFor="u-enabled">Account status</label>
            <select id="u-enabled" value={enabled ? "true" : "false"} onChange={(e) => setEnabled(e.target.value === "true")}>
              <option value="true">Enabled</option>
              <option value="false">Disabled</option>
            </select>
          </div>
        )}
        <div className="row-actions">
          <button type="submit" disabled={submitting}>
            {editingId ? "Save changes" : "Create account"}
          </button>
          {editingId && (
            <button type="button" className="secondary" onClick={cancelEdit}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {users.length === 0 ? (
        <p className="empty-state">No users yet.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Full name</th>
                <th>Role</th>
                <th>Enabled</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.fullName}</td>
                  <td>{ROLE_LABELS[u.role]}</td>
                  <td>{u.enabled ? "Yes" : "No"}</td>
                  <td>
                    <div className="row-actions">
                      <button className="secondary" onClick={() => startEdit(u)}>
                        Edit
                      </button>
                      <button className="danger" disabled={deletingId === u.id} onClick={() => handleDelete(u)}>
                        {deletingId === u.id ? "Deleting..." : "Delete"}
                      </button>
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
