import type { Role } from "../types";

/**
 * Mirrors the write-access matrix enforced server-side in SecurityConfig. This is
 * purely a UX layer (hide actions a user can't perform) - the backend is the real
 * authority and re-checks every request regardless of what the UI shows.
 */
export type Domain = "aircraft" | "crew" | "flights" | "delayEvents" | "recovery" | "passengers" | "users";

const DOMAIN_OWNERS: Record<Domain, Role[]> = {
  aircraft: ["ADMIN", "MAINTENANCE_CONTROLLER"],
  crew: ["ADMIN", "CREW_SCHEDULER"],
  flights: ["ADMIN", "OPERATIONS_CONTROLLER"],
  delayEvents: ["ADMIN", "OPERATIONS_CONTROLLER"],
  recovery: ["ADMIN", "OPERATIONS_CONTROLLER"],
  passengers: ["ADMIN", "COMMERCIAL_SERVICES"],
  users: ["ADMIN"],
};

export function canManage(role: Role | undefined, domain: Domain): boolean {
  if (!role) return false;
  return DOMAIN_OWNERS[domain].includes(role);
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: "Administrator",
  OPERATIONS_CONTROLLER: "Operations Controller / Dispatcher",
  CREW_SCHEDULER: "Crew Scheduler",
  MAINTENANCE_CONTROLLER: "Maintenance Controller",
  COMMERCIAL_SERVICES: "Commercial / Passenger Services",
  SENIOR_MANAGEMENT: "Senior Management",
};
