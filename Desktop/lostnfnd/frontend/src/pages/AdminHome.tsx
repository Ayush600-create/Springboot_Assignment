import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { campusApi, type AdminDashboard, type ClaimView } from "../api";
import { formatWhen, prettyEnum, statusTone } from "../format";
import { Badge, Card, DemoBanner, Empty, ErrorState, Skeleton } from "../ui";

export default function AdminHome({ queue = false }: { queue?: boolean }) {
  const [dash, setDash] = useState<AdminDashboard | null>(null);
  const [claims, setClaims] = useState<ClaimView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setError(null);
    Promise.all([campusApi.adminDashboard(), campusApi.adminClaims()])
      .then(([d, c]) => { setDash(d); setClaims(c); })
      .catch((e) => setError(e.message));
  };
  useEffect(load, []);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!dash || !claims) return <Skeleton className="h-64" />;

  const stats: Array<[string, number]> = [
    ["Students", dash.totalStudents],
    ["Lost reports", dash.lostReports],
    ["Found reports", dash.foundReports],
    ["Unresolved", dash.unresolvedReports],
    ["Matches", dash.potentialMatches],
    ["Pending claims", dash.pendingClaims],
    ["Approved claims", dash.approvedClaims],
    ["Completed handovers", dash.completedHandovers],
  ];

  return (
    <div className="space-y-6">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Admin</p>
        <h1 className="font-display text-3xl">{queue ? "Claim review queue" : "CampusGuard operations"}</h1>
      </div>
      <DemoBanner show={dash.demoData} />
      {!queue && (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {stats.map(([label, value]) => (
              <Card key={label}>
                <p className="text-sm text-slate-400">{label}</p>
                <p className="font-display text-3xl">{value}</p>
                <div className="factor-bar mt-3">
                  <span style={{ width: `${Math.min(100, Number(value) * 8)}%` }} />
                </div>
              </Card>
            ))}
          </div>
          <Card>
            <h2 className="font-display text-lg">Recent activity</h2>
            {dash.recentActivity.length === 0 ? <p className="mt-2 text-sm text-slate-400">No audit events yet.</p> : (
              <ul className="mt-3 space-y-2 text-sm">
                {dash.recentActivity.map((a) => (
                  <li key={a.id} className="flex justify-between gap-3">
                    <span>{a.action} · {a.entityType} #{a.entityId}</span>
                    <span className="text-slate-500">{formatWhen(a.createdAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
      <section>
        <h2 className="font-display text-xl">Claims</h2>
        {claims.length === 0 ? <Empty title="No claims" body="Submitted claims will appear here for review." /> : (
          <div className="mt-3 space-y-3">
            {claims.map((c) => (
              <Link key={c.id} to={`/app/admin/claims/${c.id}`}>
                <Card className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">Claim #{c.id}</p>
                    <p className="text-sm text-slate-400">{c.genericOutcome}</p>
                  </div>
                  <div className="flex gap-2">
                    <Badge tone={statusTone(c.status)}>{prettyEnum(c.status)}</Badge>
                    <Badge tone="slate">{prettyEnum(c.reviewStatus)}</Badge>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
