import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiRequestError } from "../api/client";
import type { AnalyticsSummary } from "../types";

function formatCurrency(value: number) {
  return `$${value.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
}

const CATEGORY_ORDER = ["TECHNICAL", "WEATHER", "ATC", "CREW", "COMMERCIAL", "REACTIONARY", "OTHER"];

function orderedCategories(...maps: Record<string, number>[]) {
  const keys = new Set<string>();
  maps.forEach((m) => Object.keys(m).forEach((k) => keys.add(k)));
  return CATEGORY_ORDER.filter((c) => keys.has(c)).concat([...keys].filter((k) => !CATEGORY_ORDER.includes(k)));
}

const PIE_COLORS_PRIMARY = ["#4f46e5", "#0891b2", "#d97706", "#16a34a", "#dc2626", "#7c3aed", "#db2777"];
const PIE_COLORS_SECONDARY = ["#ea580c", "#059669", "#e11d48", "#0284c7", "#9333ea", "#65a30d", "#be185d"];

function PieChart({
  data,
  formatValue,
  colors = PIE_COLORS_PRIMARY,
}: {
  data: { label: string; value: number }[];
  formatValue: (v: number) => string;
  colors?: string[];
}) {
  const total = data.reduce((sum, d) => sum + d.value, 0);
  let cumulativePct = 0;
  const segments = data.map((d, i) => {
    const pct = total > 0 ? (d.value / total) * 100 : 0;
    const start = cumulativePct;
    cumulativePct += pct;
    return { ...d, color: colors[i % colors.length], pct, start, end: cumulativePct };
  });
  const gradient =
    total > 0
      ? `conic-gradient(${segments.map((s) => `${s.color} ${s.start}% ${s.end}%`).join(", ")})`
      : "var(--panel-alt)";

  return (
    <div className="pie-chart-wrap">
      <div className="pie-chart" style={{ background: gradient }} />
      <div className="pie-chart-legend">
        {segments.map((s) => (
          <div key={s.label} className="pie-chart-legend-item">
            <span className="pie-chart-swatch" style={{ background: s.color }} />
            <span className="pie-chart-legend-label">{s.label}</span>
            <strong>{formatValue(s.value)}</strong>
            <span className="pie-chart-pct">{s.pct.toFixed(0)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ColumnChart({
  data,
  formatValue,
  colors = PIE_COLORS_PRIMARY,
}: {
  data: { label: string; value: number }[];
  formatValue: (v: number) => string;
  colors?: string[];
}) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div className="column-chart">
      {data.map((d, i) => {
        const heightPct = Math.max((d.value / max) * 100, d.value > 0 ? 4 : 0);
        const color = colors[i % colors.length];
        return (
          <div key={d.label} className="column-chart-col">
            <span className="column-chart-value">{formatValue(d.value)}</span>
            <div className="column-chart-track">
              <div className="column-chart-bar" style={{ height: `${heightPct}%`, background: color }} />
            </div>
            <span className="column-chart-label">{d.label}</span>
          </div>
        );
      })}
    </div>
  );
}

export function AnalyticsPage() {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        setSummary(await api.get<AnalyticsSummary>("/api/analytics/summary"));
      } catch (err) {
        setError(err instanceof ApiRequestError ? err.message : "Failed to load analytics.");
      }
    }
    load();
  }, []);

  return (
    <div className="page-photo-page page-photo-page--sky">
      <div className="page-title-chip">
        <h1>Recovery Analytics</h1>
        <p>Delay frequency, cost drivers, decision speed, and DSS performance against the manual baseline.</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {!summary ? (
        !error && <p className="empty-state">Loading analytics...</p>
      ) : (
        <>
          <div className="analytics-kpi-grid">
            <Link to="/delay-events" className="analytics-kpi-card analytics-kpi-card--blue">
              <strong>{summary.totalDelayEvents}</strong>
              <span>Delay events recorded</span>
            </Link>
            <Link to="/history?status=RESOLVED" className="analytics-kpi-card analytics-kpi-card--green">
              <strong>{summary.totalResolvedScenarios}</strong>
              <span>Recovery decisions resolved</span>
            </Link>
            <Link to="/history?status=IN_PROGRESS" className="analytics-kpi-card analytics-kpi-card--gold">
              <strong>{summary.totalPendingScenarios}</strong>
              <span>Pending a decision</span>
            </Link>
            <Link to="/history?status=RESOLVED" className="analytics-kpi-card analytics-kpi-card--green">
              <strong>{formatCurrency(summary.totalCostSaved)}</strong>
              <span>Cost saved vs estimate (all recorded outcomes)</span>
            </Link>
            <Link to="/history?status=RESOLVED" className="analytics-kpi-card analytics-kpi-card--blue">
              <strong>{summary.avgDecisionMinutes !== null ? `${summary.avgDecisionMinutes.toFixed(1)} min` : "—"}</strong>
              <span>Average time from ranked options to decision</span>
            </Link>
            <Link to="/history" className="analytics-kpi-card analytics-kpi-card--gold">
              <strong>{summary.avgComputationTimeMs !== null ? `${summary.avgComputationTimeMs.toFixed(0)} ms` : "—"}</strong>
              <span>Average MILP/RL solve time</span>
            </Link>
          </div>

          <div className="card">
            <h2>Delay frequency &amp; average duration by cause</h2>
            {orderedCategories(summary.delayCountByCategory, summary.avgDelayMinutesByCategory).length === 0 ? (
              <p className="empty-state">No delay events recorded yet.</p>
            ) : (
              <>
                <h3 className="analytics-subheading">Number of delay events</h3>
                <PieChart
                  data={orderedCategories(summary.delayCountByCategory).map((cat) => ({
                    label: cat,
                    value: summary.delayCountByCategory[cat] ?? 0,
                  }))}
                  formatValue={(v) => `${v}`}
                />
                <h3 className="analytics-subheading">Average delay duration (minutes)</h3>
                <ColumnChart
                  data={orderedCategories(summary.avgDelayMinutesByCategory).map((cat) => ({
                    label: cat,
                    value: summary.avgDelayMinutesByCategory[cat] ?? 0,
                  }))}
                  formatValue={(v) => `${v.toFixed(0)} min`}
                />
              </>
            )}
          </div>

          <div className="card">
            <h2>Recovery cost by delay cause</h2>
            <p className="cost-breakdown">Actual recorded cost, summed across resolved scenarios with an outcome logged.</p>
            {orderedCategories(summary.costByCategory).length === 0 ? (
              <p className="empty-state">No outcomes recorded yet, so no cost breakdown is available.</p>
            ) : (
              <PieChart
                data={orderedCategories(summary.costByCategory).map((cat) => ({
                  label: cat,
                  value: summary.costByCategory[cat] ?? 0,
                }))}
                formatValue={formatCurrency}
                colors={PIE_COLORS_SECONDARY}
              />
            )}
          </div>

          <div className="card">
            <h2>DSS vs. manual decision-making baseline</h2>
            {summary.scenariosWithManualBaseline === 0 ? (
              <p className="empty-state">
                No manual-baseline estimates have been logged yet. When recording a recovery outcome, dispatchers can
                optionally enter what the recovery would have cost under the prior manual process, and the comparison
                will appear here.
              </p>
            ) : (
              <div className="analytics-comparison">
                <div>
                  <strong>{formatCurrency(summary.manualBaselineTotal ?? 0)}</strong>
                  <span>Estimated manual-approach cost ({summary.scenariosWithManualBaseline} scenario(s))</span>
                </div>
                <div>
                  <strong>{formatCurrency(summary.actualCostForBaselineComparison ?? 0)}</strong>
                  <span>Actual DSS-recommended cost, same {summary.scenariosWithManualBaseline} scenario(s)</span>
                </div>
                <div className="analytics-comparison-highlight">
                  <strong>
                    {summary.manualVsDssSavingsPercent !== null ? `${summary.manualVsDssSavingsPercent.toFixed(1)}%` : "—"}
                  </strong>
                  <span>Cost reduction vs. the manual baseline</span>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
