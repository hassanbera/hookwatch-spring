import {
  Component,
  signal,
  computed,
  inject,
  provideZonelessChangeDetection,
} from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
} from "@angular/common/http";
import { firstValueFrom } from "rxjs";
interface Entity {
  id: string;
  createdAt: string;
}
class Api {
  private http = inject(HttpClient);
  async get<T>(path: string): Promise<T> {
    return (await firstValueFrom(this.http.get<{ data: T }>("/api/" + path)))
      .data;
  }
  async send<T>(method: string, path: string, body: unknown = {}): Promise<T> {
    return (
      await firstValueFrom(
        this.http.request<{ data: T }>(method, "/api/" + path, { body }),
      )
    ).data;
  }
}
interface Endpoint extends Entity {
  name: string;
  url: string;
}
interface Delivery extends Entity {
  endpointId: string;
  eventType: string;
  payload: string;
  status: string;
  attemptCount: number;
  cycleAttempts: number;
  nextAttemptAt: string;
}
interface Attempt extends Entity {
  number: number;
  statusCode: number | null;
  durationMs: number;
  error: string;
}
interface DeliveryDetail {
  delivery: Delivery;
  attempts: Attempt[];
}
@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./app.html",
  host: { "(document:keydown.escape)": "close()" },
})
class AppComponent {
  error = signal("");
  notice = signal("");
  busy = signal(false);
  panel = signal("");
  query = signal("");
  api = new Api();
  async run(fn: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set("");
    this.notice.set("");
    try {
      await fn();
    } catch (e) {
      this.error.set(
        e instanceof HttpErrorResponse
          ? e.error?.error ||
              "Sunucuya ulaşılamadı. API bağlantısını kontrol edin."
          : e instanceof Error
            ? e.message
            : "İşlem tamamlanamadı.",
      );
    } finally {
      this.busy.set(false);
    }
  }
  close() {
    this.panel.set("");
  }
  money(c: number) {
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(c / 100);
  }
  date(d: string) {
    return new Date(d).toLocaleDateString("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  }
  time(d: string) {
    return new Date(d).toLocaleTimeString("tr-TR", {
      hour: "2-digit",
      minute: "2-digit",
    });
  }
  endpoints = signal<Endpoint[]>([]);
  deliveries = signal<Delivery[]>([]);
  detail = signal<DeliveryDetail | null>(null);
  filter = signal("");
  timer: ReturnType<typeof setInterval>;
  endpoint = {
    name: "",
    url: location.protocol + "//127.0.0.1:4205/api/demo/receiver",
  };
  event = {
    endpointId: "",
    eventType: "order.created",
    payload: '{\n  "orderId": "ORD-1042",\n  "total": 2490\n}',
  };
  visible = computed(() =>
    this.deliveries().filter(
      (d) =>
        (!this.filter() || d.status === this.filter()) &&
        (d.eventType + " " + d.id)
          .toLowerCase()
          .includes(this.query().toLowerCase()),
    ),
  );
  delivered = computed(
    () => this.deliveries().filter((d) => d.status === "DELIVERED").length,
  );
  failed = computed(
    () => this.deliveries().filter((d) => d.status === "FAILED").length,
  );
  constructor() {
    void this.run(() => this.load());
    this.timer = setInterval(() => {
      if (!this.busy() && !this.panel()) void this.run(() => this.load());
    }, 5000);
  }
  ngOnDestroy() {
    clearInterval(this.timer);
  }
  async load() {
    this.endpoints.set(await this.api.get<Endpoint[]>("endpoints"));
    this.deliveries.set(await this.api.get<Delivery[]>("deliveries"));
    if (!this.event.endpointId)
      this.event.endpointId = this.endpoints()[0]?.id || "";
    if (this.detail())
      this.detail.set(
        await this.api.get<DeliveryDetail>(
          "deliveries/" + this.detail()!.delivery.id,
        ),
      );
  }
  name(id: string) {
    return this.endpoints().find((e) => e.id === id)?.name || "Hedef";
  }
  select(id: string) {
    void this.run(async () => {
      this.detail.set(await this.api.get<DeliveryDetail>("deliveries/" + id));
    });
  }
  saveEndpoint() {
    void this.run(async () => {
      const e = await this.api.send<Endpoint>(
        "POST",
        "endpoints",
        this.endpoint,
      );
      await this.load();
      this.event.endpointId = e.id;
      this.close();
      this.endpoint = { ...this.endpoint, name: "" };
      this.notice.set("Hedef eklendi.");
    });
  }
  saveEvent() {
    void this.run(async () => {
      let payload: unknown;
      try {
        payload = JSON.parse(this.event.payload);
      } catch {
        throw new Error("Geçerli bir JSON nesnesi girin.");
      }
      const d = await this.api.send<Delivery>("POST", "deliveries", {
        ...this.event,
        payload,
      });
      await this.load();
      this.close();
      this.detail.set(await this.api.get<DeliveryDetail>("deliveries/" + d.id));
    });
  }
  retry(id: string) {
    void this.run(async () => {
      await this.api.send("POST", "deliveries/" + id + "/retry");
      await this.load();
    });
  }
  pretty(s: string) {
    try {
      return JSON.stringify(JSON.parse(s), null, 2);
    } catch {
      return s;
    }
  }
}
bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(), provideZonelessChangeDetection()],
}).catch(console.error);
