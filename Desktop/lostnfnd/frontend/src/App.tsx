import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import AppShell from "./layout/AppShell";
import Landing from "./pages/Landing";
import { LoginPage, RegisterPage } from "./pages/AuthPages";
import StudentDashboard from "./pages/StudentDashboard";
import AdminHome from "./pages/AdminHome";
import Browse from "./pages/Browse";
import ReportForm from "./pages/ReportForm";
import ItemDetails from "./pages/ItemDetails";
import MatchCompare from "./pages/MatchCompare";
import ClaimDetail from "./pages/ClaimDetail";
import Notifications from "./pages/Notifications";
import AdminClaimDetail from "./pages/AdminClaimDetail";
import AdminUsers from "./pages/AdminUsers";
import AdminAudit from "./pages/AdminAudit";
import { Skeleton } from "./ui";

function Guard({ children, admin }: { children: ReactNode; admin?: boolean }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="p-10"><Skeleton className="h-24" /></div>;
  if (!user) return <Navigate to="/login" replace />;
  if (admin && user.role !== "ADMIN") return <Navigate to="/app" replace />;
  return <>{children}</>;
}

function Home() {
  const { user } = useAuth();
  return user?.role === "ADMIN" ? <AdminHome /> : <StudentDashboard />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/browse" element={<Navigate to="/app/browse" replace />} />
      <Route path="/report" element={<Navigate to="/app/report" replace />} />
      <Route
        path="/app"
        element={
          <Guard>
            <AppShell />
          </Guard>
        }
      >
        <Route index element={<Home />} />
        <Route path="browse" element={<Browse />} />
        <Route path="report" element={<ReportForm />} />
        <Route path="items/:id" element={<ItemDetails />} />
        <Route path="matches/:id" element={<MatchCompare />} />
        <Route path="claims/:id" element={<ClaimDetail />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="admin/claims" element={<Guard admin><AdminHome queue /></Guard>} />
        <Route path="admin/claims/:id" element={<Guard admin><AdminClaimDetail /></Guard>} />
        <Route path="admin/users" element={<Guard admin><AdminUsers /></Guard>} />
        <Route path="admin/audit" element={<Guard admin><AdminAudit /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
