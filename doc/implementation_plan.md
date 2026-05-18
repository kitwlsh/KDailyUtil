# KDailyUtil Implementation Plan (Updated 2026-05-18)

## Goal
Build a professional-grade utility app featuring AI news briefing, driver-optimized news shadowing, a sophisticated audio management system, and a premium stock/finance dashboard.

## Key Architectures

### 1. Unified Audio Management
- **Side-Tab Sub-menus**: Decoupled UI for Recording, Playing, File Management, and Playlists.
- **Global Player State**: Shared ViewModel across the app for seamless media control.
- **Smart System Integration**: 
    - Auto-pause on Bluetooth/Headphone disconnect.
    - Context-aware floating icons (background only).
- **Scoped Storage**: Native storage path migration resolving Android 11+ permission errors.

### 2. News Shadowing & Learning
- **Educational Content Focus**: Prioritizing high-quality editorials.
- **Adaptive Practice Loop**: Intelligent waiting periods based on sentence length.
- **Clean Extraction**: Advanced noise removal and redirect resolution for complex Korean media sites.

### 3. AI Intelligence & Smart News Briefing (New)
- **Multi-Command AI Analysis**: Dynamic input chip UI allowing registration of multiple AI instructions.
- **Sub-Tab Navigation**: In-category horizontal scrolling tabs for swapping commands.
- **Smart Caching**: In-memory caching for generated briefings with detailed generation timestamps.
- **Persistent Key Validation**: Encrypted/saved API validation state in Preferences DataStore.

### 4. Financial Dashboard Integration (Phased)
- **Phase 1 (Completed)**: Custom stock/finance keyword sub-tabs added inside the [증시] category.
- **Phase 2 (Upcoming)**: Fully decoupled Stock Menu integrating Yahoo Finance API for real-time tickers and custom Canvas-drawn sparkline charts.

## Technical Milestones
- [x] Scoped Storage & Permission system.
- [x] Background-only floating recording control.
- [x] Bluetooth-aware playback service.
- [x] Side-navigation integrated Audio UI.
- [x] Multi-Command AI Briefing & Caching system.
- [x] Premium [전체, 증시, AI] fixed category accent styling.
- [x] Phase 1 Stock Keywords setup and sub-tabs.

## Future Roadmap (Phase 2 & beyond)
- [ ] **Stock Dashboard**: Fetching real-time global stock/indices/forex from Yahoo Finance.
- [ ] **Visual Charts**: Custom Canvas-drawn sparkline curves reflecting stock indices.
- [ ] **Rich Markdown**: In-app markdown viewer for beautifully structured AI news analysis.
