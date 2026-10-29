import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { campusApi, type ClaimView } from "../api";
import { formatWhen, prettyEnum, statusTone } from "../format";
import { Badge, Card, ErrorState, Skeleton } from "../ui";

const steps: ClaimView["status"][] = [
  "PENDING",
  "VERIFIED",
  "UNDER_REVIEW",
  "APPROVED_FOR_HANDOVER",
  "COMPLETED",
];

export default function ClaimDetail() {
  const { id } = useParams();
  const [claim, setClaim] = useState<ClaimView | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    if (!id) return;
    campusApi.claim(id).then(setClaim).catch((e) => setError(e.message));
  };
  useEffect(load, [id]);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!claim) return <Skeleton className="h-48" />;

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Claim</p>
        <h1 className="font-display text-3xl">Claim #{claim.id}</h1>
      </div>
      <Card className="space-y-3">
        <div className="flex flex-wrap gap-2">
          <Badge tone={statusTone(claim.status)}>{prettyEnum(claim.status)}</Badge>
          <Badge tone="slate">{prettyEnum(claim.reviewStatus)}</Badge>
          {claim.verificationScore != null && (
            <Badge tone={claim.verificationDecision === "MEETS_THRESHOLD" ? "emerald" : "rose"}>
              {prettyEnum(claim.verificationDecision)}
            </Badge>
          )}
        </div>
        <p className="text-slate-300">{claim.genericOutcome}</p>
        <p className="text-sm text-slate-500">
          Verification score is stored on the claim and is separate from the match score.
          {claim.otpStatus ? ` OTP status: ${prettyEnum(claim.otpStatus)}.` : ""}
        </p>
        {claim.otpStatus === "DELIVERY_UNAVAILABLE" && (
          <p className="rounded-lg border border-amber-400/20 bg-amber-400/10 px-3 py-2 text-sm text-amber-200">
            OTP email is not configured. Handover cannot be completed until SMTP is available.
          </p>
        )}
        <p className="text-xs text-slate-500">Updated {formatWhen(claim.updatedAt)}</p>
        <Link className="text-sm text-sky-300" to={`/app/matches/${claim.matchId}`}>Open match comparison</Link>
      </Card>
      <Card>
        <h2 className="font-display text-lg">Status timeline</h2>
        <ol className="mt-4 space-y-3">
          {steps.map((step) => (
            <li key={step} className="flex items-center gap-3 text-sm">
              <span className={`h-2.5 w-2.5 rounded-full ${claim.status === step || (claim.status === "REJECTED" && step === "PENDING") ? "bg-sky-400" : "bg-white/20"}`} />
              {prettyEnum(step)}
            </li>
          ))}
          {claim.status === "REJECTED" && <li className="text-rose-300">Rejected / flagged for admin review</li>}
        </ol>
      </Card>
    </div>
  );
}
