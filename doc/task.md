# KDailyUtil Project Tracker

## 🏁 Milestone 1: Core Foundation (Completed)
- [x] Project Initialization & Bottom Navigation.
- [x] RSS Scraping with Jsoup.
- [x] Gemini API Integration for Briefing.

## 🏁 Milestone 2: Audio Capture & Playlist (Completed)
- [x] Foreground Service for Audio Capture.
- [x] Scoped Storage optimization (`getExternalFilesDir`).
- [x] Playlist/Folder management & File movement.
- [x] Background playback with MediaSession.

## 🏁 Milestone 3: News Shadowing (Completed)
- [x] News body extraction & cleanup.
- [x] Shadowing UI with driving mode theme.
- [x] Automatic recording and instant monitoring.
- [x] Dedicated recording subfolder logic.

## 🏁 Milestone 4: Stability & Polishing (Completed)
- [x] Fix `FileNotFoundException` in recording on Android 10+.
- [x] Fix `IndexOutOfBoundsException` in shadowing UI.
- [x] Implement runtime permission requests (Audio, Storage).
- [x] Add path fallback for robust directory creation.

## 🏁 Milestone 5: Audio System Revamp (Completed 2026-04-24)
- [x] Integrated Side-Tab architecture for Audio sub-menus.
- [x] Bluetooth disconnection auto-pause (`NoisyAudioReceiver`).
- [x] Background-only floating icon lifecycle management.
- [x] Global ViewModel for cross-screen player synchronization.
- [x] Storage migration from Public Download to App-specific folders.
- [x] Toast feedback for playlist operations.

## 🏁 Milestone 6: Smart AI Briefing & Stock Sub-tabs (Completed 2026-05-18)
- [x] Multi-command custom AI briefing settings UI (InputChips).
- [x] AI horizontal scrollable sub-tabs.
- [x] In-memory smart caching for AI results with generation timestamp.
- [x] Datastore validation state persistence.
- [x] Auto-scroll reset to top on category tab change.
- [x] Locked & Accent-styled [전체, 증시, AI] fixed side tabs.
- [x] Custom Stock keyword management and sub-tabs (Phase 1).

## 🔜 Milestone 7: Next Steps (Phase 2)
- [ ] **Decoupled Stock Dashboard**: Integration of Yahoo Finance API for global prices.
- [ ] **Canvas-drawn Sparkline**: Ascent/Descent neon sparklines inside stock cards.
- [ ] **AI Markdown Rendering**: Native markdown formatting support for AI results.
