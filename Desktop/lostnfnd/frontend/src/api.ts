export type Role = "STUDENT" | "ADMIN";
export type AccountStatus = "PENDING_APPROVAL" | "ACTIVE" | "REJECTED" | "SUSPENDED";
export type ReportType = "LOST" | "FOUND";
export type ItemCategory =
  | "ELECTRONICS"
  | "IDENTITY_CARDS"
  | "BOOKS_AND_STATIONERY"
  | "BAGS"
  | "CLOTHING"
  | "KEYS"
  | "ACCESSORIES"
  | "OTHER";
export type ReportStatus = "OPEN" | "MATCHED" | "CLAIM_IN_PROGRESS" | "RETURNED" | "CLOSED";
export type Confidence = "HIGH_CONFIDENCE" | "POSSIBLE_MATCH" | "LOW_CONFIDENCE";
export type ClaimStatus =
  | "PENDING"
  | "VERIFIED"
  | "REJECTED"
  | "UNDER_REVIEW"
  | "APPROVED_FOR_HANDOVER"
  | "COMPLETED";
export type ReviewStatus = "NONE" | "FLAGGED_FOR_ADMIN_REVIEW" | "IN_REVIEW" | "CLOSED";
export type VerificationDecision = "BELOW_THRESHOLD" | "MEETS_THRESHOLD";
export type OtpStatus = "NOT_ISSUED" | "ISSUED" | "DELIVERY_UNAVAILABLE" | "VERIFIED" | "EXPIRED" | "LOCKED";

export interface UserView {
  id: number;
  name: string;
  email: string;
  studentId: string;
  department: string;
  role: Role;
  accountStatus: AccountStatus;
  collegeEmailFormat: boolean;
}

export interface ReportCard {
  id: number;
  reportType: ReportType;
  itemName: string;
  category: ItemCategory;
  publicDescription: string;
  location: string;
  occurredAt: string;
  brand?: string | null;
  color?: string | null;
  photoUrl?: string | null;
  status: ReportStatus;
  privateDetailsOnFile: boolean;
  createdAt: string;
}

export interface MatchView {
  id: number;
  score: number;
  confidence: Confidence;
  textScore: number;
  locationScore: number;
  dateScore: number;
  categoryScore: number;
  brandScore: number;
  colorScore: number;
  explanation: string;
  lostItem: ReportCard;
  foundItem: ReportCard;
}

export interface ClaimView {
  id: number;
  matchId: number;
  status: ClaimStatus;
  reviewStatus: ReviewStatus;
  verificationDecision: VerificationDecision;
  verificationScore: number | null;
  thresholdUsed: number | null;
  adminReason?: string | null;
  createdAt: string;
  updatedAt: string;
  otpStatus?: OtpStatus | null;
  genericOutcome: string;
}

export interface NotificationView {
  id: number;
  type: string;
  title: string;
  body: string;
  read: boolean;
  createdAt: string;
  referenceId?: number | null;
}

export interface PublicStats {
  activeLostReports: number;
  foundReports: number;
  potentialMatches: number;
  demoData: boolean;
}

export interface StudentDashboard {
  profile: UserView;
  lostReports: number;
  foundReports: number;
  potentialMatches: number;
  activeClaims: number;
  recentItems: ReportCard[];
  recentNotifications: NotificationView[];
  claims: ClaimView[];
  demoData: boolean;
}

export interface AdminDashboard {
  totalStudents: number;
  lostReports: number;
  foundReports: number;
  unresolvedReports: number;
  potentialMatches: number;
  pendingClaims: number;
  approvedClaims: number;
  completedHandovers: number;
  recentActivity: AuditView[];
  demoData: boolean;
}

export interface AuditView {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: number;
  details: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

const TOKEN_KEY = "campusguard.token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function parse<T>(res: Response): Promise<T> {
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new ApiError(res.status, data?.error || "Request failed");
  }
  return data as T;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  return parse<T>(await fetch(path, { ...init, headers }));
}

export const campusApi = {
  stats: () => api<PublicStats>("/api/public/stats"),
  login: (email: string, password: string) =>
    api<{ token: string; user: UserView }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  register: (body: Record<string, string>) =>
    api<{ token: string | null; user: UserView }>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  me: () => api<UserView>("/api/auth/me"),
  studentDashboard: () => api<StudentDashboard>("/api/student/dashboard"),
  adminDashboard: () => api<AdminDashboard>("/api/admin/dashboard"),
  reports: (params: URLSearchParams) => api<Page<ReportCard>>(`/api/reports?${params}`),
  mineReports: () => api<ReportCard[]>("/api/reports/mine"),
  report: (id: string | number) => api<ReportCard>(`/api/reports/${id}`),
  reportMatches: (id: string | number) => api<MatchView[]>(`/api/reports/${id}/matches`),
  match: (id: string | number) => api<MatchView>(`/api/matches/${id}`),
  createReport: (form: FormData) => api<ReportCard>("/api/reports", { method: "POST", body: form }),
  claimsMine: () => api<ClaimView[]>("/api/claims/mine"),
  claim: (id: string | number) => api<ClaimView>(`/api/claims/${id}`),
  submitClaim: (matchId: number, ownershipStatement: string) =>
    api<ClaimView>("/api/claims", {
      method: "POST",
      body: JSON.stringify({ matchId, ownershipStatement }),
    }),
  notifications: () => api<NotificationView[]>("/api/notifications"),
  unread: () => api<{ count: number }>("/api/notifications/unread-count"),
  readNotification: (id: number) =>
    api(`/api/notifications/${id}/read`, { method: "POST" }),
  adminUsers: () => api<UserView[]>("/api/admin/users"),
  userDecision: (id: number, status: AccountStatus, reason: string) =>
    api<UserView>(`/api/admin/users/${id}/decision`, {
      method: "POST",
      body: JSON.stringify({ status, reason }),
    }),
  adminClaims: () => api<ClaimView[]>("/api/admin/claims"),
  adminClaim: (id: string | number) => api<ClaimView>(`/api/admin/claims/${id}`),
  openReview: (id: number) => api<ClaimView>(`/api/admin/claims/${id}/open-review`, { method: "POST" }),
  reopen: (id: number, reason: string) =>
    api<ClaimView>(`/api/admin/claims/${id}/reopen`, { method: "POST", body: JSON.stringify({ reason }) }),
  approve: (id: number, reason: string) =>
    api<ClaimView>(`/api/admin/claims/${id}/approve`, { method: "POST", body: JSON.stringify({ reason }) }),
  reject: (id: number, reason: string) =>
    api<ClaimView>(`/api/admin/claims/${id}/reject`, { method: "POST", body: JSON.stringify({ reason }) }),
  issueOtp: (claimId: number) =>
    api<{ otpStatus: string; message: string }>(`/api/admin/handovers/${claimId}/issue-otp`, { method: "POST" }),
  verifyOtp: (claimId: number, otp: string) =>
    api<{ status: string }>(`/api/admin/handovers/${claimId}/verify-otp`, {
      method: "POST",
      body: JSON.stringify({ otp }),
    }),
  reportStatus: (id: number, status: ReportStatus) =>
    api<ReportCard>(`/api/admin/reports/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
  audit: () => api<AuditView[]>("/api/admin/audit"),
};

export const CATEGORY_LABEL: Record<ItemCategory, string> = {
  ELECTRONICS: "Electronics",
  IDENTITY_CARDS: "Identity cards",
  BOOKS_AND_STATIONERY: "Books and stationery",
  BAGS: "Bags",
  CLOTHING: "Clothing",
  KEYS: "Keys",
  ACCESSORIES: "Accessories",
  OTHER: "Other",
};
