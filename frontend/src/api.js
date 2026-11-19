import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 12000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("locker-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error?.config?.url || "";
    if (error?.response?.status === 401 && !requestUrl.startsWith("/auth/")) {
      window.dispatchEvent(new Event("locker:unauthorized"));
    }
    return Promise.reject(error);
  }
);

export function messageFromError(error) {
  const details = error?.response?.data?.details;
  if (Array.isArray(details) && details.length) return details.join(". ");
  const serverMessage = error?.response?.data?.error;
  if (error?.response?.status === 409 && /already registered/i.test(serverMessage || "")) {
    return "This email or mobile already has an account. Choose “Already registered? Sign in” below and use your existing password.";
  }
  if (serverMessage) return serverMessage;
  if (error?.code === "ECONNABORTED") return "The server took too long to respond. Please try again.";
  if (error?.message === "Network Error") return "Can't reach the Class 7 server. Check that the backend is running.";
  return "Something went wrong. Please try again.";
}

export const authApi = {
  studentLogin: (data) => api.post("/auth/student/login", data),
  parentLogin: (data) => api.post("/auth/parent/login", data),
  parentRegister: (data) => api.post("/auth/parent/register", data),
  teacherLogin: (data) => api.post("/auth/teacher/login", data)
};

export const lockerApi = {
  today: () => api.get("/locker/today"),
  activityToday: () => api.get("/locker/activity/today"),
  addItem: (data) => api.post("/locker/items", data),
  updateItem: (id, data) => api.put(`/locker/items/${id}`, data),
  activity: (data) => api.post("/locker/activity", data)
};

export const parentApi = {
  monitor: (date) => api.get("/parent/monitor", { params: date ? { date } : {} })
};

export const classApi = {
  dailyInfo: () => api.get("/class7/daily-info"),
  portalAddresses: () => api.get("/class7/portal-addresses"),
  paripathToday: () => api.get("/class7/paripath/today")
};

export const teacherApi = {
  students: () => api.get("/teacher/students"),
  createStudent: (data) => api.post("/teacher/students", data),
  schedules: () => api.get("/teacher/paripath"),
  saveSchedule: (date, data) => api.put(`/teacher/paripath/${date}`, data),
  timetable: (day) => api.get(`/teacher/timetable/${day}`),
  saveTimetable: (data) => api.put("/teacher/timetable", data),
  notes: () => api.get("/teacher/notes"),
  saveNote: (data) => api.post("/teacher/notes", data)
};

export const aiApi = {
  chat: (message) => api.post("/ai/chat", { message })
};
