// Static hosting and npm run dev keep the standalone demo.
// server.mjs supplies this module dynamically when BACKEND_URL is configured.
export const config = Object.freeze({mode:'demo',apiBase:'/api/v1'});
