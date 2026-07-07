import { useEffect, useState } from "react";
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

function BarRow({ label, value, max, formatValue }: { label: string; value: number; max: number; formatValue: (v: number) => string }) {
  const pct = max > 0 ? Math.max((value / max) * 100, value > 0 ? 3 : 0) : 0;
  return (
    <div className="analytics-bar-row">
      <span className="analytics-bar-label">{label}</span>
      <div className="analytics-bar-track">
        <div className="analytics-bar-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="analytics-bar-value">{formatValue(value)}</span>
    </div>
  );
}

const PIE_COLORS = ["#4f46e5", "#0891b2", "#d97706", "#dc2626", "#16a34a", "#7c3aed", "#db2777"];

function PieChart({
  data,
  formatValue,
}: {
  data: { label: string; value: number }[];
  formatValue: (v: number) => string;
}) {
  const total = data.reduce((sum, d) => sum + d.value, 0);
  let cumulativePct = 0;
  const segments = data.map((d, i) => {
    const pct = total > 0 ? (d.value / total) * 100 : 0;
    const start = cumulativePct;
    cumulativePct += pct;
    return { ...d, color: PIE_COLORS[i % PIE_COLORS.length], pct, start, end: cumulativePct };
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
            <div className="analytics-kpi-card">
              <strong>{summary.totalDelayEvents}</strong>
              <span>Delay events recorded</span>
            </div>
            <div className="analytics-kpi-card">
              <strong>{summary.totalResolvedScenarios}</strong>
              <span>Recovery decisions resolved</span>
            </div>
            <div className="analytics-kpi-card">
              <strong>{summary.totalPendingScenarios}</strong>
              <span>Pending a decision</span>
            </div>
            <div className="analytics-kpi-card">
              <strong>{formatCurrency(summary.totalCostSaved)}</strong>
              <span>Cost saved vs estimate (all recorded outcomes)</span>
            </div>
            <div className="analytics-kpi-card">
              <strong>{summary.avgDecisionMinutes !== null ? `${summary.avgDecisionMinutes.toFixed(1)} min` : "—"}</strong>
              <span>Average time from ranked options to decision</span>
            </div>
            <div className="analytics-kpi-card">
              <strong>{summary.avgComputationTimeMs !== null ? `${summary.avgComputationTimeMs.toFixed(0)} ms` : "—"}</strong>
              <span>Average MILP/RL solve time</span>
            </div>
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
                {(() => {
                  const max = Math.max(1, ...Object.values(summary.avgDelayMinutesByCategory));
                  return orderedCategories(summary.avgDelayMinutesByCategory).map((cat) => (
                    <BarRow
                      key={cat}
                      label={cat}
                      value={summary.avgDelayMinutesByCategory[cat] ?? 0}
                      max={max}
                      formatValue={(v) => `${v.toFixed(0)} min`}
                    />
                  ));
                })()}
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
