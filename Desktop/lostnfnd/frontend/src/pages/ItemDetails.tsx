import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { CATEGORY_LABEL, campusApi, type MatchView, type ReportCard, type ReportStatus } from "../api";
import { useAuth } from "../auth";
import { formatWhen, prettyEnum, statusTone } from "../format";
import { AuthImage, Badge, Breadcrumbs, Button, Card, ErrorState, Skeleton } from "../ui";

export default function ItemDetails() {
  const { id } = useParams();
  const confirmation = Boolean((useLocation().state as { confirmation?: boolean } | null)?.confirmation);
  const { user } = useAuth();
  const [item, setItem] = useState<ReportCard | null>(null);
  const [matches, setMatches] = useState<MatchView[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const load = () => {
    if (!id) return;
    setError(null);
    Promise.all([campusApi.report(id), campusApi.reportMatches(id)])
      .then(([r, m]) => { setItem(r); setMatches(m); })
      .catch((e) => setError(e.message));
  };
  useEffect(load, [id]);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!item) return <Skeleton className="h-64" />;

  const statuses: ReportStatus[] = ["OPEN", "MATCHED", "CLAIM_IN_PROGRESS", "RETURNED", "CLOSED"];

  return (
    <div className="grid gap-6 lg:grid-cols-[1.1fr_.9fr]">
      <div className="space-y-4">
      <Breadcrumbs items={[
        { label: "Home", href: "/app" },
        { label: "Browse", href: "/app/browse" },
        { label: item.itemName },
      ]} />
      <Card className="overflow-hidden p-0">
        <AuthImage src={item.photoUrl} alt={item.itemName} className="h-72 w-full object-cover" />
        <div className="space-y-3 p-5">
          {confirmation && (
            <p className="rounded-lg bg-emerald-400/10 px-3 py-2 text-sm text-emerald-200">Report submitted. Private details were stored encrypted and will not appear below.</p>
          )}
          <div className="flex flex-wrap gap-2">
            <Badge>{item.reportType}</Badge>
            <Badge tone={statusTone(item.status)}>{prettyEnum(item.status)}</Badge>
            <Badge tone="slate">{CATEGORY_LABEL[item.category]}</Badge>
          </div>
          <h1 className="font-display text-3xl">{item.itemName}</h1>
          <p className="text-slate-300">{item.publicDescription}</p>
          <dl className="grid gap-2 text-sm text-slate-400 sm:grid-cols-2">
            <div><dt className="text-slate-500">Location</dt><dd>{item.location}</dd></div>
            <div><dt className="text-slate-500">When</dt><dd>{formatWhen(item.occurredAt)}</dd></div>
            <div><dt className="text-slate-500">Brand</dt><dd>{item.brand || "—"}</dd></div>
            <div><dt className="text-slate-500">Color</dt><dd>{item.color || "—"}</dd></div>
            <div><dt className="text-slate-500">Private details on file</dt><dd>{item.privateDetailsOnFile ? "Yes (contents hidden)" : "No"}</dd></div>
          </dl>
          {user?.role === "ADMIN" && (
            <form className="flex flex-wrap items-end gap-2 pt-2" onSubmit={async (e) => {
              e.preventDefault();
              const status = String(new FormData(e.currentTarget).get("status")) as ReportStatus;
              try {
                setItem(await campusApi.reportStatus(item.id, status));
                setStatusMsg("Report status updated.");
              } catch (err) {
                setStatusMsg(err instanceof Error ? err.message : "Unable to update status");
              }
            }}>
              <label className="text-sm">
                <span className="mb-1 block text-slate-400">Admin: report status</span>
                <select name="status" defaultValue={item.status} className="rounded-lg border border-white/10 bg-slate-950/60 px-3 py-2 text-sm">
                  {statuses.map((s) => <option key={s} value={s}>{prettyEnum(s)}</option>)}
                </select>
              </label>
              <Button type="submit" variant="secondary">Update status</Button>
            </form>
          )}
          {statusMsg && <p className="text-sm text-slate-400">{statusMsg}</p>}
        </div>
      </Card>
      </div>
      <div className="space-y-3">
        <h2 className="font-display text-xl">Potential matches</h2>
        {matches.length === 0 && <Card><p className="text-sm text-slate-400">No compatible matches stored yet.</p></Card>}
        {matches.map((m) => (
          <Card key={m.id}>
            <div className="flex items-center justify-between">
              <p className="font-display text-2xl">{m.score}%</p>
              <Badge tone={m.confidence === "HIGH_CONFIDENCE" ? "emerald" : m.confidence === "POSSIBLE_MATCH" ? "violet" : "amber"}>{prettyEnum(m.confidence)}</Badge>
            </div>
            <p className="mt-2 text-sm text-slate-400">{m.explanation}</p>
            <p className="mt-2 text-xs text-slate-500">A high match score suggests correspondence but does not prove ownership.</p>
            <Link to={`/app/matches/${m.id}`}><Button className="mt-3" variant="secondary">Compare</Button></Link>
          </Card>
        ))}
      </div>
    </div>
  );
}
