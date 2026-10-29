import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../api";
import { useAuth } from "../auth";
import { Button, Card, Field, Logo, inputClass } from "../ui";

export function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setPending(true);
    setError(null);
    try {
      await login(String(fd.get("email")), String(fd.get("password")));
      nav("/app");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Unable to log in");
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthFrame title="Welcome back">
      <form className="space-y-4" onSubmit={onSubmit}>
        <Field label="College email"><input className={inputClass} name="email" type="email" required /></Field>
        <Field label="Password"><input className={inputClass} name="password" type="password" required /></Field>
        {error && <p className="text-sm text-rose-300">{error}</p>}
        <Button className="w-full" disabled={pending}>{pending ? "Signing in…" : "Log in"}</Button>
      </form>
      <p className="mt-4 text-sm text-slate-400">No account? <Link className="text-sky-300" to="/register">Register</Link></p>
    </AuthFrame>
  );
}

export function RegisterPage() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setPending(true);
    setError(null);
    try {
      const res = await register({
        name: String(fd.get("name")),
        email: String(fd.get("email")),
        password: String(fd.get("password")),
        studentId: String(fd.get("studentId")),
        department: String(fd.get("department")),
      });
      if (!res.token) {
        setMessage("Account created and waiting for admin approval. Typing a student ID does not verify you.");
      } else {
        nav("/app");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Unable to register");
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthFrame title="Create a student account">
      {message ? <p className="text-sm text-sky-200">{message}</p> : (
        <form className="space-y-4" onSubmit={onSubmit}>
          <Field label="Full name"><input className={inputClass} name="name" required /></Field>
          <Field label="College email"><input className={inputClass} name="email" type="email" required /></Field>
          <Field label="Password" hint="At least 8 characters"><input className={inputClass} name="password" type="password" minLength={8} required /></Field>
          <Field label="Student ID"><input className={inputClass} name="studentId" required /></Field>
          <Field label="Department"><input className={inputClass} name="department" required /></Field>
          {error && <p className="text-sm text-rose-300">{error}</p>}
          <Button className="w-full" disabled={pending}>{pending ? "Submitting…" : "Register"}</Button>
        </form>
      )}
      <p className="mt-4 text-sm text-slate-400">Already registered? <Link className="text-sky-300" to="/login">Log in</Link></p>
    </AuthFrame>
  );
}

function AuthFrame({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4 py-10">
      <Link to="/" className="mb-6"><Logo /></Link>
      <Card>
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-300">CampusGuard — Smart Lost and Found</p>
        <h1 className="mt-2 font-display text-2xl">{title}</h1>
        <div className="mt-5">{children}</div>
      </Card>
    </div>
  );
}
