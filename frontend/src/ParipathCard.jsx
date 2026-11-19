import { useEffect, useState } from "react";
import { CalendarDays, Mic2, Newspaper, Sparkles, Sun } from "lucide-react";
import { classApi, messageFromError } from "./api.js";

export default function ParipathCard() {
  const [paripath, setParipath] = useState(null);
  const [error, setError] = useState("");
  const [speaking, setSpeaking] = useState(false);

  async function load() {
    try {
      const response = await classApi.paripathToday();
      setParipath(response.data);
      setError("");
    } catch (requestError) {
      setError(messageFromError(requestError));
    }
  }

  useEffect(() => {
    load();
    return () => {
      if ("speechSynthesis" in window) window.speechSynthesis.cancel();
    };
  }, []);

  function practice() {
    if (!("speechSynthesis" in window)) {
      setError("Read-aloud is not supported in this browser. You can still read the assembly script below.");
      return;
    }
    if (speaking) {
      window.speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }
    const script = [
      paripath.suvichar,
      `Meaning: ${paripath.suvicharMeaning}`,
      ...(paripath.todaysNews || []),
      paripath.specialDayInfo,
      paripath.gkQuestion,
      paripath.assemblyRoleAssignment
    ].filter(Boolean).join(". ");
    const utterance = new SpeechSynthesisUtterance(script);
    utterance.onend = () => setSpeaking(false);
    utterance.onerror = () => setSpeaking(false);
    utterance.onstart = () => setSpeaking(true);
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  if (error) {
    return <div className="error-banner" role="alert">
      <span>{error}</span>
      <button className="text-xs font-extrabold underline" onClick={load}>Retry Paripath</button>
    </div>;
  }
  if (!paripath?.isParipathDay) return null;

  return <section className="surface-card mt-7 overflow-hidden border-[#dce9d5]">
    <div className="bg-[#c45100] p-5 text-white sm:p-7">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <span className="flex items-center gap-2 text-[10px] font-black tracking-[.18em] text-[#ffe0bb]"><Sun size={15} /> CLASS 7 PARIPATH</span>
          <h2 className="mt-2 text-2xl font-black">A little stage, a big bright voice.</h2>
          <p className="mt-1 flex items-center gap-1.5 text-xs text-white/75"><CalendarDays size={14} /> {paripath.date} · {paripath.dayOfWeek?.toLowerCase()}</p>
        </div>
        <button className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-white px-4 text-xs font-black text-[#9b3e00] hover:bg-[#fff1e2]" onClick={practice}>
          <Mic2 size={16} /> {speaking ? "Stop practice" : "Read aloud / practice"}
        </button>
      </div>
      <div className="mt-5 grid gap-3 md:grid-cols-2">
        <article className="rounded-2xl bg-white/10 p-4">
          <h3 className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-[#ffe0bb]"><Sparkles size={15} /> Aaj ka Suvichar</h3>
          <blockquote className="mt-3 text-lg font-bold leading-7">“{paripath.suvichar}”</blockquote>
          <p className="mt-2 text-sm leading-6 text-white/80"><b>Simple meaning:</b> {paripath.suvicharMeaning}</p>
        </article>
        <article className="rounded-2xl bg-white/10 p-4">
          <h3 className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-[#ffe0bb]"><Newspaper size={15} /> Aaj ke Mukhya Samachar</h3>
          <ul className="mt-3 list-disc space-y-2 pl-5 text-sm leading-5 text-white/85">
            {(paripath.todaysNews || []).map((headline, index) => <li key={`${index}-${headline}`}>{headline}</li>)}
          </ul>
        </article>
        <article className="rounded-2xl bg-white/10 p-4">
          <h3 className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-[#ffe0bb]"><Sun size={15} /> Din Vishesh</h3>
          <p className="mt-3 text-sm leading-6 text-white/85">{paripath.specialDayInfo}</p>
        </article>
        {paripath.gkQuestion && <article className="rounded-2xl bg-white/10 p-4">
          <h3 className="text-xs font-black uppercase tracking-wide text-[#ffe0bb]">GK Question</h3>
          <p className="mt-3 whitespace-pre-line text-sm leading-6 text-white/85">{paripath.gkQuestion}</p>
        </article>}
        {paripath.assemblyRoleAssignment && <article className="rounded-2xl bg-white/10 p-4">
          <h3 className="text-xs font-black uppercase tracking-wide text-[#ffe0bb]">Assembly roles</h3>
          <p className="mt-3 whitespace-pre-line text-sm leading-6 text-white/85">{paripath.assemblyRoleAssignment}</p>
        </article>}
      </div>
      {paripath.teacherReviewRecommended && <p className="mt-4 rounded-xl bg-[#fff4d8] px-3 py-2 text-xs font-semibold leading-5 text-[#694c18]">
        AI suggestions need teacher review. Generated headlines are learning prompts, not verified live news.
      </p>}
    </div>
  </section>;
}
