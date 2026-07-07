import { useEffect, useState, type FormEvent } from "react";
import { api, ApiRequestError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { ROLE_LABELS } from "../auth/permissions";
import type { Role } from "../types";

interface OwnProfile {
  username: string;
  fullName: string;
  role: Role;
}

export function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const [profile, setProfile] = useState<OwnProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [fullName, setFullName] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    api
      .get<OwnProfile>("/api/auth/me")
      .then((p) => {
        setProfile(p);
        setFullName(p.fullName || "");
      })
      .catch((err) => setError(err instanceof ApiRequestError ? err.message : "Failed to load profile."))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword && newPassword !== confirmPassword) {
      setError("New password and confirmation do not match.");
      return;
    }

    setSaving(true);
    try {
      const updated = await api.put<OwnProfile>("/api/auth/me", {
        fullName,
        currentPassword: newPassword ? currentPassword : undefined,
        newPassword: newPassword || undefined,
      });
      setProfile(updated);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccess(newPassword ? "Profile updated and password changed." : "Profile updated.");
      refreshUser(updated.fullName);
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to update profile.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <p className="empty-state">Loading...</p>;
  }

  return (
    <div>
      <div className="card">
        <h2>My Profile</h2>
        {error && <div className="error-banner">{error}</div>}
        {success && (
          <p className="cost-breakdown" style={{ color: "var(--success)", fontWeight: 600 }}>
            ✓ {success}
          </p>
        )}
        <form className="inline-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="p-username">Username</label>
            <input id="p-username" value={profile?.username ?? ""} disabled />
          </div>
          <div>
            <label htmlFor="p-role">Role</label>
            <input id="p-role" value={user ? ROLE_LABELS[user.role] : ""} disabled />
          </div>
          <div>
            <label htmlFor="p-fullname">Full name</label>
            <input id="p-fullname" value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </div>
          <div className="row-actions">
            <button type="submit" disabled={saving}>
              {saving ? "Saving..." : "Save changes"}
            </button>
          </div>
        </form>
      </div>

      <div className="card">
        <h2>Change Password</h2>
        <p className="cost-breakdown">Leave these blank if you don't want to change your password.</p>
        <form className="inline-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="p-current">Current password</label>
            <input
              id="p-current"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="p-new">New password</label>
            <input id="p-new" type="password" minLength={8} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
          </div>
          <div>
            <label htmlFor="p-confirm">Confirm new password</label>
            <input
              id="p-confirm"
              type="password"
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>
          <div className="row-actions">
            <button type="submit" disabled={saving}>
              {saving ? "Saving..." : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
