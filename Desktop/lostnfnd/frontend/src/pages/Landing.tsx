import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { ShieldCheck, ScanSearch, KeyRound } from "lucide-react";
import { campusApi, type PublicStats } from "../api";
import { Badge, Button, Card, DemoBanner, Logo } from "../ui";

const steps = ["Report", "Match", "Notify", "Claim", "Verify", "Approve", "OTP Handover"];

export default function Landing() {
  const [stats, setStats] = useState<PublicStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    campusApi.stats().then(setStats).catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <header className="sticky top-0 z-20 border-b border-white/5 bg-ink-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Logo />
          <nav className="hidden items-center gap-6 text-sm text-slate-300 md:flex">
            <a href="#how">How it works</a>
            <a href="#features">Features</a>
            <Link to="/browse">Browse</Link>
          </nav>
          <div className="flex gap-2">
            <Link to="/login"><Button variant="ghost">Log in</Button></Link>
            <Link to="/register"><Button>Join CampusGuard</Button></Link>
          </div>
        </div>
      </header>

      <section className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 md:grid-cols-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Smart Lost and Found</p>
          <h1 className="mt-3 font-display text-4xl font-semibold leading-tight md:text-5xl">
            Lost Something? Let's Bring It Back.
          </h1>
          <p className="mt-4 max-w-xl text-slate-300">
            CampusGuard is a secure, smart, campus-wide lost-and-found portal. Verified students, weighted matching,
            private identifying details, and admin-controlled OTP handover.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/report?type=LOST"><Button>Report Lost Item</Button></Link>
            <Link to="/browse?type=FOUND"><Button variant="secondary">Explore Found Items</Button></Link>
          </div>
        </div>
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="grid gap-3">
          <Card>
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-slate-400">Sony WH-1000XM4</p>
                <p className="font-display text-xl">Headphones pair</p>
              </div>
              <Badge>94.2% HIGH_CONFIDENCE</Badge>
            </div>
            <div className="mt-4 space-y-2">
              {[
                ["Text 40%", 85.5],
                ["Location 20%", 100],
                ["Date 15%", 100],
                ["Category 10%", 100],
                ["Brand 10%", 100],
                ["Color 5%", 100],
              ].map(([label, value]) => (
                <div key={String(label)}>
                  <div className="mb-1 flex justify-between text-[11px] text-slate-400">
                    <span>{label}</span><span>{value}</span>
                  </div>
                  <div className="factor-bar"><span style={{ width: `${value}%` }} /></div>
                </div>
              ))}
            </div>
            <p className="mt-3 text-sm text-slate-400">A high match score suggests correspondence but does not prove ownership.</p>
          </Card>
          <div className="grid grid-cols-2 gap-3">
            <Card><p className="text-xs text-slate-400">Lost · Central Library</p><p className="font-display text-lg">Report</p><p className="mt-1 text-xs text-slate-500">Private serials stay encrypted</p></Card>
            <Card><p className="text-xs text-slate-400">Found · 2nd floor desks</p><p className="font-display text-lg">Match</p><p className="mt-1 text-xs text-slate-500">Six-factor score, not ownership</p></Card>
          </div>
        </motion.div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-12">
        <DemoBanner show={stats?.demoData} />
        {error && <p className="mt-3 text-sm text-rose-300">{error}</p>}
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          {[
            ["Active lost reports", stats?.activeLostReports],
            ["Found reports", stats?.foundReports],
            ["Potential matches", stats?.potentialMatches],
          ].map(([label, value]) => (
            <Card key={String(label)}>
              <p className="text-sm text-slate-400">{label}</p>
              <p className="font-display text-3xl">{value ?? "—"}</p>
            </Card>
          ))}
        </div>
      </section>

      <section id="how" className="mx-auto max-w-6xl px-4 py-12">
        <h2 className="font-display text-2xl">How It Works</h2>
        <p className="mt-2 max-w-2xl text-sm text-slate-400">
          Recovery process: report the item, receive weighted matches, get notified, start a claim, complete ownership
          verification, wait for admin approval, then finish with OTP handover.
        </p>
        <div className="mt-5 flex flex-wrap gap-2">
          {steps.map((s, i) => (
            <Badge key={s} tone={i % 2 ? "violet" : "cyan"}>{`${i + 1}. ${s}`}</Badge>
          ))}
        </div>
      </section>

      <section id="features" className="mx-auto grid max-w-6xl gap-4 px-4 pb-16 md:grid-cols-3">
        {[
          [ScanSearch, "Smart matching", "Six weighted factors with explainable scores."],
          [ShieldCheck, "Verified students", "College email format plus admin approval. Typing an ID is not verification."],
          [KeyRound, "Secure claims", "Private details stay encrypted. OTP handover is admin-controlled."],
        ].map(([Icon, title, body]) => (
          <Card key={String(title)}>
            <Icon className="h-5 w-5 text-sky-300" />
            <h3 className="mt-3 font-display text-lg">{title as string}</h3>
            <p className="mt-1 text-sm text-slate-400">{body as string}</p>
          </Card>
        ))}
      </section>

      <footer className="border-t border-white/5 py-8 text-sm text-slate-400">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 md:flex-row md:items-center md:justify-between">
          <Logo />
          <nav className="flex flex-wrap gap-4">
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
            <Link to="/browse">Browse reports</Link>
            <a href="mailto:contact@campusguard.local">contact@campusguard.local</a>
          </nav>
          <p>CampusGuard — Smart Lost and Found</p>
        </div>
      </footer>
    </div>
  );
}
