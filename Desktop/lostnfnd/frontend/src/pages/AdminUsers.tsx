import { useEffect, useState } from "react";
import { campusApi, type AccountStatus, type UserView } from "../api";
import { prettyEnum } from "../format";
import { Badge, Button, Card, ErrorState, Skeleton, inputClass } from "../ui";

export default function AdminUsers() {
  const [users, setUsers] = useState<UserView[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [reason, setReason] = useState("Admin review of student account");

  const load = () => campusApi.adminUsers().then(setUsers).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!users) return <Skeleton className="h-40" />;

  const filtered = users.filter((u) => `${u.name} ${u.email} ${u.studentId}`.toLowerCase().includes(q.toLowerCase()));

  async function decide(id: number, status: AccountStatus) {
    if (!reason.trim()) return;
    await campusApi.userDecision(id, status, reason.trim());
    load();
  }

  return (
    <div className="space-y-4">
      <h1 className="font-display text-3xl">User management</h1>
      <input className={inputClass} placeholder="Search users" value={q} onChange={(e) => setQ(e.target.value)} />
      <textarea className={inputClass} value={reason} onChange={(e) => setReason(e.target.value)} rows={2} aria-label="Recorded reason" />
      {filtered.map((u) => (
        <Card key={u.id} className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-medium">{u.name} · {u.email}</p>
            <p className="text-sm text-slate-400">{u.studentId} · {u.department} · {u.role}</p>
            <p className="text-xs text-slate-500">
              {u.collegeEmailFormat ? "College email format matches campus suffixes" : "Email is not a configured college suffix"}.
              Account is {prettyEnum(u.accountStatus)} — typing a student ID is not verification.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={u.accountStatus === "ACTIVE" ? "emerald" : u.accountStatus === "PENDING_APPROVAL" ? "amber" : "rose"}>
              {prettyEnum(u.accountStatus)}
            </Badge>
            {u.role !== "ADMIN" && u.accountStatus === "PENDING_APPROVAL" && (
              <>
                <Button variant="secondary" onClick={() => decide(u.id, "ACTIVE")}>Approve</Button>
                <Button variant="danger" onClick={() => decide(u.id, "REJECTED")}>Reject</Button>
              </>
            )}
            {u.role !== "ADMIN" && u.accountStatus === "ACTIVE" && (
              <Button variant="danger" onClick={() => decide(u.id, "SUSPENDED")}>Suspend</Button>
            )}
            {u.role !== "ADMIN" && (u.accountStatus === "REJECTED" || u.accountStatus === "SUSPENDED") && (
              <Button variant="secondary" onClick={() => decide(u.id, "ACTIVE")}>Reactivate</Button>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
}
