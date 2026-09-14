# AI Code Review frontend

A React and Vite dashboard for the asynchronous code-review backend. It starts a review, shows elapsed time and live activity over Server-Sent Events, and falls back to polling the activities and result endpoints every five seconds if the stream cannot be maintained.

## Run locally

Start the Spring Boot backend on port `8080`, then run:

```powershell
cd frontend
npm install
npm run dev
```

Vite serves the frontend at `http://localhost:5173` and proxies `/api` requests to `http://localhost:8080`.

## Optional deployment configuration

Copy `.env.example` to `.env` and set `VITE_API_BASE_URL` when the frontend and API are hosted on different origins. Leave it blank for local Vite proxy development.
