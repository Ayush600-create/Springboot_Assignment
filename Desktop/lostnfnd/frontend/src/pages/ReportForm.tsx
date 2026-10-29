import { FormEvent, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CATEGORY_LABEL, campusApi, type ItemCategory, type ReportType } from "../api";
import { Button, Card, Field, inputClass } from "../ui";

const types: ReportType[] = ["LOST", "FOUND"];

export default function ReportForm() {
  const [params] = useSearchParams();
  const nav = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const defaultType = params.get("type") === "FOUND" ? "FOUND" : "LOST";

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    const file = fd.get("photograph");
    if (file instanceof File && file.size > 0) {
      if (file.size > 5 * 1024 * 1024) {
        setError("Photographs must be 5 MB or smaller.");
        return;
      }
      if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
        setError("Only JPEG, PNG, or WebP photographs are accepted.");
        return;
      }
    }
    setPending(true);
    setError(null);
    try {
      const created = await campusApi.createReport(fd);
      nav(`/app/items/${created.id}`, { state: { confirmation: true } });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to submit report");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <div>
        <p className="text-xs uppercase tracking-[0.18em] text-sky-300">Report</p>
        <h1 className="font-display text-3xl">Report a lost or found item</h1>
        <p className="mt-1 text-sm text-slate-400">Public fields appear on item cards. Private identifying details are encrypted and never returned by the API.</p>
      </div>
      <form className="space-y-5" onSubmit={onSubmit}>
        <Card className="space-y-4">
          <h2 className="font-display text-lg">Public details</h2>
          <Field label="Report type">
            <select className={inputClass} name="reportType" defaultValue={defaultType}>
              {types.map((t) => <option key={t} value={t}>{t === "LOST" ? "Lost item" : "Found item"}</option>)}
            </select>
          </Field>
          <Field label="Item name"><input className={inputClass} name="itemName" required /></Field>
          <Field label="Category">
            <select className={inputClass} name="category" required>
              {(Object.keys(CATEGORY_LABEL) as ItemCategory[]).map((c) => (
                <option key={c} value={c}>{CATEGORY_LABEL[c]}</option>
              ))}
            </select>
          </Field>
          <Field label="Public description" hint="Safe to show on public cards.">
            <textarea className={inputClass} name="publicDescription" rows={4} required />
          </Field>
          <div className="grid gap-3 md:grid-cols-2">
            <Field label="Location"><input className={inputClass} name="location" required /></Field>
            <Field label="Date and approximate time"><input className={inputClass} type="datetime-local" name="occurredAt" required /></Field>
            <Field label="Brand (optional)"><input className={inputClass} name="brand" /></Field>
            <Field label="Color (optional)"><input className={inputClass} name="color" /></Field>
          </div>
          <Field label="Photograph" hint="JPEG, PNG, or WebP. Max 5 MB.">
            <input className={inputClass} type="file" name="photograph" accept="image/jpeg,image/png,image/webp" />
          </Field>
        </Card>
        <Card className="space-y-4">
          <h2 className="font-display text-lg">Private identifying details</h2>
          <p className="text-sm text-slate-400">Serial numbers and secret ownership marks stay encrypted at rest (AES-256-GCM). They are used only for verification scoring and are never shown on cards or in API responses.</p>
          <Field label="Serial number (optional)"><input className={inputClass} name="serialNumber" /></Field>
          <Field label="Identifying marks or ownership information (optional)">
            <textarea className={inputClass} name="privateIdentifyingMarks" rows={4} />
          </Field>
        </Card>
        {error && <p className="text-sm text-rose-300">{error}</p>}
        <Button disabled={pending}>{pending ? "Submitting…" : "Submit report"}</Button>
      </form>
    </div>
  );
}
