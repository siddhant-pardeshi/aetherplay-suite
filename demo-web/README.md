# AetherPlay Suite — demo-web

Hosted interactive product tour for the AetherPlay Android suite (AetherPlay Lab + AetherPlay Space).
Everything lives in a single self-contained `index.html` — no external CDNs, fonts, or images; it works fully offline.
Serve locally: `python -m http.server 8000` from this folder, then open http://localhost:8000
Deployment: GitHub Pages serves this `demo-web/` directory straight from the repository.
All simulations are clearly labeled — the tour carries an "Interactive product tour — simulated preview" badge and every panel is browser-only state.
The search tour, Crystal Sprint mini-game, and live memory readout are educational simulations, not the real apps.
APK downloads and footer docs link to the GitHub repository's Releases and `docs/` paths.
Design system: "Aurora Precision" (dark, mint/violet accents, 8px spacing grid, reduced-motion aware).
