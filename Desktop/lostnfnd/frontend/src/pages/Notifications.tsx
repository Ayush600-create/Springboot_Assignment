import { useEffect, useState } from "react";
import { campusApi, type NotificationView } from "../api";
import { formatWhen } from "../format";
import { Button, Card, Empty, ErrorState, Skeleton } from "../ui";

export default function Notifications() {
  const [items, setItems] = useState<NotificationView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setError(null);
    campusApi.notifications().then(setItems).catch((e) => setError(e.message));
  };
  useEffect(load, []);

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!items) return <Skeleton className="h-40" />;
  if (items.length === 0) return <Empty title="No notifications" body="Match and claim updates will appear here." />;

  return (
    <div className="space-y-4">
      <h1 className="font-display text-3xl">Notifications</h1>
      {items.map((n) => (
        <Card key={n.id} className={n.read ? "opacity-70" : ""}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-medium">{n.title}</p>
              <p className="text-sm text-slate-400">{n.body}</p>
              <p className="mt-1 text-xs text-slate-500">{formatWhen(n.createdAt)}</p>
            </div>
            {!n.read && (
              <Button variant="secondary" onClick={async () => {
                await campusApi.readNotification(n.id);
                load();
              }}>Mark read</Button>
            )}
          </div>
        </Card>
      ))}
    </div>
  );
}
