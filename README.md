# Class 7 Smart AI Locker

## Requirements
- Java 17+
- Maven 3.9+
- Node.js 20+

## Frontend development

```powershell
cd frontend
npm install
npm run dev
```

Vite runs at `http://localhost:5173` and proxies `/api` requests to the Spring
Boot server at `http://localhost:8080`. To change that backend URL while
developing, set `VITE_API_PROXY_TARGET`.
The portal address page displays, copies, and generates a QR code for the
selected `http://` or `https://` portal URL. A local `localhost` address only
works on the development computer. In the local profile, the page discovers
private network addresses to choose from. Select the address on the same Wi-Fi
as the student phones; do not select a USB/mobile tether address for phones on
Wi-Fi. For reliable access from other networks, deploy the app with HTTPS.

Build the installable PWA into Spring Boot's static resources:

```powershell
cd frontend
npm run build
cd ..
mvn package
```

The generated frontend is then served from the same origin as the backend.
The QR page uses that origin automatically. API responses are deliberately
not cached by the PWA.

## Deploy on Render with Railway MySQL

The root `Dockerfile` builds the frontend and Spring Boot app together.
`render.yaml` defines a Render web service and prompts for deployment secrets
and database connection values. The application uses Railway MySQL as an
external database.

1. Create a **private** GitHub repository for this project only. Do not select
   the Windows home folder as the repository; the project folder is
   `smart ai locker`. Keep `.env`, credentials, and API keys out of GitHub.
2. Push the project folder to that repository and connect it to Render using
   **New > Blueprint**. Render reads `render.yaml` and builds the app from the
   Dockerfile.
3. In Railway, create or select a MySQL service and enable its **TCP Proxy /
   public networking**. Use the Railway-provided public proxy host and port,
   not an internal `*.railway.internal` address, for Render's `MYSQLHOST` and
   `MYSQLPORT`. Use the Railway database name, user, and password for
   `MYSQLDATABASE`, `MYSQLUSER`, and `MYSQLPASSWORD`. Keep these values in
   Render's environment settings; never commit them.
4. Render generates `JWT_SECRET`. Set `TEACHER_ACCESS_KEY` to a separate,
   strong value in the Render service environment.
5. Wait for the Render deployment to become healthy, then open its HTTPS
   domain. The portal's QR page uses this public origin automatically.

`MYSQL_SSL_MODE=REQUIRED` encrypts the database connection and expects the
Railway MySQL endpoint to support TLS. If Railway's TCP proxy does not support
TLS for the selected database, do not switch to an unencrypted public
connection; use a TLS-capable endpoint or host the app beside the database.
Local H2 profiles are for development only; do not enable the demo profile
for a public deployment. The Render profile disables the optional AI
integration.

Render's free web service may spin down after inactivity. This can delay the
first visit after a quiet period. Upgrade to an always-on instance if the
school needs consistent immediate availability; Railway MySQL may also incur
charges depending on its plan.

## Run locally (no MySQL or Gemini key needed)

PowerShell:

```powershell
$env:JWT_SECRET = "local-development-secret-change-this-before-production-123"
$env:TEACHER_ACCESS_KEY = "local-teacher-access-key-change-this-32chars"
mvn spring-boot:run "-Dspring-boot.run.profiles=local,demo"
```

The local profile uses an in-memory H2 database. The demo profile loads sample
Class 7 data on each start. The database is cleared when the app stops.
AI is disabled in this default local profile. To enable the Gemini chatbot
locally, set `GEMINI_API_KEY` in the current PowerShell session and run with the
additional `ai-local` profile:

```powershell
$env:GEMINI_API_KEY = "your-Gemini-API-key"
mvn spring-boot:run "-Dspring-boot.run.profiles=local,demo,ai-local"
```

Keep the key in an environment variable, not in source files or screenshots.

## Run with MySQL and Gemini

```powershell
# Create the class7_locker database in MySQL, then set the credentials for a
# MySQL user that has permission to create/alter tables in that database.
$env:DB_USERNAME = "your_mysql_user"
$env:DB_PASSWORD = "your_mysql_password"
$env:GEMINI_API_KEY = "your-key"
$env:JWT_SECRET = "use-a-long-random-secret-with-at-least-32-bytes"
$env:TEACHER_ACCESS_KEY = "use-a-separate-random-school-secret-at-least-32-bytes"
mvn spring-boot:run
```

`TEACHER_ACCESS_KEY` is a server-side secret used to sign in to the school staff
portal. Keep it out of source control and share it only with authorized staff.
Teachers can create student accounts and set up dated Paripath schedules from
the portal. If a teacher leaves the student's special password blank, the app
generates a one-time password and returns it only in the creation response.
Students sign in with credentials created and shared by an authorized teacher.

To load sample rows with MySQL, also activate the `demo` profile:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Demo data is for development only. It does not erase existing activity logs.

Run the automated unit and application smoke tests with:

```powershell
mvn test
```

Public daily-info endpoint: `http://localhost:8080/api/class7/daily-info`.

Sample demo student:
- Roll number: `C7-101`
- Special password: `SCIENCE7`

Sample demo parent:
- Email: `parent@example.com`
- Password: `Parent@123`

## API

- `POST /api/auth/student/login`
- `POST /api/auth/parent/register`
- `POST /api/auth/parent/login`
- `POST /api/auth/teacher/login` (access key configured by the school)
- `GET /api/teacher/students` (teacher JWT required)
- `POST /api/teacher/students` (teacher JWT required)
- `GET /api/teacher/paripath` (teacher JWT required)
- `PUT /api/teacher/paripath/{yyyy-MM-dd}` (teacher JWT required)
- `GET /api/locker/today`
- `POST /api/locker/items` (add an item to the authenticated student's locker)
- `PUT /api/locker/items/{itemId}` (update an item owned by the authenticated student)
- `GET /api/locker/activity/today` (the student's own latest activity feed)
- `POST /api/locker/activity`
- `GET /api/parent/monitor`
- `GET /api/parent/monitor?date=2026-11-18` (optional past or current date)
- `GET /api/class7/daily-info`
- `GET /api/class7/paripath/today`
- `POST /api/ai/chat`

Send `Authorization: Bearer <token>` on protected endpoints.

Example student login:

```json
{"rollNumber":"C7-101","specialPassword":"SCIENCE7"}
```

The dated Paripath endpoint returns `isParipathDay: false` when no Class 7
turn is configured for today. Teachers set the date, turn, assembly roles and
any manually prepared content through the teacher portal. On a Class 7 turn,
missing Suvichar, meaning, news suggestions or Din Vishesh are generated with
Gemini. Gemini has no live news feed here, so AI-generated headlines are
learning prompts and are clearly marked for teacher review.

Example locker activity:

```json
{"itemId":1,"action":"TAKEN_FROM_LOCKER"}
```

Example locker item:

```json
{"name":"Science Book","category":"Book","subject":"Science","requiredToday":false}
```
