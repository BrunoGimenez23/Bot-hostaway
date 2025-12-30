import { useEffect, useMemo, useState } from "react";
import axios from "axios";

export default function BotDemo() {
  const [listingId, setListingId] = useState("");
  const [message, setMessage] = useState("¿A qué hora es el check-in?");
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);

  // Estado real de conexión
  const [connected, setConnected] = useState(false);

  const API_URL = import.meta.env.VITE_API_URL;

  // ===== Verificar estado backend (DEMO vs HOSTAWAY) =====
  useEffect(() => {
    const checkHealth = async () => {
      try {
        const res = await axios.get(`${API_URL}/api/health`);
        setConnected(res?.data?.mode === "HOSTAWAY");
      } catch {
        setConnected(false);
      }
    };

    if (API_URL) checkHealth();
  }, [API_URL]);

  const canAsk = useMemo(() => {
    const idNum = Number(listingId);
    const idOk = listingId.trim() !== "" && !Number.isNaN(idNum) && idNum > 0;
    const msgOk = message.trim().length > 0;
    return idOk && msgOk && !loading;
  }, [listingId, message, loading]);

  const statusLabel = connected ? "CONECTADO A HOSTAWAY" : "DEMO";
  const statusStyle = connected
    ? "bg-emerald-100 text-emerald-800 border-emerald-200"
    : "bg-amber-100 text-amber-800 border-amber-200";

  const examples = [
    "¿A qué hora es el check-in?",
    "¿A qué hora es el check-out?",
    "¿Puedo entrar antes (early check-in)?",
    "¿Puedo salir más tarde (late check-out)?",
    "¿Cómo entro al apartamento? / ¿Cómo retiro las llaves?",
    "¿Dónde queda la propiedad? / ¿Cuál es la dirección?",
    "¿Se puede fumar?",
    "¿Hay wifi?",
    "¿Se permiten fiestas?",
    "¿Dónde puedo dejar el auto estacionado?",
    "¿El precio incluye todos los servicios?",
    "¿El alojamiento incluye toallas, sábanas y secador de pelo?",
    "¿Hay sombrilla para la playa?",
    "Hola, te acabo de reservar para la noche del jueves.",
    "Llego mañana 21:40, ¿puedo hacer el check-in tipo 23:00?",
    "¿El apartamento está disponible para hoy?",
  ];

  const handleAsk = async () => {
    const idNum = Number(listingId);

    if (!listingId.trim() || Number.isNaN(idNum) || idNum <= 0) {
      setAnswer("Ingresá un listingId válido (número mayor a 0).");
      return;
    }
    if (!message.trim()) {
      setAnswer("Escribí una pregunta.");
      return;
    }

    try {
      setLoading(true);
      setAnswer("");

      const start = performance.now();
      const res = await axios.post(`${API_URL}/api/bot/answer`, {
        listingId: idNum,
        message: message.trim(),
        language: "es",
      });
      const ms = Math.round(performance.now() - start);

      setAnswer(`${res.data.answer}\n\n— Tiempo de respuesta: ${ms} ms`);
    } catch (err) {
      const apiMsg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Error al consultar el bot.";
      setAnswer(typeof apiMsg === "string" ? apiMsg : "Error al consultar el bot.");
    } finally {
      setLoading(false);
    }
  };

  const onMessageKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      if (canAsk) handleAsk();
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-slate-100 py-10 px-4">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* ===== Header ===== */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
          <div className="flex justify-between items-start gap-4">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">
                Hostaway Guest Bot
              </h1>
              <p className="text-sm text-slate-600 mt-1">
                Demo de atención automática a huéspedes basada en datos reales.
              </p>
            </div>

            <div
              className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full border text-xs font-semibold ${statusStyle}`}
            >
              <span
                className={`h-2 w-2 rounded-full ${
                  connected ? "bg-emerald-500" : "bg-amber-500"
                }`}
              />
              {statusLabel}
            </div>
          </div>
        </div>

        {/* ===== Form ===== */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="text-sm font-semibold text-slate-800">
                Listing ID
              </label>
              <input
                type="number"
                className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 focus:ring-2 focus:ring-blue-200"
                value={listingId}
                onChange={(e) => setListingId(e.target.value)}
                placeholder="Ej: 430409"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="text-sm font-semibold text-slate-800">
                Pregunta del huésped
              </label>
              <textarea
                className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 min-h-[110px] focus:ring-2 focus:ring-blue-200"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={onMessageKeyDown}
              />
              <div className="flex justify-between text-xs text-slate-500 mt-2">
                <span>Ctrl + Enter para enviar</span>
                <span>Idioma: ES</span>
              </div>
            </div>
          </div>

          {/* ===== Examples ===== */}
          <div className="border border-slate-200 rounded-xl p-4 bg-slate-50">
            <div className="text-sm font-semibold mb-2">
              Preguntas sugeridas (Fase 1)
            </div>
            <div className="flex flex-wrap gap-2">
              {examples.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => setMessage(q)}
                  className="text-xs px-3 py-1.5 rounded-full border bg-white hover:bg-slate-100"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>

          {/* ===== CTA ===== */}
          <button
            onClick={handleAsk}
            disabled={!canAsk}
            className={`w-full rounded-xl px-4 py-3 font-semibold text-white ${
              canAsk ? "bg-blue-600 hover:bg-blue-700" : "bg-blue-300"
            }`}
          >
            {loading ? "Consultando..." : "Preguntar al Bot"}
          </button>
        </div>

        {/* ===== Answer ===== */}
        {answer && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <h2 className="text-lg font-bold mb-3">Respuesta</h2>
            <pre className="whitespace-pre-wrap text-slate-800">{answer}</pre>
          </div>
        )}

        <div className="text-center text-xs text-slate-500">
          Fase 1: Demo + lógica. Fase 2: Integración con Airbnb / Booking / WhatsApp.
        </div>
      </div>
    </div>
  );
}
