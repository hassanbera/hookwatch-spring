import { test } from "node:test";
import assert from "node:assert/strict";
import { randomUUID, createHmac } from "node:crypto";
import { createServer } from "node:http";
const base = process.env.TEST_API;
if (!base)
  throw new Error(
    "TEST_API must point to a disposable test API/database; see README.",
  );
async function request(method, path, body, status = 200) {
  const r = await fetch(base + "/" + path, {
    method,
    headers: { "Content-Type": "application/json" },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const j = await r.json();
  assert.equal(r.status, status, JSON.stringify(j));
  return j.data;
}
const get = (p) => request("GET", p);
const post = (p, b, status) => request("POST", p, b, status);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
test("database health", async () =>
  assert.equal((await get("health")).status, "ok"));
test("unknown ID is rejected", async () => {
  const r = await fetch(base + "/deliveries/" + randomUUID());
  assert.ok([404, 405].includes(r.status));
});
test("real signed HTTP delivery, bounded retries and manual requeue", async () => {
  const origin = process.env.TEST_WEBHOOK_ORIGIN;
  if (!origin) throw new Error("TEST_WEBHOOK_ORIGIN required");
  const secret =
    process.env.WEBHOOK_SIGNING_SECRET ||
    "local-signing-key-change-before-deploy";
  let seen = 0;
  let signatures = 0;
  let mode = "transient";
  const server = createServer(async (req, res) => {
    let body = "";
    for await (const b of req) body += b;
    const ts = req.headers["x-hookwatch-timestamp"];
    const signature =
      "sha256=" +
      createHmac("sha256", secret)
        .update(ts + "." + body)
        .digest("hex");
    assert.equal(req.headers["x-hookwatch-signature"], signature);
    assert.ok(req.headers["x-hookwatch-id"]);
    signatures++;
    seen++;
    res.writeHead(
      mode === "permanent" ? 400 : mode === "transient" && seen < 3 ? 503 : 200,
    );
    res.end("ok");
  });
  await new Promise((r) =>
    server.listen(Number(new URL(origin).port), "127.0.0.1", r),
  );
  try {
    await post(
      "endpoints",
      { name: "Forbidden", url: "http://169.254.169.254/latest/meta-data" },
      400,
    );
    const endpoint = await post("endpoints", {
      name: "Test receiver",
      url: origin + "/receiver",
    });
    async function wait(id, status) {
      for (let n = 0; n < 250; n++) {
        const d = await get("deliveries/" + id);
        if (d.delivery.status === status) return d;
        await sleep(200);
      }
      throw new Error("Worker did not reach " + status);
    }
    const d = await post("deliveries", {
      endpointId: endpoint.id,
      eventType: "test.created",
      payload: { test: true },
    });
    const success = await wait(d.id, "DELIVERED");
    assert.equal(success.attempts.length, 3);
    assert.deepEqual(
      success.attempts.map((a) => a.statusCode),
      [503, 503, 200],
    );
    assert.equal(signatures, 3);
    mode = "permanent";
    const bad = await post("deliveries", {
      endpointId: endpoint.id,
      eventType: "test.failed",
      payload: { test: true },
    });
    const failed = await wait(bad.id, "FAILED");
    assert.equal(failed.attempts.length, 1);
    mode = "success";
    await post("deliveries/" + bad.id + "/retry");
    const retried = await wait(bad.id, "DELIVERED");
    assert.equal(retried.attempts.length, 2);
    await post("deliveries/" + d.id + "/retry", {}, 409);
    await post(
      "deliveries",
      { endpointId: endpoint.id, eventType: "bad", payload: "not an object" },
      400,
    );
  } finally {
    await new Promise((r) => server.close(r));
  }
});
