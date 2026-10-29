import type { ClaimStatus, Confidence, ReportStatus } from "./api";

export function formatWhen(value?: string | null) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value.replace("T", " ").slice(0, 16);
  return d.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

export function statusTone(status: ReportStatus | ClaimStatus | Confidence) {
  if (status === "RETURNED" || status === "COMPLETED" || status === "HIGH_CONFIDENCE") return "emerald" as const;
  if (status === "REJECTED" || status === "CLOSED") return "rose" as const;
  if (status === "APPROVED_FOR_HANDOVER" || status === "POSSIBLE_MATCH" || status === "MATCHED") return "violet" as const;
  if (status === "CLAIM_IN_PROGRESS" || status === "UNDER_REVIEW" || status === "VERIFIED") return "amber" as const;
  return "cyan" as const;
}

export function prettyEnum(value: string) {
  return value.replaceAll("_", " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}
