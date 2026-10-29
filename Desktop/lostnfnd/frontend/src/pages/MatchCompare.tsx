import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { campusApi, type MatchView } from "../api";
import { useAuth } from "../auth";
import { prettyEnum, statusTone } from "../format";
import { AuthImage, Badge, Breadcrumbs, Button, Card, ErrorState, Field, Skeleton, inputClass } from "../ui";

const factors: Array<[string, keyof MatchView]> = [
  ["Text similarity (40%)", "textScore"],
  ["Location (20%)", "locationScore"],
  ["Date (15%)", "dateScore"],
  ["Category (10%)", "categoryScore"],
  ["Brand (10%)", "brandScore"],
  ["Color (5%)", "colorScore"],
];

export default function MatchCompare() {
  const { id } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const [match, setMatch] = useState<MatchView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [claimError, setClaimError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const load = () => {
    if (!id) return;
    campusApi.match(id).then(setMatch).catch((e) => setError(e.message));
  };
  useEffect(load, [id]);

  async function startClaim(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!match) return;
    const statement = String(new FormData(e.currentTarget).get("ownershipStatement") || "");
    setPending(true);
    setClaimError(null);
    try {
      const claim = await campusApi.submitClaim(match.id, statement);
      nav(`/app/claims/${claim.id}`);
    } catch (err) {
      setClaimError(err instanceof Error ? err.message : "Unable to start claim");
    } finally {
      setPending(false);
    }
  }

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!match) return <Skeleton className="h-64" />;

  return (
    <div className="space-y-6">
      <Breadcrumbs items={[
        { label: "Home", href: "/app" },
        { label: "Match comparison" },
      ]} />
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Match comparison</p>
        <h1 className="font-display text-3xl">{match.score}% · {prettyEnum(match.confidence)}</h1>
        <p className="mt-1 max-w-2xl text-sm text-slate-400">{match.explanation} A high match score suggests correspondence but does not prove ownership.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Side title="Lost report" item={match.lostItem} />
        <Side title="Found report" item={match.foundItem} />
      </div>
      <Card>
        <h2 className="font-display text-lg">Factor scores</h2>
        <div className="mt-4 space-y-3">
          {factors.map(([label, key]) => {
            const value = Number(match[key]);
            return (
              <div key={label}>
                <div className="mb-1 flex justify-between text-sm"><span>{label}</span><span>{value}</span></div>
                <div className="factor-bar"><span style={{ width: `${Math.min(100, value)}%` }} /></div>
              </div>
            );
          })}
        </div>
      </Card>
      {user?.role === "STUDENT" && (
        <Card>
          <h2 className="font-display text-lg">Start claim</h2>
          <p className="mt-1 text-sm text-slate-400">Describe identifying details you know. The system scores this against encrypted private details. You will only see a generic outcome — never the original secret.</p>
          <form className="mt-4 space-y-3" onSubmit={startClaim}>
            <Field label="Ownership information">
              <textarea className={inputClass} name="ownershipStatement" rows={4} minLength={4} required />
            </Field>
            {claimError && <p className="text-sm text-rose-300">{claimError}</p>}
            <Button disabled={pending}>{pending ? "Submitting…" : "Start claim"}</Button>
          </form>
        </Card>
      )}
    </div>
  );
}

function Side({ title, item }: { title: string; item: MatchView["lostItem"] }) {
  return (
    <Card className="overflow-hidden p-0">
      <AuthImage src={item.photoUrl} alt={item.itemName} className="h-48 w-full object-cover" />
      <div className="space-y-2 p-4">
        <div className="flex items-center justify-between">
          <p className="text-xs uppercase tracking-wide text-slate-400">{title}</p>
          <Badge tone={statusTone(item.status)}>{prettyEnum(item.status)}</Badge>
        </div>
        <h3 className="font-display text-xl">{item.itemName}</h3>
        <p className="text-sm text-slate-400">{item.publicDescription}</p>
        <p className="text-sm text-slate-500">{item.location}</p>
      </div>
    </Card>
  );
}
