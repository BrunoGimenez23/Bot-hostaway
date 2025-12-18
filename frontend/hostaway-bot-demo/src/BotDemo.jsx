import { useMemo, useState } from "react";
import axios from "axios";

export default function BotDemo() {
  // Mantener honesto: si no está conectado realmente por API, dejalo en false
  const SHOW_CONNECTED_UI = false;

  const [listingId, setListingId] = useState(""); // mejor vacío para que el usuario lo ingrese
  const [message, setMessage] = useState("¿A qué hora es el check-in?");
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);

  const API_URL = import.meta.env.VITE_API_URL;

  const canAsk = useMemo(() => {
    const idNum = Number(listingId);
    const idOk = listingId.trim() !== "" && !Number.isNaN(idNum) && idNum > 0;
    const msgOk = message.trim().length > 0;
    return idOk && msgOk && !loading;
  }, [listingId, message, loading]);

  const statusLabel = SHOW_CONNECTED_UI ? "CONECTADO A HOSTAWAY" : "DEMO";
  const statusStyle = SHOW_CONNECTED_UI
    ? "bg-emerald-100 text-emerald-800 border-emerald-200"
    : "bg-amber-100 text-amber-800 border-amber-200";

  // Preguntas que CIERRAN Fase 1 (horarios + acceso + dirección + reglas + amenities)
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
  // NUEVAS (basadas en chats reales)
  "¿Dónde puedo dejar el auto estacionado? ¿Hay parking cerca?",
  "¿El precio incluye todos los servicios (luz, agua, gas, internet)?",
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
      console.error(err);
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
        {/* Header */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
                Hostaway Guest Bot
              </h1>
              <p className="text-sm text-slate-600 mt-1">
                Demo de atención a huéspedes basada en datos de la propiedad (reglas, horarios, amenities).
              </p>
            </div>

            <div
              className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full border text-xs font-semibold ${statusStyle}`}
            >
              <span
                className={`h-2 w-2 rounded-full ${
                  SHOW_CONNECTED_UI ? "bg-emerald-500" : "bg-amber-500"
                }`}
              />
              {statusLabel}
            </div>
          </div>

          {/* Nota de presentación (opcional) */}
          {SHOW_CONNECTED_UI && (
            <div className="mt-4 text-xs text-slate-500">
              Estado mostrado como conectado solo para visualizar el flujo final.
            </div>
          )}
        </div>

        {/* Form */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-1">
              <label className="text-sm font-semibold text-slate-800">
                Listing ID
              </label>
              <input
                type="number"
                className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-200"
                value={listingId}
                onChange={(e) => setListingId(e.target.value)}
                placeholder="Ej: 12345"
              />
              <p className="text-xs text-slate-500 mt-2">
                Identificador de la propiedad en Hostaway.
              </p>
            </div>

            <div className="sm:col-span-2">
              <label className="text-sm font-semibold text-slate-800">
                Pregunta del huésped
              </label>
              <textarea
                className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 min-h-[110px] focus:outline-none focus:ring-2 focus:ring-blue-200"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={onMessageKeyDown}
                placeholder="Ej: ¿A qué hora es el check-in?"
              />
              <div className="flex items-center justify-between mt-2">
                <p className="text-xs text-slate-500">Tip: Ctrl+Enter para enviar</p>
                <div className="text-xs text-slate-500">
                  Idioma: <span className="font-semibold text-slate-700">ES</span>
                </div>
              </div>
            </div>
          </div>

          {/* Examples */}
          <div className="border border-slate-200 rounded-xl p-4 bg-slate-50">
            <div className="text-sm font-semibold text-slate-800 mb-2">
              Preguntas sugeridas (Fase 1)
            </div>
            <div className="flex flex-wrap gap-2">
              {examples.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => setMessage(q)}
                  className="text-xs px-3 py-1.5 rounded-full border border-slate-200 bg-white hover:bg-slate-100 text-slate-700"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>

          {/* CTA */}
          <button
            onClick={handleAsk}
            disabled={!canAsk}
            className={`w-full rounded-xl px-4 py-3 font-semibold text-white transition ${
              canAsk ? "bg-blue-600 hover:bg-blue-700" : "bg-blue-300 cursor-not-allowed"
            }`}
          >
            {loading ? "Consultando y generando respuesta..." : "Preguntar al Bot"}
          </button>
        </div>

        {/* Answer */}
        {answer && (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-bold text-slate-900">Respuesta</h2>
              <span className="text-xs px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
                Listing: {listingId || "-"}
              </span>
            </div>
            <div className="mt-3 whitespace-pre-wrap text-slate-800 leading-relaxed">
              {answer}
            </div>
          </div>
        )}

        {/* Footer note */}
        <div className="text-center text-xs text-slate-500">
          Fase 1: Demo + lógica de respuestas (horarios, acceso, dirección, reglas). Fase 2: Integración con canales (Airbnb/Booking/WhatsApp).
        </div>
      </div>
    </div>
  );
}
