import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowDownLeft, ArrowLeft, ArrowRight, ArrowUpRight, BookOpen, Check,
  CalendarDays, CheckCircle2, ChevronDown, Clock3, GraduationCap, Heart,
  HelpCircle, KeyRound, Leaf, LockKeyhole, LogOut, MessageCircle, Plus,
  Copy, QrCode, Search, Send, ShieldCheck, Sparkles, Sun, Trash2, Users, X
} from "lucide-react";
import QRCode from "qrcode";
import { aiApi, authApi, classApi, lockerApi, messageFromError, parentApi, teacherApi } from "./api.js";
import ParipathCard from "./ParipathCard.jsx";

const dayLabels = {
  MONDAY: "Monday", TUESDAY: "Tuesday", WEDNESDAY: "Wednesday",
  THURSDAY: "Thursday", FRIDAY: "Friday", SATURDAY: "Saturday", SUNDAY: "Sunday"
};
const todayISO = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
};

function App() {
  const [session, setSession] = useState(() => {
    try { return JSON.parse(localStorage.getItem("locker-session") || "null"); } catch { return null; }
  });
  const [screen, setScreen] = useState(() => {
    if (new URLSearchParams(location.search).get("screen") === "qr") return "qr";
    if (session?.role === "PARENT") return "parent";
    if (session?.role === "STUDENT") return "student";
    if (session?.role === "TEACHER") return "teacher";
    return "home";
  });

  useEffect(() => {
    const handleUnauthorized = () => {
      localStorage.removeItem("locker-session");
      localStorage.removeItem("locker-token");
      setSession(null);
      setScreen("home");
    };
    window.addEventListener("locker:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("locker:unauthorized", handleUnauthorized);
  }, []);

  useEffect(() => {
    if (session?.token) localStorage.setItem("locker-token", session.token);
    else {
      localStorage.removeItem("locker-token");
      localStorage.removeItem("locker-session");
    }
  }, [session]);

  const signIn = (data) => {
    localStorage.setItem("locker-session", JSON.stringify(data));
    setSession(data);
    setScreen(data.role === "PARENT" ? "parent" : data.role === "TEACHER" ? "teacher" : "student");
  };
  const signOut = () => { setSession(null); setScreen("home"); };

  if (screen === "qr") return <QrPage onBack={() => setScreen("home")} />;
  if (session?.role === "STUDENT" && screen !== "home") return <StudentDashboard session={session} onSignOut={signOut} />;
  if (session?.role === "PARENT" && screen !== "home") return <ParentDashboard session={session} onSignOut={signOut} />;
  if (session?.role === "TEACHER" && screen !== "home") return <TeacherDashboard session={session} onSignOut={signOut} />;
  return <Landing onSignIn={signIn} onQr={() => setScreen("qr")} />;
}

function Brand({ compact = false }) {
  return <div className="flex items-center gap-3">
    <div className="brand-mark"><LockKeyhole size={19} strokeWidth={2.2} /></div>
    {!compact && <div><p className="text-sm font-extrabold tracking-tight text-ink">Class 7 <span className="text-forest">Locker</span></p><p className="text-[10px] font-semibold tracking-[.17em] text-muted">SMARTER SCHOOL DAYS</p></div>}
  </div>;
}

function Landing({ onSignIn, onQr }) {
  const [mode, setMode] = useState(null);
  const [parentRegister, setParentRegister] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [registeredIdentity, setRegisteredIdentity] = useState("");

  async function submit(event) {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    if (mode === "parent" && parentRegister
      && !String(form.get("email") || "").trim()
      && !String(form.get("mobile") || "").trim()) {
      setError("Enter an email address or mobile number to create your parent account.");
      return;
    }
    setBusy(true);
    try {
      let response;
      if (mode === "student") {
        response = await authApi.studentLogin({ rollNumber: form.get("rollNumber"), specialPassword: form.get("password") });
      } else if (mode === "teacher") {
        response = await authApi.teacherLogin({ accessKey: form.get("accessKey") });
      } else if (parentRegister) {
        response = await authApi.parentRegister({
          email: form.get("email") || null,
          mobile: form.get("mobile") || null,
          password: form.get("password"),
          rollNumber: form.get("rollNumber"),
          specialPassword: form.get("specialPassword")
        });
      } else {
        response = await authApi.parentLogin({ identity: form.get("identity"), password: form.get("password") });
      }
      onSignIn(response.data);
    } catch (requestError) {
      const registrationError = requestError?.response?.data?.error || "";
      if (mode === "parent" && parentRegister && requestError?.response?.status === 409
        && /already registered/i.test(registrationError)) {
        const duplicateIsMobile = /mobile/i.test(registrationError);
        const identity = String(form.get(duplicateIsMobile ? "mobile" : "email") || "").trim();
        setRegisteredIdentity(identity);
        setError(duplicateIsMobile
          ? "This mobile number already has an account. Sign in with it below."
          : "This email address already has an account. Sign in with it below.");
      } else {
        setError(messageFromError(requestError));
      }
    } finally { setBusy(false); }
  }

  return <main className="min-h-screen bg-paper px-5 pb-10 pt-5 sm:px-8 sm:pt-8">
    <header className="mx-auto flex max-w-6xl items-center justify-between"><Brand /><button className="subtle-button" onClick={onQr}><ArrowUpRight size={16} /> Portal address</button></header>
    <section className="mx-auto grid max-w-6xl items-center gap-12 pb-12 pt-12 lg:grid-cols-[1.05fr_.95fr] lg:pb-20 lg:pt-20">
      <div className="max-w-xl">
        <div className="eyebrow"><span className="eyebrow-dot" /> MADE FOR CLASS 7</div>
        <h1 className="landing-title mt-6 text-[clamp(3rem,8vw,5.7rem)] font-black leading-[.99] tracking-[-.065em] text-ink"><span className="headline-orange">A lighter bag.</span><br /><span className="text-forest">A brighter</span><br />school day.</h1>
        <p className="mt-6 max-w-md text-base leading-7 text-muted sm:text-lg">Your locker knows what you need today. Pack the right books, keep the rest safe, and leave the heavy lifting behind.</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <div className="trust-chip"><CheckCircle2 size={16} /> Your own school checklist</div>
          <div className="trust-chip"><Heart size={16} /> Parents stay in the loop</div>
        </div>
        <div className="mt-10 flex items-center gap-3 text-xs font-medium text-muted"><div className="flex -space-x-2"><span className="avatar-dot bg-[#d8f36a]">S</span><span className="avatar-dot bg-[#c4e6d9]">P</span><span className="avatar-dot bg-[#f7d8b5]">7</span></div> One simple portal for students and parents</div>
      </div>
      <div className="relative mx-auto w-full max-w-[470px]">
        <div className="hero-blob" />
        <div className="hero-card relative">
          <div className="flex items-start justify-between"><div><p className="text-xs font-bold uppercase tracking-[.16em] text-muted">Today's school bag</p><p className="mt-1 text-2xl font-black tracking-tight text-ink">Looking lighter ✨</p></div><div className="hero-icon"><Leaf size={22} /></div></div>
          <div className="mt-7 space-y-3">
            <div className="demo-item"><span className="demo-book"><BookOpen size={17} /></span><span className="flex-1"><strong>Science Book</strong><small>Homework today</small></span><span className="take-tag">TAKE HOME</span><Check size={16} className="text-forest" /></div>
            <div className="demo-item"><span className="demo-book bg-[#f1e7d9] text-[#9b7243]"><BookOpen size={17} /></span><span className="flex-1"><strong>Hindi Book</strong><small>No homework today</small></span><span className="leave-tag">STAYS HERE</span></div>
          </div>
          <div className="mt-6 flex items-center gap-3 rounded-2xl bg-[#f4f8e7] p-4"><span className="thought-icon"><Sparkles size={17} /></span><p className="text-xs leading-5 text-ink"><b>Your day, made simpler</b><br />Pack what today needs. Leave the rest safe.</p></div>
          <div className="mt-5 flex items-center justify-between border-t border-line pt-4 text-xs text-muted"><span className="flex items-center gap-2"><span className="live-dot" /> Locker synced</span><span>Ready for a great day</span></div>
        </div>
        <div className="float-note"><span className="float-note-icon"><Heart size={16} /></span><span><b>Parent connected</b><small>All packed for today</small></span><CheckCircle2 size={18} className="ml-auto text-forest" /></div>
      </div>
    </section>
    <section className="mx-auto max-w-3xl">
      {!mode ? <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <button className="portal-card portal-student" onClick={() => setMode("student")}><span className="portal-icon"><GraduationCap size={24} /></span><span className="flex-1 text-left"><strong>Student Login</strong><small>Sign in with the details provided by your teacher</small></span><ArrowRight size={19} /></button>
        <button className="portal-card portal-parent" onClick={() => setMode("parent")}><span className="portal-icon parent-icon"><Heart size={22} /></span><span className="flex-1 text-left"><strong>Parent Portal</strong><small>See your child's school day</small></span><ArrowRight size={19} /></button>
        <button className="portal-card portal-teacher" onClick={() => setMode("teacher")}><span className="portal-icon teacher-icon"><Users size={24} /></span><span className="flex-1 text-left"><strong>School Staff</strong><small>Teacher dashboard and student accounts</small></span><ArrowRight size={19} /></button>
      </div> : <div className="login-card">
        <div className="flex items-center justify-between"><div><button className="mb-3 inline-flex items-center gap-1 text-xs font-bold text-muted hover:text-forest" onClick={() => { setMode(null); setParentRegister(false); setError(""); }}><ArrowLeft size={14} /> Back</button><h2 className="text-2xl font-black text-ink">{mode === "student" ? "Welcome, student" : mode === "teacher" ? "School staff login" : parentRegister ? "Create parent account" : "Welcome back"}</h2><p className="mt-1 text-sm text-muted">{mode === "student" ? "Sign in with the login ID and password given to you by your teacher." : mode === "teacher" ? "Use the access key configured by your school." : parentRegister ? "Enter an email or mobile number, then link your child." : "Your child's activity is just a sign-in away."}</p></div><div className="login-icon">{mode === "student" ? <GraduationCap /> : mode === "teacher" ? <Users /> : <Heart />}</div></div>
        <form key={`${mode}-${parentRegister}`} className="mt-6 grid gap-4 sm:grid-cols-2" onSubmit={submit}>
          {mode === "teacher" ? <Field label="School teacher access key" name="accessKey" placeholder="Enter your school access key" icon={<KeyRound size={17} />} type="password" autoComplete="current-password" required /> : mode === "student" ? <>
            <Field label="Student roll number" name="rollNumber" placeholder="e.g. C7-101" icon={<GraduationCap size={17} />} autoComplete="username" maxLength={20} required />
            <Field label="Student password" name="password" placeholder="Your school password" icon={<KeyRound size={17} />} type="password" autoComplete="current-password" maxLength={72} required />
          </> : parentRegister ? <>
            <Field label="Email (optional)" name="email" placeholder="you@example.com" icon={<MessageCircle size={17} />} type="email" autoComplete="email" />
            <Field label="Mobile (optional)" name="mobile" placeholder="+91 98765 43210" icon={<MessageCircle size={17} />} type="tel" autoComplete="tel" />
            <Field label="Create password" name="password" placeholder="At least 8 characters" icon={<KeyRound size={17} />} type="password" autoComplete="new-password" minLength={8} required />
            <Field label="Child's roll number" name="rollNumber" placeholder="e.g. C7-101" icon={<GraduationCap size={17} />} required />
            <Field label="Child's special password" name="specialPassword" placeholder="To securely link your child" icon={<ShieldCheck size={17} />} type="password" required />
          </> : <>
            <Field label="Email or mobile" name="identity" placeholder="Email or phone number" icon={<MessageCircle size={17} />} autoComplete="username" defaultValue={registeredIdentity} required />
            <Field label="Password" name="password" placeholder="Your password" icon={<KeyRound size={17} />} type="password" autoComplete="current-password" required />
          </>}
          {error && <p className="form-error sm:col-span-2" role="alert">{error}</p>}
          {mode === "parent" && parentRegister && registeredIdentity && error && <button type="button" className="sm:col-span-2 text-left text-sm font-bold text-forest underline" onClick={() => { setParentRegister(false); setError(""); }}>Sign in using {registeredIdentity}</button>}
          <button className="primary-button sm:col-span-2" disabled={busy}>{busy ? "One moment…" : mode === "student" ? "Open my dashboard" : parentRegister ? "Link my child" : "Sign in to Parent Portal"} <ArrowRight size={17} /></button>
        </form>
        {mode === "parent" && <button className="mt-4 w-full text-center text-sm font-bold text-forest hover:underline" onClick={() => { setParentRegister(!parentRegister); setError(""); }}>{parentRegister ? "Already registered? Sign in" : "New here? Register and link your child"}</button>}
      </div>}
    </section>
    <footer className="mx-auto mt-12 flex max-w-6xl flex-col items-center justify-between gap-2 border-t border-line pt-5 text-xs text-muted sm:flex-row"><span>Made for curious minds in Class 7.</span><span className="flex items-center gap-1"><ShieldCheck size={14} /> Your school account stays private</span></footer>
  </main>;
}

function Field({ label, icon, ...props }) {
  return <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">{label}</span><span className="input-wrap">{icon}<input {...props} className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-[#a3ada7]" /></span></label>;
}

function DashboardShell({ session, onSignOut, active = "student", children }) {
  return <main className="min-h-screen bg-paper px-4 pb-24 pt-4 sm:px-7 sm:pt-6">
    <header className="mx-auto flex max-w-6xl items-center justify-between"><Brand /><div className="flex items-center gap-3"><span className="hidden text-right sm:block"><b className="block text-xs text-ink">{session.name}</b><small className="text-[11px] text-muted">{active === "parent" ? "Parent portal" : active === "teacher" ? "School staff portal" : "Student · Class 7"}</small></span><button className="icon-button" title="Sign out" onClick={onSignOut}><LogOut size={17} /></button></div></header>
    {children}
    {active !== "teacher" && <nav className="bottom-nav sm:hidden"><div className="flex items-center justify-around">
      <a href="#overview" className={`bottom-nav-item ${active === "student" ? "selected" : ""}`}><BookOpen size={19} /><span>Locker</span></a>
      <a href="#activity" className={`bottom-nav-item ${active === "parent" ? "selected" : ""}`}><Clock3 size={19} /><span>Activity</span></a>
      <a href="#assistant" className="bottom-nav-item"><Sparkles size={19} /><span>Ask AI</span></a>
    </div></nav>}
  </main>;
}

function StudentDashboard({ session, onSignOut }) {
  const [checklist, setChecklist] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [itemBusy, setItemBusy] = useState(null);
  const [itemActions, setItemActions] = useState({});
  const [adding, setAdding] = useState(false);
  const [dailyInfo, setDailyInfo] = useState(null);

  async function load() {
    try {
      const [todayResult, activityResult, dailyResult] = await Promise.all([
        lockerApi.today(), lockerApi.activityToday(), classApi.dailyInfo()
      ]);
      setChecklist(todayResult.data);
      setDailyInfo(dailyResult.data);
      const latestActions = {};
      for (const log of activityResult.data) {
        if (latestActions[log.itemId] === undefined) latestActions[log.itemId] = log.action;
      }
      setItemActions(latestActions);
      setError("");
    } catch (requestError) { setError(messageFromError(requestError)); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, []);

  async function markItem(item, action) {
    setItemBusy(item.itemId); setError("");
    try {
      await lockerApi.activity({ itemId: item.itemId, action });
      setItemActions((current) => ({ ...current, [item.itemId]: action }));
    } catch (requestError) { setError(messageFromError(requestError)); }
    finally { setItemBusy(null); }
  }

  async function addItem(event) {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
    setAdding(true); setError("");
    try {
      const response = await lockerApi.addItem({ ...data, requiredToday: data.requiredToday === "on" });
      setChecklist((current) => ({ ...current, checklist: [...current.checklist, response.data] }));
      event.currentTarget.reset();
    } catch (requestError) { setError(messageFromError(requestError)); }
    finally { setAdding(false); }
  }

  return <DashboardShell session={session} onSignOut={onSignOut} navigation={[
    { id: "overview", label: "Home", icon: <Sun size={17} /> },
    { id: "locker-checklist", label: "Locker", icon: <BookOpen size={17} /> },
    { id: "student-timetable", label: "Timetable", icon: <CalendarDays size={17} /> },
    { id: "student-updates", label: "Class updates", icon: <Sparkles size={17} /> },
    { id: "student-notes", label: "Teacher notes", icon: <MessageCircle size={17} /> }
  ]}>
    <section className="mx-auto max-w-6xl pb-8 pt-8" id="overview">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end"><div><p className="eyebrow"><span className="eyebrow-dot" /> YOUR CLASS 7 DAY</p><h1 className="mt-3 text-4xl font-black tracking-tight text-ink sm:text-5xl">Hey, {session.name?.split(" ")[0] || "there"} <span className="wave">✦</span></h1><p className="mt-2 text-sm text-muted">Here's your smart school-day plan. You've got this.</p></div><span className="date-pill"><Sun size={16} /> {new Date().toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" })}</span></div>
      {error && <ErrorBanner message={error} onRetry={load} />}
      <ParipathCard />
      <div className="mt-8 grid gap-6 lg:grid-cols-[1.35fr_.65fr]">
        <section className="surface-card p-5 sm:p-7" id="locker-checklist">
          <div className="flex flex-wrap items-start justify-between gap-4"><div><span className="section-kicker">PACK SMARTER</span><h2 className="mt-1 text-2xl font-black tracking-tight text-ink">Your locker checklist</h2><p className="mt-1 text-sm text-muted">Take only what you need for homework today.</p></div><span className="count-pill">{checklist?.checklist?.filter((item) => item.action === "TAKE_HOME").length || 0} to pack</span></div>
          {loading ? <LoadingRows /> : <div className="mt-6 divide-y divide-line">{(checklist?.checklist || []).map((item) => <ChecklistRow key={item.itemId} item={item} busy={itemBusy === item.itemId} selectedAction={itemActions[item.itemId]} onSelect={(action) => markItem(item, action)} />)}{!checklist?.checklist?.length && <EmptyState title="Your locker is waiting" copy="Add your books below to get a smart checklist." />}</div>}
          <form onSubmit={addItem} className="mt-5 grid gap-2 rounded-2xl bg-[#f7f9f6] p-3 sm:grid-cols-[1.2fr_.8fr_.8fr_auto]">
            <input name="name" required maxLength={120} placeholder="Book or notebook" className="mini-input" aria-label="Item name" />
            <select name="category" className="mini-input" aria-label="Category"><option>Book</option><option>Notebook</option><option>Other</option></select>
            <input name="subject" required maxLength={100} placeholder="Subject" className="mini-input" aria-label="Subject" />
            <button disabled={adding} className="add-button"><Plus size={18} /> Add item</button>
            <label className="flex items-center gap-2 px-1 text-xs text-muted sm:col-span-4"><input name="requiredToday" type="checkbox" className="accent-forest" /> Always remind me to take this home</label>
          </form>
        </section>
        <aside className="space-y-5">
          <section className="surface-card p-5 scroll-mt-24" id="student-timetable"><div className="flex items-center justify-between"><div><span className="section-kicker">YOUR DAY</span><h2 className="mt-1 text-xl font-black text-ink">Today's timetable</h2></div><span className="mini-calendar"><BookOpen size={17} /></span></div>
            {loading ? <LoadingRows /> : <div className="mt-5 space-y-3">{(checklist?.timetable || []).map((period, index) => <div key={`${period.period}-${index}`} className="period-row"><span className={`period-dot dot-${index % 4}`} /><span className="flex-1"><b>{period.subject}</b>{period.homeworkRequired && <small>Homework</small>}</span><span className="text-[11px] font-semibold text-muted">{period.period}</span></div>)}{!checklist?.timetable?.length && <p className="py-3 text-sm text-muted">No timetable entries for today yet.</p>}</div>}
          </section>
          {dailyInfo && <section className="surface-card scroll-mt-24 p-5" id="student-updates"><div className="flex items-center gap-3"><span className="mini-calendar"><Sparkles size={17} /></span><div><span className="section-kicker">TODAY IN CLASS</span><h2 className="mt-1 text-lg font-black text-ink">Class update</h2></div></div><p className="mt-4 whitespace-pre-line text-sm leading-6 text-muted">{dailyInfo.importantNews}</p><p className="mt-3 rounded-xl bg-[#f4f8e7] p-3 text-sm font-semibold leading-6 text-ink">“{dailyInfo.suvichar}”</p><p className="mt-3 text-xs leading-5 text-muted">{dailyInfo.assemblyInfo}</p></section>}
          {dailyInfo?.notes?.length > 0 && <section className="surface-card scroll-mt-24 p-5" id="student-notes"><span className="section-kicker">FROM YOUR TEACHER</span><h2 className="mt-1 text-lg font-black text-ink">Important notes</h2><div className="mt-3 space-y-3">{dailyInfo.notes.map((item) => <article key={item.id} className="rounded-xl bg-[#f7f9f6] p-3"><b className="block text-sm text-ink">{item.title}</b><small className="mt-1 block text-[10px] font-bold uppercase text-forest">{item.category}</small><p className="mt-2 whitespace-pre-line text-xs leading-5 text-muted">{item.content}</p></article>)}</div></section>}
          <section className="tip-card"><div className="flex items-start gap-3"><span className="tip-icon"><Leaf size={19} /></span><div><span className="section-kicker">LIGHTER BAG TIP</span><p className="mt-2 text-sm font-semibold leading-6 text-ink">Only pack the books marked “Take home”. Your back will thank you!</p></div></div></section>
        </aside>
      </div>
    </section>
    <ChatWidget audience="student" />
  </DashboardShell>;
}

function ChecklistRow({ item, busy, selectedAction, onSelect }) {
  return <div className="flex items-center gap-3 py-4">
    <span className={`item-icon ${item.requiredToday ? "item-icon-take" : ""}`}><BookOpen size={18} /></span>
    <div className="min-w-0 flex-1"><p className="truncate text-sm font-bold text-ink">{item.name}</p><p className="mt-0.5 text-xs text-muted">{item.subject} · {item.category}{item.requiredToday ? " · Homework today" : ""}</p></div>
    <div className="flex shrink-0 items-center gap-1">
      <button disabled={busy} onClick={() => onSelect("TAKEN_FROM_LOCKER")} className={`toggle-pill !min-w-0 !px-2.5 ${selectedAction === "TAKEN_FROM_LOCKER" ? "toggle-take" : "toggle-leave"}`} aria-pressed={selectedAction === "TAKEN_FROM_LOCKER"} aria-label={`Mark ${item.name} as taken home`}><span>Take</span>{busy && selectedAction !== "TAKEN_FROM_LOCKER" ? <span className="spinner" /> : <ArrowUpRight size={14} />}</button>
      <button disabled={busy} onClick={() => onSelect("KEPT_IN_LOCKER")} className={`toggle-pill !min-w-0 !px-2.5 ${selectedAction === "KEPT_IN_LOCKER" ? "toggle-take" : "toggle-leave"}`} aria-pressed={selectedAction === "KEPT_IN_LOCKER"} aria-label={`Mark ${item.name} as kept in locker`}><span>Leave</span>{busy && selectedAction !== "KEPT_IN_LOCKER" ? <span className="spinner" /> : <ArrowDownLeft size={14} />}</button>
    </div>
  </div>;
}

function TeacherDashboard({ session, onSignOut }) {
  const [students, setStudents] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [credentials, setCredentials] = useState(null);
  const [credentialsCopied, setCredentialsCopied] = useState(false);
  const [studentName, setStudentName] = useState("");
  const [rollNumber, setRollNumber] = useState("");
  const [studentPassword, setStudentPassword] = useState("");
  const [scheduleDate, setScheduleDate] = useState(todayISO());
  const [schedule, setSchedule] = useState({
    isClass7Turn: false, suvichar: "", suvicharMeaning: "", todaysNews: "",
    specialDayInfo: "", assemblyRoleAssignment: "", gkQuestion: ""
  });
  const [timetableDay, setTimetableDay] = useState("MONDAY");
  const [timetableEntries, setTimetableEntries] = useState([]);
  const [weeklyTimetable, setWeeklyTimetable] = useState({});
  const [noteDate, setNoteDate] = useState(todayISO());
  const [note, setNote] = useState({ title: "", content: "", category: "Important information" });
  const [classNotes, setClassNotes] = useState([]);

  async function load() {
    try {
      const [studentsResponse, schedulesResponse] = await Promise.all([teacherApi.students(), teacherApi.schedules()]);
      setStudents(studentsResponse.data);
      setSchedules(schedulesResponse.data);
      const days = Object.keys(dayLabels);
      const [timetableResponses, notesResponse] = await Promise.all([
        Promise.all(days.map((day) => teacherApi.timetable(day))),
        teacherApi.notes()
      ]);
      const week = Object.fromEntries(days.map((day, index) => [
        day, timetableResponses[index].data.entries || []
      ]));
      setWeeklyTimetable(week);
      setTimetableEntries(week[timetableDay] || []);
      setClassNotes(notesResponse.data || []);
      setError("");
    } catch (requestError) {
      setError(messageFromError(requestError));
    }
  }
  useEffect(() => { load(); }, []);

  async function createStudent(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    setCredentials(null);
    setCredentialsCopied(false);
    try {
      const response = await teacherApi.createStudent({
        name: studentName, rollNumber,
        specialPassword: studentPassword.trim() || null
      });
      setCredentials(response.data);
      setStudentName("");
      setRollNumber("");
      setStudentPassword("");
      await load();
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function copyStudentCredentials() {
    const text = `Student login ID: ${credentials.student.rollNumber}\nPassword: ${credentials.specialPassword}`;
    try {
      await navigator.clipboard.writeText(text);
      setCredentialsCopied(true);
    } catch {
      setError("Could not copy the student credentials. Select and copy them manually.");
    }
  }

  async function chooseTimetableDay(day) {
    setTimetableDay(day);
    try {
      const response = await teacherApi.timetable(day);
      const entries = response.data.entries || [];
      setTimetableEntries(entries);
      setWeeklyTimetable((current) => ({ ...current, [day]: entries }));
    } catch (requestError) { setError(messageFromError(requestError)); }
  }

  async function saveTimetable(event) {
    event.preventDefault();
    setBusy(true); setError(""); setNotice("");
    try {
      const response = await teacherApi.saveTimetable({ dayOfWeek: timetableDay, entries: timetableEntries });
      const entries = response.data.entries || timetableEntries;
      setTimetableEntries(entries);
      setWeeklyTimetable((current) => ({ ...current, [timetableDay]: entries }));
      setNotice(`${dayLabels[timetableDay]} timetable saved.`);
    } catch (requestError) { setError(messageFromError(requestError)); }
    finally { setBusy(false); }
  }

  const timetableChartData = Object.keys(dayLabels).map((day) => {
    const entries = day === timetableDay ? timetableEntries : weeklyTimetable[day] || [];
    return {
      day,
      periods: entries.length,
      homework: entries.filter((entry) => entry.homeworkRequired).length
    };
  });
  const maxTimetablePeriods = Math.max(1, ...timetableChartData.map((day) => day.periods));
  const weeklyPeriodCount = timetableChartData.reduce((total, day) => total + day.periods, 0);
  const weeklyHomeworkCount = timetableChartData.reduce((total, day) => total + day.homework, 0);

  async function saveNote(event) {
    event.preventDefault();
    setBusy(true); setError(""); setNotice("");
    try {
      await teacherApi.saveNote({ date: noteDate, ...note });
      setNote({ title: "", content: "", category: "Important information" });
      const response = await teacherApi.notes();
      setClassNotes(response.data || []);
      setNotice("Class note or important information was published.");
    } catch (requestError) { setError(messageFromError(requestError)); }
    finally { setBusy(false); }
  }

  function chooseSchedule(date) {
    const saved = schedules.find((entry) => entry.date === date);
    setScheduleDate(date);
    setSchedule(saved ? {
      isClass7Turn: saved.isParipathDay,
      suvichar: saved.suvichar || "",
      suvicharMeaning: saved.suvicharMeaning || "",
      todaysNews: (saved.todaysNews || []).join("\n"),
      specialDayInfo: saved.specialDayInfo || "",
      assemblyRoleAssignment: saved.assemblyRoleAssignment || "",
      gkQuestion: saved.gkQuestion || ""
    } : {
      isClass7Turn: false, suvichar: "", suvicharMeaning: "", todaysNews: "",
      specialDayInfo: "", assemblyRoleAssignment: "", gkQuestion: ""
    });
    setNotice("");
  }

  async function saveSchedule(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const payload = {
        ...schedule,
        todaysNews: schedule.todaysNews.split("\n").map((headline) => headline.trim()).filter(Boolean)
      };
      await teacherApi.saveSchedule(scheduleDate, payload);
      setNotice("The Class 7 Paripath schedule was saved.");
      await load();
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return <DashboardShell session={session} onSignOut={onSignOut} active="teacher" navigation={[
    { id: "students", label: "Students", icon: <Users size={17} /> },
    { id: "timetable", label: "Timetable", icon: <CalendarDays size={17} /> },
    { id: "class-notes", label: "Class notes", icon: <BookOpen size={17} /> },
    { id: "paripath-schedule", label: "Paripath", icon: <Sparkles size={17} /> }
  ]}>
    <section className="mx-auto max-w-6xl pb-8 pt-8">
      <div className="mb-7"><p className="eyebrow"><span className="eyebrow-dot" /> SCHOOL STAFF PORTAL</p><h1 className="mt-3 text-4xl font-black tracking-tight text-ink">Class 7 admin</h1><p className="mt-2 text-sm text-muted">Create student access and manage date-specific Paripath schedules.</p></div>
      {error && <ErrorBanner message={error} onRetry={load} />}
      {notice && <p className="mt-5 rounded-xl border border-[#d3e6ce] bg-[#f0f8e9] p-3 text-sm font-bold text-forest" role="status">{notice}</p>}
      <div className="mt-7 grid gap-6 lg:grid-cols-2">
        <section className="surface-card p-5 sm:p-7" id="students">
          <div className="flex items-center gap-3"><span className="login-icon"><Users size={20} /></span><div><span className="section-kicker">STUDENT ACCESS</span><h2 className="text-xl font-black text-ink">Register a Class 7 student</h2></div></div>
          <form onSubmit={createStudent} className="mt-5 grid gap-4">
            <Field label="Student's name" name="studentName" value={studentName} onChange={(event) => setStudentName(event.target.value)} maxLength={120} placeholder="Full name" icon={<GraduationCap size={17} />} required />
            <Field label="Student login ID (roll number)" name="rollNumber" value={rollNumber} onChange={(event) => setRollNumber(event.target.value)} maxLength={20} pattern="[A-Za-z0-9][A-Za-z0-9-]{1,19}" placeholder="e.g. C7-103" icon={<BookOpen size={17} />} required />
            <Field label="Student password (optional)" name="specialPassword" value={studentPassword} onChange={(event) => setStudentPassword(event.target.value)} minLength={8} maxLength={72} placeholder="Set one or leave empty to generate" icon={<KeyRound size={17} />} type="password" />
            <p className="text-xs leading-5 text-muted">The password is securely hashed. A generated password is shown only once after registration, so share the credentials with the student before leaving this page.</p>
            <button disabled={busy} className="primary-button">{busy ? "Registering…" : "Register student and create login"} <ArrowRight size={16} /></button>
          </form>
          {credentials && <div className="mt-5 rounded-2xl border border-[#d3e6ce] bg-[#f3f8ed] p-4" role="status">
            <p className="text-sm font-black text-ink">Student registered — give them these login details</p>
            <p className="mt-3 text-sm text-muted">Login ID: <b className="text-ink">{credentials.student.rollNumber}</b></p>
            <p className="mt-1 break-all text-sm text-muted">Password: <b className="font-mono text-ink">{credentials.specialPassword}</b></p>
            <button type="button" className="subtle-button mt-4" onClick={copyStudentCredentials}><Copy size={15} /> {credentialsCopied ? "Credentials copied" : "Copy login details"}</button>
            <p className="mt-2 text-xs font-semibold text-[#596c4a]">Share securely. The password cannot be viewed again after leaving this page.</p>
          </div>}
          <div className="mt-7 border-t border-line pt-5"><h3 className="text-sm font-black text-ink">Class 7 students ({students.length})</h3>{!students.length ? <p className="mt-3 text-sm text-muted">No student accounts found.</p> : <ul className="mt-3 divide-y divide-line">{students.map((student) => <li key={student.studentId} className="flex items-center justify-between gap-3 py-3"><span className="text-sm font-bold text-ink">{student.name}</span><span className="rounded-full bg-[#f2f5ef] px-3 py-1 text-xs font-bold text-muted">{student.rollNumber}</span></li>)}</ul>}</div>
        </section>
        <section className="surface-card p-5 sm:p-7" id="timetable">
          <div className="flex items-center gap-3"><span className="login-icon"><CalendarDays size={20} /></span><div><span className="section-kicker">CLASS 7 TIMETABLE</span><h2 className="text-xl font-black text-ink">Manage weekly periods</h2></div></div>
          <div className="timetable-chart-card mt-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div><span className="section-kicker">WEEK AT A GLANCE</span><h3 className="mt-1 text-base font-black text-ink">Class periods by day</h3></div>
              <div className="flex gap-2">
                <span className="timetable-stat"><b>{weeklyPeriodCount}</b> periods</span>
                <span className="timetable-stat homework"><b>{weeklyHomeworkCount}</b> homework</span>
              </div>
            </div>
            <div className="timetable-chart mt-5" role="img" aria-label={`Weekly timetable bar graph: ${timetableChartData.map((day) => `${dayLabels[day.day]} ${day.periods} periods, ${day.homework} with homework`).join("; ")}`}>
              <div className="timetable-chart-axis"><span>{maxTimetablePeriods}</span><span>{Math.ceil(maxTimetablePeriods / 2)}</span><span>0</span></div>
              {timetableChartData.map((day) => <div className="timetable-chart-day" key={day.day} title={`${dayLabels[day.day]}: ${day.periods} periods, ${day.homework} with homework`}>
                <div className="timetable-chart-track">
                  {day.periods > 0
                    ? <div className="timetable-chart-bar" style={{ height: `${Math.max(12, day.periods / maxTimetablePeriods * 100)}%` }}>
                      <span className="timetable-chart-value">{day.periods}</span>
                      {day.homework > 0 && <span className="timetable-chart-homework" style={{ height: `${day.homework / day.periods * 100}%` }} />}
                    </div>
                    : <span className="timetable-chart-zero">0</span>}
                </div>
                <span className="timetable-chart-label">{dayLabels[day.day].slice(0, 3)}</span>
              </div>)}
            </div>
            <div className="mt-4 flex flex-wrap gap-4 text-[10px] font-semibold text-muted">
              <span className="flex items-center gap-1.5"><i className="timetable-legend-period" /> Periods</span>
              <span className="flex items-center gap-1.5"><i className="timetable-legend-homework" /> Periods with homework</span>
            </div>
          </div>
          <form onSubmit={saveTimetable} className="mt-5 grid gap-4">
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Day</span><select className="mini-input" value={timetableDay} onChange={(event) => chooseTimetableDay(event.target.value)}>{Object.keys(dayLabels).map((day) => <option key={day} value={day}>{dayLabels[day]}</option>)}</select></label>
            {timetableEntries.map((entry, index) => <div key={entry.id || index} className="grid gap-2 rounded-xl border border-line p-3 sm:grid-cols-[1fr_1fr_auto]"><Field label="Period" name={`period-${index}`} value={entry.period || ""} onChange={(event) => setTimetableEntries((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, period: event.target.value } : item))} required /><Field label="Subject" name={`subject-${index}`} value={entry.subject || ""} onChange={(event) => setTimetableEntries((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, subject: event.target.value } : item))} required /><label className="flex items-center gap-2 pb-1 text-xs font-bold text-ink"><input type="checkbox" checked={Boolean(entry.homeworkRequired)} onChange={(event) => setTimetableEntries((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, homeworkRequired: event.target.checked } : item))} className="accent-forest" /> Homework</label></div>)}
            <button type="button" className="secondary-button" onClick={() => setTimetableEntries((current) => [...current, { period: `Period ${current.length + 1}`, subject: "", homeworkRequired: false }])}><Plus size={16} /> Add period</button>
            <button disabled={busy} className="primary-button">{busy ? "Saving…" : "Save timetable"} <ArrowRight size={16} /></button>
          </form>
        </section>
        <section className="surface-card p-5 sm:p-7" id="class-notes">
          <div className="flex items-center gap-3"><span className="login-icon"><BookOpen size={20} /></span><div><span className="section-kicker">CLASS INFORMATION</span><h2 className="text-xl font-black text-ink">Add important news and notes</h2></div></div>
          <form onSubmit={saveNote} className="mt-5 grid gap-4">
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Display date</span><input className="mini-input" type="date" value={noteDate} onChange={(event) => setNoteDate(event.target.value)} required /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Category</span><select className="mini-input" value={note.category} onChange={(event) => setNote((current) => ({ ...current, category: event.target.value }))}><option>Important information</option><option>Today in the city</option><option>Exam note</option><option>Homework note</option><option>Suvichar</option><option>GK</option></select></label>
            <Field label="Note title" name="noteTitle" value={note.title} onChange={(event) => setNote((current) => ({ ...current, title: event.target.value }))} maxLength={120} required />
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Information or note</span><textarea className="mini-input min-h-28 py-2" maxLength={4000} value={note.content} onChange={(event) => setNote((current) => ({ ...current, content: event.target.value }))} required /></label>
            <button disabled={busy} className="primary-button">{busy ? "Publishing…" : "Publish for Class 7"} <ArrowRight size={16} /></button>
          </form>
          {classNotes.length > 0 && <div className="mt-6 border-t border-line pt-5"><h3 className="text-sm font-black text-ink">Published notes</h3><ul className="mt-3 space-y-2">{classNotes.slice(0, 5).map((item) => <li key={item.id} className="rounded-xl bg-[#f7f9f6] p-3"><b className="block text-sm text-ink">{item.title}</b><small className="text-[11px] text-muted">{item.category} · {item.date}</small></li>)}</ul></div>}
        </section>
        <section className="surface-card p-5 sm:p-7" id="paripath-schedule">
          <div className="flex items-center gap-3"><span className="login-icon"><CalendarDays size={20} /></span><div><span className="section-kicker">ASSEMBLY CONTENT</span><h2 className="text-xl font-black text-ink">Schedule Paripath</h2></div></div>
          <form onSubmit={saveSchedule} className="mt-5 grid gap-4">
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Schedule date</span><input className="mini-input" type="date" value={scheduleDate} onChange={(event) => chooseSchedule(event.target.value)} required /></label>
            <label className="flex items-center gap-2 text-sm font-bold text-ink"><input type="checkbox" checked={schedule.isClass7Turn} onChange={(event) => setSchedule((current) => ({ ...current, isClass7Turn: event.target.checked }))} className="accent-forest" /> Class 7 has the Paripath turn on this date</label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Aaj ka Suvichar</span><textarea className="mini-input min-h-20 py-2" maxLength={1000} value={schedule.suvichar} onChange={(event) => setSchedule((current) => ({ ...current, suvichar: event.target.value }))} /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Simple meaning</span><textarea className="mini-input min-h-16 py-2" maxLength={1000} value={schedule.suvicharMeaning} onChange={(event) => setSchedule((current) => ({ ...current, suvicharMeaning: event.target.value }))} /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Day's headlines (one per line, up to 4)</span><textarea className="mini-input min-h-24 py-2" maxLength={1200} value={schedule.todaysNews} onChange={(event) => setSchedule((current) => ({ ...current, todaysNews: event.target.value }))} /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Din Vishesh</span><textarea className="mini-input min-h-20 py-2" maxLength={2000} value={schedule.specialDayInfo} onChange={(event) => setSchedule((current) => ({ ...current, specialDayInfo: event.target.value }))} /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">GK question</span><textarea className="mini-input min-h-16 py-2" maxLength={2000} value={schedule.gkQuestion} onChange={(event) => setSchedule((current) => ({ ...current, gkQuestion: event.target.value }))} placeholder="Question and answer for students" /></label>
            <label className="block"><span className="mb-1.5 block text-xs font-bold text-ink">Student assembly roles</span><textarea className="mini-input min-h-20 py-2" maxLength={4000} placeholder="Welcome: …&#10;Suvichar: …&#10;News: …" value={schedule.assemblyRoleAssignment} onChange={(event) => setSchedule((current) => ({ ...current, assemblyRoleAssignment: event.target.value }))} /></label>
            <p className="rounded-xl bg-[#f7f9f6] p-3 text-xs leading-5 text-muted">If any required content is missing on a Paripath day, Gemini fills only the missing sections. AI headline suggestions are not verified live news and should be checked by a teacher.</p>
            <button disabled={busy} className="primary-button">{busy ? "Saving…" : "Save Paripath schedule"} <ArrowRight size={16} /></button>
          </form>
          {schedules.length > 0 && <div className="mt-6 border-t border-line pt-5"><h3 className="text-sm font-black text-ink">Upcoming schedules</h3><div className="mt-3 flex flex-wrap gap-2">{schedules.slice(0, 8).map((entry) => <button key={entry.date} className="date-pill" onClick={() => chooseSchedule(entry.date)}>{entry.date}{entry.isParipathDay ? " · Class 7" : ""}</button>)}</div></div>}
        </section>
      </div>
    </section>
  </DashboardShell>;
}

function ParentDashboard({ session, onSignOut }) {
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");
  const [date, setDate] = useState(todayISO());
  const [loading, setLoading] = useState(true);

  async function loadReport(selectedDate = date) {
    try { setReport((await parentApi.monitor(selectedDate)).data); setError(""); }
    catch (requestError) { setError(messageFromError(requestError)); }
    finally { setLoading(false); }
  }
  useEffect(() => {
    loadReport(date);
    const timer = window.setInterval(() => loadReport(date), 20000);
    return () => window.clearInterval(timer);
  }, [date]);

  const homeworkComplete = report?.homeworkChecklistComplete;
  return <DashboardShell session={session} onSignOut={onSignOut} active="parent">
    <section className="mx-auto max-w-6xl pb-8 pt-8">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end"><div><p className="eyebrow"><span className="eyebrow-dot" /> YOUR FAMILY DASHBOARD</p><h1 className="mt-3 text-4xl font-black tracking-tight text-ink">Good day, {session.name?.split(" ")[0] || "parent"} <span className="wave">✦</span></h1><p className="mt-2 text-sm text-muted">A reassuring little window into your child's school day.</p></div><div className="flex flex-wrap items-center gap-2"><span className="live-badge"><span className="live-dot" /> Refreshes every 20 sec</span><label className="date-pill"><Clock3 size={15} /><input type="date" max={todayISO()} value={date} onChange={(event) => setDate(event.target.value)} aria-label="Select activity date" /></label></div></div>
      <ParipathCard />
      {error && <ErrorBanner message={error} onRetry={() => loadReport(date)} />}
      <div className="mt-8 grid gap-6 lg:grid-cols-[.8fr_1.2fr]">
        <section className="surface-card p-5 sm:p-7">
          <span className="section-kicker">YOUR CLASS 7 STUDENT</span><div className="mt-4 flex items-center gap-4"><span className="student-avatar">{session.name?.charAt(0) || "S"}</span><div><h2 className="text-xl font-black text-ink">{report?.studentName || session.name}</h2><p className="mt-1 text-sm text-muted">Roll number {report?.rollNumber || "Class 7"}</p></div></div>
          <div className={`alert-card mt-6 ${homeworkComplete === false ? "alert-warn" : homeworkComplete ? "alert-good" : ""}`}><span className="alert-icon">{homeworkComplete === false ? <HelpCircle size={19} /> : homeworkComplete ? <CheckCircle2 size={19} /> : <Clock3 size={19} />}</span><div><p className="text-sm font-extrabold text-ink">{homeworkComplete === false ? "A quick packing reminder" : homeworkComplete ? "Homework books are packed" : "Checking today's checklist…"}</p><p className="mt-1 text-xs leading-5 text-muted">{homeworkComplete === false ? `Still to take: ${report?.missedRequiredItems?.join(", ") || "check the activity feed"}.` : homeworkComplete ? "The latest locker activity shows all required homework items were taken home." : "The locker hasn't reported an activity yet."}</p></div></div>
          <div className="mt-5 rounded-2xl bg-[#f7f9f6] p-4"><div className="flex items-center gap-2 text-xs font-bold text-muted"><ShieldCheck size={15} className="text-forest" /> A gentle note</div><p className="mt-2 text-xs leading-5 text-muted">Locker updates tell you which items were moved. They don't measure study time or prove whether a child studied.</p></div>
        </section>
        <section className="surface-card p-5 sm:p-7" id="activity">
          <div className="flex items-start justify-between gap-4"><div><span className="section-kicker">LOCKER TIMELINE</span><h2 className="mt-1 text-2xl font-black text-ink">Today's activity</h2><p className="mt-1 text-sm text-muted">{report?.reportDate || date}</p></div><span className="count-pill">{report?.todayLogs?.length || 0} updates</span></div>
          {loading ? <LoadingRows /> : !report?.todayLogs?.length ? <EmptyState title="A quiet locker so far" copy="When your child checks an item in or out, you'll see the update here." /> : <div className="mt-6 space-y-0">{report.todayLogs.map((log) => <div key={log.id} className="timeline-row"><span className={`timeline-icon ${log.action === "TAKEN_FROM_LOCKER" ? "timeline-take" : "timeline-leave"}`}>{log.action === "TAKEN_FROM_LOCKER" ? <ArrowUpRight size={17} /> : <ArrowDownLeft size={17} />}</span><div className="min-w-0 flex-1"><p className="text-sm font-bold text-ink">{log.itemName}</p><p className="mt-0.5 text-xs text-muted">{log.action === "TAKEN_FROM_LOCKER" ? "Taken from locker" : "Kept in locker"}</p></div><time className="text-xs font-semibold text-muted">{new Date(log.timestamp).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</time></div>)}</div>}
        </section>
      </div>
    </section>
    <ChatWidget audience="parent" />
  </DashboardShell>;
}

function ChatWidget({ audience }) {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([{ role: "assistant", text: audience === "parent" ? "Hello! I can help explain today's locker updates or homework packing status." : "Hi! Ask me about today's timetable, books to pack, or a subject you're learning." }]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function send(event) {
    event.preventDefault(); const form = event.currentTarget; const message = new FormData(form).get("message")?.trim(); if (!message || busy) return;
    setMessages((current) => [...current, { role: "user", text: message }]); form.reset(); setBusy(true); setError("");
    try { const response = await aiApi.chat(message); setMessages((current) => [...current, { role: "assistant", text: response.data.reply }]); }
    catch (requestError) { setError(messageFromError(requestError)); }
    finally { setBusy(false); }
  }
  return <>
    {open && <section className="chat-panel" id="assistant"><div className="flex items-center justify-between border-b border-line px-4 py-3"><div className="flex items-center gap-2"><span className="chat-avatar"><Sparkles size={16} /></span><span><b className="block text-sm text-ink">Locker buddy</b><small className="text-[10px] text-muted">Class 7 AI helper</small></span></div><button className="icon-button !h-8 !w-8" onClick={() => setOpen(false)} aria-label="Close chat"><X size={16} /></button></div><div className="chat-messages">{messages.map((entry, index) => <p key={index} className={`chat-bubble ${entry.role === "user" ? "chat-user" : "chat-ai"}`}>{entry.text}</p>)}{busy && <p className="chat-bubble chat-ai"><span className="typing-dots">Thinking…</span></p>}{error && <p className="chat-error">{error}</p>}</div><form onSubmit={send} className="flex gap-2 border-t border-line p-3"><input name="message" maxLength={1000} placeholder="Ask a question…" aria-label="Message to locker buddy" required className="chat-input" /><button className="chat-send" disabled={busy} aria-label="Send message"><Send size={16} /></button></form></section>}
    <button className="chat-fab" onClick={() => setOpen(!open)} aria-label={open ? "Close AI chat" : "Open AI chat"}>{open ? <X size={21} /> : <><Sparkles size={20} /><span className="hidden sm:inline">Ask AI</span></>}</button>
  </>;
}

function QrPage({ onBack }) {
  const [url, setUrl] = useState(() => sessionStorage.getItem("locker-portal-url") || import.meta.env.VITE_PORTAL_URL || `${location.origin}/`);
  const [qr, setQr] = useState("");
  const [qrError, setQrError] = useState("");
  const [addressOptions, setAddressOptions] = useState([]);
  const [addressError, setAddressError] = useState("");
  const [addressLoading, setAddressLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState("");
  const refreshAddresses = useCallback(async () => {
    setAddressLoading(true);
    try {
      const { data } = await classApi.portalAddresses();
      setAddressOptions(data);
      setAddressError("");
    } catch (error) {
      console.error("Could not discover local network addresses for the locker QR.", error);
      setAddressOptions([]);
      setAddressError(messageFromError(error));
    } finally {
      setAddressLoading(false);
    }
  }, []);
  const portalUrl = useMemo(() => {
    try {
      const parsed = new URL(url.trim());
      return ["http:", "https:"].includes(parsed.protocol)
        && !parsed.username && !parsed.password
        ? parsed.href
        : "";
    } catch {
      return "";
    }
  }, [url]);
  const localAddress = useMemo(() => {
    if (!portalUrl) return false;
    const hostname = new URL(portalUrl).hostname.toLowerCase();
    return hostname === "localhost" || hostname.endsWith(".localhost")
      || hostname === "0.0.0.0" || hostname === "::" || hostname === "[::]"
      || hostname === "::1" || hostname === "[::1]" || /^127\./.test(hostname);
  }, [portalUrl]);
  const loopbackAddress = useMemo(() => {
    if (!portalUrl) return false;
    const hostname = new URL(portalUrl).hostname.toLowerCase();
    return hostname === "localhost" || hostname.endsWith(".localhost")
      || hostname === "0.0.0.0" || hostname === "::" || hostname === "[::]"
      || hostname === "::1" || hostname === "[::1]" || /^127\./.test(hostname);
  }, [portalUrl]);
  useEffect(() => {
    if (!loopbackAddress) return undefined;
    void refreshAddresses();
    return undefined;
  }, [loopbackAddress, refreshAddresses]);
  useEffect(() => {
    let active = true;
    setQr("");
    setQrError("");
    if (!portalUrl || localAddress) return () => { active = false; };
    QRCode.toDataURL(portalUrl, { width: 640, margin: 4, color: { dark: "#172321", light: "#ffffff" }, errorCorrectionLevel: "Q" })
      .then((dataUrl) => { if (active) setQr(dataUrl); })
      .catch((error) => {
        console.error("Could not generate the locker QR code.", error);
        if (active) setQrError("Could not generate the QR code for this address.");
      });
    return () => { active = false; };
  }, [portalUrl, localAddress]);
  async function copy() {
    setCopyError("");
    try { await navigator.clipboard.writeText(portalUrl); setCopied(true); window.setTimeout(() => setCopied(false), 1800); }
    catch (error) {
      console.error("Could not copy the portal address.", error);
      setCopied(false);
      setCopyError("Copy is unavailable here. Select the address above and copy it manually.");
    }
  }
  return <main className="min-h-screen bg-paper px-5 pb-12 pt-5 sm:px-8 sm:pt-8">
    <header className="mx-auto flex max-w-6xl items-center justify-between"><Brand /><button className="subtle-button" onClick={onBack}><ArrowLeft size={16} /> Home</button></header>
    <section className="mx-auto grid max-w-5xl items-center gap-10 py-12 md:grid-cols-[1fr_.85fr] md:py-20">
      <div><p className="eyebrow"><span className="eyebrow-dot" /> PORTAL ADDRESS</p><h1 className="mt-5 text-5xl font-black leading-[1.02] tracking-[-.05em] text-ink">Open the<br /><span className="text-forest">Class 7</span> portal.</h1><p className="mt-5 max-w-md leading-7 text-muted">Use this web address on a phone or computer to open the Class 7 Smart Locker.</p>
        <div className="mt-8">
          <label className="mb-2 block text-xs font-bold text-ink" htmlFor="portal-url">Portal address</label>
          <div className="flex gap-2">
            <input id="portal-url" type="url" inputMode="url" maxLength={2048} value={url} onChange={(event) => { setUrl(event.target.value); sessionStorage.setItem("locker-portal-url", event.target.value); setCopied(false); setCopyError(""); }} className="mini-input min-w-0 flex-1" />
            <button className="subtle-button !rounded-xl" onClick={copy} disabled={!portalUrl || localAddress}>{copied ? <Check size={16} /> : <ArrowRight size={16} />}{copied ? "Copied" : "Copy"}</button>
          </div>
          <p className="mt-2 text-xs leading-5 text-muted">For a local network address, connect the phone and this computer to the same Wi-Fi. A deployed HTTPS address works from anywhere.</p>
          {localAddress && <p className="address-notice mt-3" role="alert">This address only opens on this computer. Connect this computer and your phone to the same Wi-Fi, then refresh and select its network address.</p>}
          {loopbackAddress && <div className="mt-3 rounded-xl border border-[#d3e6ce] bg-[#f0f8e9] p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-xs font-bold text-forest">Choose the network address for your phone</p>
              <button type="button" className="subtle-button !min-h-9 !rounded-lg !px-2.5 !text-[10px]" onClick={refreshAddresses} disabled={addressLoading}>{addressLoading ? "Checking…" : "Refresh addresses"}</button>
            </div>
            <p className="mt-1 text-[11px] leading-5 text-muted">Connect this computer and phone to the same Wi-Fi, then refresh. Select that Wi-Fi address below. Do not choose a USB/mobile tether address for a phone connected to Wi-Fi.</p>
            {addressOptions.length > 0 && <div className="mt-2 flex flex-wrap gap-2">{addressOptions.map((option) => <button key={option.url} type="button" className="subtle-button !min-h-9 !rounded-lg !px-2.5 !text-[10px]" onClick={() => { setUrl(option.url); sessionStorage.setItem("locker-portal-url", option.url); setCopied(false); setCopyError(""); }}>{option.network}: {option.url.replace(/^http:\/\//, "")}</button>)}</div>}
          </div>}
          {loopbackAddress && addressError && <p className="address-notice mt-3" role="alert">Could not find a phone-reachable local address automatically. Connect this computer to the same Wi-Fi as the students, or type the school network address here. Details: {addressError}</p>}
          {copyError && <p className="form-error mt-3" role="alert">{copyError}</p>}
        </div>
      </div>
      <div className="surface-card p-6 sm:p-8">
        <div className="flex items-center justify-between gap-4"><Brand /><span className="address-badge">PORTAL QR</span></div>
        <h2 className="mt-8 text-xl font-black text-ink">Scan to open</h2>
        <div className="portal-qr-wrap mt-4">
          {qr
            ? <img className="portal-qr-image" src={qr} alt={`QR code for ${portalUrl}`} />
            : <p className="px-4 text-center text-sm text-muted" role="status">{localAddress ? "Choose a network address to generate its QR code." : qrError || "Generating QR code…"}</p>}
        </div>
        {portalUrl
          ? <a className="mt-4 block break-all rounded-xl border border-line bg-white p-4 text-sm font-bold leading-6 text-forest underline decoration-forest/30 underline-offset-4" href={portalUrl}>{portalUrl}</a>
          : <p className="mt-3 rounded-xl border border-line bg-white p-4 text-sm text-muted" role="status">Enter a valid address beginning with http:// or https://.</p>}
        <p className="mt-4 text-xs leading-5 text-muted">{localAddress ? "This is a computer-only address. Choose a network address above to generate a QR for another device." : "The QR code opens this address. For local access, make sure the phone and computer are on the same Wi-Fi."}</p>
        <div className="mt-5 flex items-center gap-2 border-t border-line pt-4 text-[10px] font-semibold uppercase tracking-[.12em] text-muted"><ShieldCheck size={13} className="text-forest" /> Official Class 7 portal</div>
      </div>
    </section>
  </main>;
}

function ErrorBanner({ message, onRetry }) { return <div className="error-banner" role="alert"><span>{message}</span><button onClick={onRetry} className="font-bold underline">Try again</button></div>; }
function LoadingRows() { return <div className="mt-5 space-y-3"><div className="skeleton h-12" /><div className="skeleton h-12" /><div className="skeleton h-12" /></div>; }
function EmptyState({ title, copy }) { return <div className="empty-state"><span className="empty-icon"><BookOpen size={19} /></span><p className="text-sm font-bold text-ink">{title}</p><p className="mt-1 max-w-xs text-xs leading-5 text-muted">{copy}</p></div>; }

export default App;
