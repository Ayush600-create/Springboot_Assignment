import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CATEGORY_LABEL, campusApi, type ItemCategory, type Page, type ReportCard } from "../api";
import { prettyEnum, statusTone } from "../format";
import { AuthImage, Badge, Button, Card, Empty, ErrorState, Field, Skeleton, inputClass } from "../ui";

export default function Browse() {
  const [params, setParams] = useSearchParams();
  const [page, setPage] = useState<Page<ReportCard> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const query = useMemo(() => params.toString(), [params]);

  const load = () => {
    setError(null);
    const sp = new URLSearchParams(params);
    if (!sp.get("size")) sp.set("size", "12");
    campusApi.reports(sp).then(setPage).catch((e) => setError(e.message));
  };
  useEffect(load, [query]);

  function apply(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const next = new URLSearchParams();
    for (const [k, v] of fd.entries()) {
      if (String(v)) next.set(k, String(v));
    }
    next.set("page", "0");
    setParams(next);
  }

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Search</p>
        <h1 className="font-display text-3xl">Browse campus reports</h1>
      </div>
      <Card>
        <form className="grid gap-3 md:grid-cols-3" onSubmit={apply}>
          <Field label="Name or description"><input className={inputClass} name="q" defaultValue={params.get("q") ?? ""} /></Field>
          <Field label="Lost / found">
            <select className={inputClass} name="type" defaultValue={params.get("type") ?? ""}>
              <option value="">Both</option>
              <option value="LOST">Lost</option>
              <option value="FOUND">Found</option>
            </select>
          </Field>
          <Field label="Category">
            <select className={inputClass} name="category" defaultValue={params.get("category") ?? ""}>
              <option value="">Any</option>
              {(Object.keys(CATEGORY_LABEL) as ItemCategory[]).map((c) => (
                <option key={c} value={c}>{CATEGORY_LABEL[c]}</option>
              ))}
            </select>
          </Field>
          <Field label="Location"><input className={inputClass} name="location" defaultValue={params.get("location") ?? ""} /></Field>
          <Field label="Brand"><input className={inputClass} name="brand" defaultValue={params.get("brand") ?? ""} /></Field>
          <Field label="Color"><input className={inputClass} name="color" defaultValue={params.get("color") ?? ""} /></Field>
          <Field label="Status">
            <select className={inputClass} name="status" defaultValue={params.get("status") ?? ""}>
              <option value="">Any</option>
              <option value="OPEN">Open</option>
              <option value="MATCHED">Matched</option>
              <option value="CLAIM_IN_PROGRESS">Claim in progress</option>
              <option value="RETURNED">Returned</option>
              <option value="CLOSED">Closed</option>
            </select>
          </Field>
          <Field label="From"><input className={inputClass} type="date" name="fromDate" defaultValue={params.get("fromDate") ?? ""} /></Field>
          <Field label="To"><input className={inputClass} type="date" name="toDate" defaultValue={params.get("toDate") ?? ""} /></Field>
          <Field label="Sort">
            <select className={inputClass} name="sort" defaultValue={params.get("sort") ?? "newest"}>
              <option value="newest">Newest</option>
              <option value="oldest">Oldest</option>
              <option value="relevance">Match relevance</option>
            </select>
          </Field>
          <div className="flex items-end"><Button type="submit">Apply filters</Button></div>
        </form>
      </Card>
      {error && <ErrorState message={error} onRetry={load} />}
      {!page && !error && <Skeleton className="h-40" />}
      {page && page.content.length === 0 && <Empty title="No matching reports" body="Try a broader search. Results come from the live CampusGuard database." />}
      {page && page.content.length > 0 && (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {page.content.map((item) => (
              <ItemCard key={item.id} item={item} />
            ))}
          </div>
          <div className="flex items-center justify-between text-sm text-slate-400">
            <p>{page.totalElements} reports</p>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={page.number <= 0} onClick={() => { const n = new URLSearchParams(params); n.set("page", String(page.number - 1)); setParams(n); }}>Previous</Button>
              <Button variant="secondary" disabled={page.number + 1 >= page.totalPages} onClick={() => { const n = new URLSearchParams(params); n.set("page", String(page.number + 1)); setParams(n); }}>Next</Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export function ItemCard({ item }: { item: ReportCard }) {
  return (
    <Link to={`/app/items/${item.id}`}>
      <Card className="h-full overflow-hidden p-0">
        <AuthImage src={item.photoUrl} alt={item.itemName} className="h-40 w-full object-cover" />
        <div className="space-y-2 p-4">
          <div className="flex items-start justify-between gap-2">
            <p className="font-medium">{item.itemName}</p>
            <Badge tone={item.reportType === "LOST" ? "amber" : "cyan"}>{item.reportType}</Badge>
          </div>
          <p className="line-clamp-2 text-sm text-slate-400">{item.publicDescription}</p>
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>{item.location}</span>
            <Badge tone={statusTone(item.status)}>{prettyEnum(item.status)}</Badge>
          </div>
        </div>
      </Card>
    </Link>
  );
}
