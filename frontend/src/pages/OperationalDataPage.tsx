import { useEffect, useState } from "react";
import { api, ApiRequestError } from "../api/client";
import type { Aircraft } from "../types";
import { sortByIdDesc } from "../utils/sort";
import { AircraftManager } from "../components/AircraftManager";
import { CrewManager } from "../components/CrewManager";
import { FlightManager } from "../components/FlightManager";
import { PassengerManager } from "../components/PassengerManager";

export function OperationalDataPage() {
  const [aircraft, setAircraft] = useState<Aircraft[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function loadAircraft() {
    try {
      setAircraft(sortByIdDesc(await api.get<Aircraft[]>("/api/aircraft")));
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : "Failed to load aircraft.");
    }
  }

  useEffect(() => {
    loadAircraft();
  }, []);

  return (
    <div className="page-photo-page page-photo-page--sky">
      <div className="page-title-chip">
        <h1>Operational Data</h1>
        <p>Manage the fleet, crew roster, flight schedule, and passenger records that feed every recovery decision.</p>
      </div>

      {error && <div className="error-banner">{error}</div>}
      <AircraftManager aircraft={aircraft} onChanged={loadAircraft} />
      <CrewManager />
      <FlightManager aircraftOptions={aircraft} />
      <PassengerManager />
    </div>
  );
}
