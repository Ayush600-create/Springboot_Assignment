import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { Bell, LayoutDashboard, LogOut, Menu, Search, Shield, Upload, type LucideIcon } from "lucide-react";
import { campusApi } from "../api";
import { useAuth } from "../auth";
import { Button, Logo } from "../ui";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-2 rounded-lg px-3 py-2 text-sm ${isActive ? "bg-white/10 text-white" : "text-slate-300 hover:bg-white/5"}`;

export default function AppShell() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const admin = user?.role === "ADMIN";

  useEffect(() => {
    campusApi.unread().then((r) => setUnread(r.count)).catch(() => setUnread(0));
  }, []);

  const links: Array<[string, string, LucideIcon]> = admin
    ? [
        ["/app", "Dashboard", LayoutDashboard],
        ["/app/browse", "Reports", Search],
        ["/app/admin/claims", "Claims", Shield],
        ["/app/notifications", "Notifications", Bell],
      ]
    : [
        ["/app", "Dashboard", LayoutDashboard],
        ["/app/browse", "Search", Search],
        ["/app/report", "Report item", Upload],
        ["/app/notifications", "Notifications", Bell],
      ];

  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className={`border-b border-white/5 bg-ink-900/80 p-4 md:min-h-screen md:border-b-0 md:border-r ${open ? "block" : "hidden md:block"}`}>
        <Link to="/" onClick={() => setOpen(false)}><Logo /></Link>
        <nav className="mt-8 space-y-1">
          {links.map(([to, label, Icon]) => (
            <NavLink key={to} to={to} end={to === "/app"} className={linkClass} onClick={() => setOpen(false)}>
              <Icon className="h-4 w-4" />
              {label}
              {label === "Notifications" && unread > 0 && (
                <span className="ml-auto rounded-full bg-sky-400 px-1.5 text-[10px] font-bold text-slate-950">{unread}</span>
              )}
            </NavLink>
          ))}
          {admin && (
            <>
              <NavLink to="/app/admin/users" className={linkClass} onClick={() => setOpen(false)}>Users</NavLink>
              <NavLink to="/app/admin/audit" className={linkClass} onClick={() => setOpen(false)}>Audit log</NavLink>
            </>
          )}
        </nav>
      </aside>
      <div>
        <header className="sticky top-0 z-10 flex items-center justify-between border-b border-white/5 bg-ink-950/80 px-4 py-3 backdrop-blur">
          <button className="rounded-lg p-2 hover:bg-white/5 md:hidden" onClick={() => setOpen((v) => !v)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <p className="hidden text-sm text-slate-400 md:block">CampusGuard — Smart Lost and Found</p>
          <div className="flex items-center gap-3 text-sm">
            <div className="text-right">
              <p className="font-medium">{user?.name}</p>
              <p className="text-xs text-slate-400">{user?.role === "ADMIN" ? "Administrator" : user?.department}</p>
            </div>
            <Button variant="ghost" onClick={() => { logout(); nav("/"); }}>
              <LogOut className="h-4 w-4" /> Logout
            </Button>
          </div>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
