import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { campusApi, type StudentDashboard as Dash } from "../api";
import { useAuth } from "../auth";
import { formatWhen, prettyEnum, statusTone } from "../format";
import { AuthImage, Badge, Button, Card, DemoBanner, Empty, ErrorState, Skeleton } from "../ui";

export default function StudentDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState<Dash | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setError(null);
    campusApi.studentDashboard().then(setData).catch((e) => setError(e.message));
  };
  useEffect(load, []);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!data) return <Skeleton className="h-64" />;

  const stats = [
    ["Lost reports", data.lostReports],
    ["Found reports", data.foundReports],
    ["Potential matches", data.potentialMatches],
    ["Active claims", data.activeClaims],
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-sky-300">CampusGuard — Smart Lost and Found</p>
          <h1 className="font-display text-3xl">Welcome, {user?.name}</h1>
          <p className="text-sm text-slate-400">{user?.email} · {user?.studentId} · {user?.department}</p>
        </div>
        <div className="flex gap-2">
          <Link to="/app/report?type=LOST"><Button>Report lost item</Button></Link>
          <Link to="/app/report?type=FOUND"><Button variant="secondary">Report found item</Button></Link>
        </div>
      </div>
      <DemoBanner show={data.demoData} />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map(([label, value]) => (
          <Card key={String(label)}>
            <p className="text-sm text-slate-400">{label}</p>
            <p className="font-display text-3xl">{value}</p>
          </Card>
        ))}
      </div>
      <section>
        <h2 className="font-display text-xl">Recent items</h2>
        {data.recentItems.length === 0 ? (
          <Empty title="No reports yet" body="Start by reporting a lost or found item." />
        ) : (
          <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {data.recentItems.map((item) => (
              <Link key={item.id} to={`/app/items/${item.id}`}>
                <Card className="h-full overflow-hidden p-0">
                  <AuthImage src={item.photoUrl} alt={item.itemName} className="h-36 w-full object-cover" />
                  <div className="p-4">
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-medium">{item.itemName}</p>
                      <Badge tone={statusTone(item.status)}>{prettyEnum(item.status)}</Badge>
                    </div>
                    <p className="mt-1 text-sm text-slate-400">{item.location}</p>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </section>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <h2 className="font-display text-xl">Match notifications</h2>
          {data.recentNotifications.length === 0 ? (
            <p className="mt-2 text-sm text-slate-400">No notifications yet.</p>
          ) : (
            <ul className="mt-3 space-y-3">
              {data.recentNotifications.map((n) => (
                <li key={n.id}>
                  <p className="text-sm font-medium">{n.title}</p>
                  <p className="text-sm text-slate-400">{n.body}</p>
                  <p className="text-xs text-slate-500">{formatWhen(n.createdAt)}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card>
          <h2 className="font-display text-xl">Claim timeline</h2>
          {data.claims.length === 0 ? (
            <p className="mt-2 text-sm text-slate-400">No claims in progress.</p>
          ) : (
            <ol className="mt-3 space-y-3">
              {data.claims.map((c) => (
                <li key={c.id}>
                  <Link to={`/app/claims/${c.id}`} className="flex items-center justify-between">
                    <span>Claim #{c.id}</span>
                    <Badge tone={statusTone(c.status)}>{prettyEnum(c.status)}</Badge>
                  </Link>
                  <p className="text-xs text-slate-500">{c.genericOutcome}</p>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </div>
  );
}
