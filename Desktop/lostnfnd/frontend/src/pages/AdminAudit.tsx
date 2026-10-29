import { useEffect, useMemo, useState } from "react";
import { campusApi, type AuditView, type ReportCard, type ReportStatus } from "../api";
import { formatWhen, prettyEnum } from "../format";
import { Button, Card, ErrorState, Skeleton, inputClass } from "../ui";

export default function AdminAudit() {
  const [logs, setLogs] = useState<AuditView[] | null>(null);
  const [reports, setReports] = useState<ReportCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");

  const load = () => {
    setError(null);
    Promise.all([
      campusApi.audit(),
      campusApi.reports(new URLSearchParams({ size: "50", sort: "newest" })),
    ]).then(([a, r]) => { setLogs(a); setReports(r.content); })
      .catch((e) => setError(e.message));
  };
  useEffect(load, []);

  const filtered = useMemo(
    () => (logs ?? []).filter((l) => `${l.action} ${l.actorEmail} ${l.details}`.toLowerCase().includes(q.toLowerCase())),
    [logs, q],
  );

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!logs || !reports) return <Skeleton className="h-40" />;

  return (
    <div className="space-y-6">
      <h1 className="font-display text-3xl">Reports and audit</h1>
      <section className="space-y-3">
        <h2 className="font-display text-xl">Report moderation</h2>
        {reports.map((r) => (
          <Card key={r.id} className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-medium">{r.itemName}</p>
              <p className="text-sm text-slate-400">{r.reportType} · {prettyEnum(r.status)} · {r.location}</p>
            </div>
            <select
              className={inputClass + " max-w-xs"}
              defaultValue={r.status}
              onChange={async (e) => {
                await campusApi.reportStatus(r.id, e.target.value as ReportStatus);
                load();
              }}
            >
              {["OPEN", "MATCHED", "CLAIM_IN_PROGRESS", "RETURNED", "CLOSED"].map((s) => (
                <option key={s} value={s}>{prettyEnum(s)}</option>
              ))}
            </select>
          </Card>
        ))}
      </section>
      <section className="space-y-3">
        <h2 className="font-display text-xl">Audit log</h2>
        <input className={inputClass} placeholder="Search activity" value={q} onChange={(e) => setQ(e.target.value)} />
        {filtered.map((a) => (
          <Card key={a.id}>
            <p className="font-medium">{a.action}</p>
            <p className="text-sm text-slate-400">{a.actorEmail} · {a.entityType} #{a.entityId}</p>
            <p className="text-sm text-slate-500">{a.details}</p>
            <p className="text-xs text-slate-500">{formatWhen(a.createdAt)}</p>
          </Card>
        ))}
      </section>
      <Button variant="ghost" onClick={load}>Refresh</Button>
    </div>
  );
}
