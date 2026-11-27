
export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: corsHeaders()
      });
    }

    if (request.method !== "POST") {
      return Response.json(
        { error: "Method not allowed" },
        { status: 405, headers: corsHeaders() }
      );
    }

    try {
      const { text } = await request.json();

      if (!text || typeof text !== "string" || text.length > 2500) {
        return Response.json(
          { error: "Invalid text" },
          { status: 400, headers: corsHeaders() }
        );
      }

      const response = await fetch(
        `https://api.elevenlabs.io/v1/text-to-speech/${env.ELEVENLABS_VOICE_ID}`,
        {
          method: "POST",
          headers: {
            "xi-api-key": env.ELEVENLABS_API_KEY,
            "Content-Type": "application/json",
            "Accept": "audio/mpeg"
          },
          body: JSON.stringify({
            text,
            model_id: "eleven_multilingual_v2"
          })
        }
      );

      if (!response.ok) {
        return Response.json(
          { error: "ElevenLabs speech generation failed" },
          { status: response.status, headers: corsHeaders() }
        );
      }

      return new Response(response.body, {
        headers: {
          ...corsHeaders(),
          "Content-Type": "audio/mpeg",
          "Cache-Control": "no-store"
        }
      });
    } catch {
      return Response.json(
        { error: "Invalid request" },
        { status: 400, headers: corsHeaders() }
      );
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
