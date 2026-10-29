import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { campusApi, type ClaimView, type MatchView } from "../api";
import { prettyEnum, statusTone } from "../format";
import { Badge, Button, Card, ErrorState, Field, Skeleton, inputClass } from "../ui";

export default function AdminClaimDetail() {
  const { id } = useParams();
  const [claim, setClaim] = useState<ClaimView | null>(null);
  const [match, setMatch] = useState<MatchView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [otpMessage, setOtpMessage] = useState<string | null>(null);

  const load = () => {
    if (!id) return;
    setError(null);
    campusApi.adminClaim(id).then(async (c) => {
      setClaim(c);
      setMatch(await campusApi.match(c.matchId));
    }).catch((e) => setError(e.message));
  };
  useEffect(load, [id]);

  async function run(action: (reason: string) => Promise<ClaimView>, e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const reason = String(new FormData(e.currentTarget).get("reason") || "");
    setMessage(null);
    try {
      setClaim(await action(reason));
      setMessage("Recorded.");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Action failed");
    }
  }

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!claim) return <Skeleton className="h-64" />;

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Claim review</p>
        <h1 className="font-display text-3xl">Claim #{claim.id}</h1>
      </div>
      <Card className="space-y-2">
        <div className="flex flex-wrap gap-2">
          <Badge tone={statusTone(claim.status)}>{prettyEnum(claim.status)}</Badge>
          <Badge tone="slate">{prettyEnum(claim.reviewStatus)}</Badge>
        </div>
        <p>{claim.genericOutcome}</p>
        <p className="text-sm text-slate-400">
          Verification score {claim.verificationScore ?? "—"} against threshold {claim.thresholdUsed ?? "—"}.
          This is not the match score.
        </p>
        {claim.otpStatus === "DELIVERY_UNAVAILABLE" && (
          <p className="rounded-lg bg-amber-400/10 px-3 py-2 text-sm text-amber-200">OTP email is not configured. Handover cannot be completed.</p>
        )}
        {match && <Link className="text-sm text-sky-300" to={`/app/matches/${match.id}`}>Open side-by-side match ({match.score}%)</Link>}
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        <ActionForm title="Open review" onSubmit={(e) => { e.preventDefault(); campusApi.openReview(claim.id).then(setClaim).catch((err) => setMessage(err.message)); }} reason={false} />
        <ActionForm title="Reopen rejected claim" hint="Allowed once, reason required." onSubmit={(e) => run((r) => campusApi.reopen(claim.id, r), e)} />
        <ActionForm title="Approve for handover" onSubmit={(e) => run((r) => campusApi.approve(claim.id, r), e)} />
        <ActionForm title="Reject" danger onSubmit={(e) => run((r) => campusApi.reject(claim.id, r), e)} />
      </div>
      {message && <p className="text-sm text-sky-200">{message}</p>}

      <Card className="space-y-3">
        <h2 className="font-display text-lg">OTP handover</h2>
        <p className="text-sm text-slate-400">The claimant tells the administrator the emailed OTP. The code is never displayed here.</p>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={async () => {
            try {
              const res = await campusApi.issueOtp(claim.id);
              setOtpMessage(res.otpStatus === "DELIVERY_UNAVAILABLE" || /not configured/i.test(res.message)
                ? "OTP email is not configured"
                : res.message);
              load();
            } catch (err) {
              setOtpMessage(err instanceof Error ? err.message : "Unable to issue OTP");
            }
          }}>Issue OTP email</Button>
        </div>
        {claim.otpStatus === "DELIVERY_UNAVAILABLE" || claim.otpStatus === "NOT_ISSUED" ? (
          <p className="text-sm text-amber-200">OTP verification is blocked until a code is successfully emailed.</p>
        ) : (
        <form className="flex flex-wrap gap-2" onSubmit={async (e) => {
          e.preventDefault();
          const otp = String(new FormData(e.currentTarget).get("otp") || "");
          try {
            await campusApi.verifyOtp(claim.id, otp);
            setOtpMessage("Handover completed. Item marked returned.");
            load();
          } catch (err) {
            setOtpMessage(err instanceof Error ? err.message : "OTP verification failed");
          }
        }}>
          <input className={inputClass + " max-w-xs"} name="otp" inputMode="numeric" minLength={6} maxLength={6} placeholder="6-digit OTP" required />
          <Button type="submit">Verify OTP</Button>
        </form>
        )}
        {otpMessage && <p className="text-sm text-slate-300">{otpMessage}</p>}
      </Card>
    </div>
  );
}

function ActionForm({ title, hint, danger, reason = true, onSubmit }: {
  title: string; hint?: string; danger?: boolean; reason?: boolean; onSubmit: (e: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <Card>
      <h3 className="font-medium">{title}</h3>
      {hint && <p className="text-xs text-slate-400">{hint}</p>}
      <form className="mt-3 space-y-2" onSubmit={onSubmit}>
        {reason && <Field label="Recorded reason"><textarea className={inputClass} name="reason" required /></Field>}
        <Button variant={danger ? "danger" : "secondary"}>{title}</Button>
      </form>
    </Card>
  );
}
