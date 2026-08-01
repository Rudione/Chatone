// Supabase Edge Function: grant-supporter
// Deploy:  supabase functions deploy grant-supporter --no-verify-jwt
// Secrets: supabase secrets set GRANT_WEBHOOK_SECRET=<long-random-string>
//
// Call from your payment provider's webhook (or manually):
//   POST https://zupjweowhttidaxqgxjq.supabase.co/functions/v1/grant-supporter
//   Headers: x-webhook-secret: <GRANT_WEBHOOK_SECRET>
//   Body:    {"twitch_user_id": "123456789"}
//
// Uses the service-role key server-side only — never ship it in the app.

import { createClient } from "jsr:@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("method not allowed", { status: 405 });
  }

  const secret = Deno.env.get("GRANT_WEBHOOK_SECRET");
  if (!secret || req.headers.get("x-webhook-secret") !== secret) {
    return new Response("unauthorized", { status: 401 });
  }

  let body: { twitch_user_id?: string };
  try {
    body = await req.json();
  } catch {
    return new Response("bad json", { status: 400 });
  }

  const userId = (body.twitch_user_id ?? "").trim();
  if (!/^[0-9]{1,32}$/.test(userId)) {
    return new Response("invalid twitch_user_id", { status: 400 });
  }

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );

  const { error } = await supabase
    .from("chatone_badge_users")
    .upsert({ user_id: userId, badge_id: "supporter" });

  if (error) {
    return new Response(JSON.stringify({ ok: false, error: error.message }), {
      status: 500,
      headers: { "content-type": "application/json" },
    });
  }

  return new Response(JSON.stringify({ ok: true }), {
    headers: { "content-type": "application/json" },
  });
});
