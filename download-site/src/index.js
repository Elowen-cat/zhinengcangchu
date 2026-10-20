const APK_PATH = "/smart-warehouse-1.8.0.apk";
const APK_SIZE = 25893866;
const PARTS = [
  "part-00.bin",
  "part-01.bin",
  "part-02.bin",
  "part-03.bin",
  "part-04.bin",
  "part-05.bin",
  "part-06.bin"
];

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== APK_PATH) return env.ASSETS.fetch(request);

    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405, headers: { Allow: "GET, HEAD" } });
    }

    const headers = {
      "Content-Type": "application/vnd.android.package-archive",
      "Content-Disposition": 'attachment; filename="smart-warehouse-1.8.0.apk"',
      "Content-Length": String(APK_SIZE),
      "Cache-Control": "public, max-age=3600",
      "X-Content-Type-Options": "nosniff"
    };
    if (request.method === "HEAD") return new Response(null, { headers });

    let partIndex = 0;
    let reader = null;
    const body = new ReadableStream({
      async pull(controller) {
        try {
          while (true) {
            if (!reader) {
              if (partIndex >= PARTS.length) {
                controller.close();
                return;
              }
              const partUrl = new URL(`/parts/${PARTS[partIndex]}`, request.url);
              const part = await env.ASSETS.fetch(new Request(partUrl));
              if (!part.ok || !part.body) throw new Error(`Download part ${partIndex} is unavailable`);
              reader = part.body.getReader();
            }
            const { done, value } = await reader.read();
            if (done) {
              reader = null;
              partIndex++;
              continue;
            }
            controller.enqueue(value);
            return;
          }
        } catch (error) {
          controller.error(error);
        }
      },
      async cancel(reason) {
        if (reader) await reader.cancel(reason);
      }
    });

    return new Response(body, { headers });
  }
};
