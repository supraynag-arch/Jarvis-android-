export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method !== "POST") {
      return Response.json({ error: "Method not allowed" }, { status: 405, headers: corsHeaders() });
    }

    try {
      const body = await request.json();
      const message = typeof body.message === "string" ? body.message.trim() : "";
      if (!message || message.length > 8000) {
        return Response.json({ error: "Message must contain 1–8000 characters." }, { status: 400, headers: corsHeaders() });
      }
      if (!env.GROQ_API_KEY) {
        return Response.json({ error: "The AI service is not configured." }, { status: 503, headers: corsHeaders() });
      }

      const upstream = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${env.GROQ_API_KEY}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: "llama-3.3-70b-versatile",
          temperature: 0.7,
          max_tokens: 900,
          messages: [
            {
              role: "system",
              content: "You are JARVIS, a helpful, capable Android personal assistant. Be clear and concise. Do not claim to have performed actions on the phone unless the app confirms that action. Ask for confirmation before sensitive actions."
            },
            { role: "user", content: message }
          ]
        })
      });

      const result = await upstream.json();
      if (!upstream.ok) {
        return Response.json(
          { error: "The AI provider returned an error.", details: result?.error?.message || "Request failed." },
          { status: upstream.status, headers: corsHeaders() }
        );
      }

      const reply = result?.choices?.[0]?.message?.content;
      if (typeof reply !== "string" || !reply.trim()) {
        return Response.json({ error: "The AI returned an empty response." }, { status: 502, headers: corsHeaders() });
      }

      return Response.json({ reply: reply.trim() }, { headers: corsHeaders() });
    } catch {
      return Response.json({ error: "Invalid request or AI service unavailable." }, { status: 400, headers: corsHeaders() });
    }
  }
};

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Cache-Control": "no-store"
  };
}
