import { useEffect, useState, type ButtonHTMLAttributes, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { getToken } from "./api";

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "ghost" | "danger" }) {
  const styles = {
    primary: "bg-sky-400 text-slate-950 hover:bg-sky-300",
    secondary: "bg-white/5 text-slate-100 border border-white/10 hover:bg-white/10",
    ghost: "text-slate-200 hover:bg-white/5",
    danger: "bg-rose-500/90 text-white hover:bg-rose-400",
  }[variant];
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition disabled:opacity-50 ${styles} ${className}`}
      {...props}
    />
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`surface rounded-2xl p-5 ${className}`}>{children}</div>;
}

export function Badge({ children, tone = "cyan" }: { children: ReactNode; tone?: "cyan" | "violet" | "amber" | "rose" | "emerald" | "slate" }) {
  const map = {
    cyan: "bg-sky-400/15 text-sky-300",
    violet: "bg-indigo-400/15 text-indigo-300",
    amber: "bg-amber-400/15 text-amber-300",
    rose: "bg-rose-400/15 text-rose-300",
    emerald: "bg-emerald-400/15 text-emerald-300",
    slate: "bg-slate-400/15 text-slate-300",
  };
  return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${map[tone]}`}>{children}</span>;
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-slate-200">{label}</span>
      {children}
      {hint && <span className="block text-xs text-slate-400">{hint}</span>}
    </label>
  );
}

export const inputClass =
  "w-full rounded-lg border border-white/10 bg-slate-950/60 px-3 py-2.5 text-sm text-slate-100 outline-none ring-sky-400/0 transition placeholder:text-slate-500 focus:border-sky-400/40 focus:ring-4 focus:ring-sky-400/10";

export function Skeleton({ className = "" }: { className?: string }) {
  return <div className={`animate-pulse rounded-xl bg-white/5 ${className}`} />;
}

export function Empty({ title, body }: { title: string; body: string }) {
  return (
    <Card className="text-center">
      <h3 className="font-display text-lg">{title}</h3>
      <p className="mt-1 text-sm text-slate-400">{body}</p>
    </Card>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <Card className="border-rose-400/20">
      <p className="text-sm text-rose-300">{message}</p>
      {onRetry && (
        <Button className="mt-3" variant="secondary" onClick={onRetry}>
          Retry
        </Button>
      )}
    </Card>
  );
}

export function DemoBanner({ show }: { show?: boolean }) {
  if (!show) return null;
  return (
    <div className="rounded-lg border border-amber-400/20 bg-amber-400/10 px-3 py-2 text-xs font-medium text-amber-200">
      Demo data
    </div>
  );
}

export function AuthImage({ src, alt, className }: { src?: string | null; alt: string; className?: string }) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!src) return;
    const token = getToken();
    let objectUrl: string | null = null;
    fetch(src, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((r) => (r.ok ? r.blob() : Promise.reject()))
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => setUrl(null));
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [src]);
  if (!src) {
    return <div className={`grid place-items-center bg-white/5 text-xs text-slate-500 ${className}`}>No photo</div>;
  }
  if (!url) return <div className={`animate-pulse bg-white/5 ${className}`} />;
  return <img src={url} alt={alt} className={className} />;
}

export function Breadcrumbs({ items }: { items: Array<{ label: string; href?: string }> }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-4 text-xs text-slate-500">
      <ol className="flex flex-wrap items-center gap-1">
        {items.map((item, i) => (
          <li key={item.label} className="flex items-center gap-1">
            {i > 0 && <span aria-hidden="true">/</span>}
            {item.href ? (
              <Link className="hover:text-slate-200" to={item.href}>{item.label}</Link>
            ) : (
              <span className="text-slate-300">{item.label}</span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-sky-400 text-sm font-bold text-slate-950">CG</span>
      <span className={compact ? "hidden sm:block" : "block"}>
        <span className="block font-display text-sm font-semibold leading-tight">CampusGuard</span>
        <span className="block text-[11px] text-slate-400">Smart Lost and Found</span>
      </span>
    </div>
  );
}
