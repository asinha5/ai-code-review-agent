# AI Code Review frontend

This React + Vite dashboard starts and follows asynchronous reviews from the Spring Boot backend. REST starts reviews and retrieves stored state; SSE delivers live `review-activity` events; five-second polling is the fallback when the browser cannot maintain its EventSource connection.

## Run locally

Start the backend at `http://localhost:8080`, then run:

```powershell
cd frontend
npm install
npm run dev
```

Vite serves the frontend at `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

Build a production bundle with:

```powershell
npm run build
```

## Optional deployment configuration

Copy `.env.example` to `.env` and set `VITE_API_BASE_URL` when the frontend and API are hosted on different origins. Leave it blank for local Vite proxy development.
