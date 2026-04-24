# KDailyUtil Implementation Plan (Updated 2026-04-24)

## Goal
Build a professional-grade utility app featuring AI news briefing, driver-optimized news shadowing, and a sophisticated audio management system.

## Key Architectures

### 1. Unified Audio Management (Revamped)
- **Side-Tab Sub-menus**: Decoupled UI for Recording, Playing, File Management, and Playlists.
- **Global Player State**: Shared ViewModel across the app for seamless media control.
- **Smart System Integration**: 
    - Auto-pause on Bluetooth/Headphone disconnect.
    - Context-aware floating icons (background only).
- **Modern Storage Strategy**: 
    - Transition to Scoped Storage native paths to eliminate Permission Denied errors.
    - Automatic data migration for legacy files.

### 2. News Shadowing & Learning
- **Educational Content Focus**: Prioritizing high-quality editorials.
- **Adaptive Practice Loop**: Intelligent waiting periods based on sentence length.
- **Clean Extraction**: Advanced noise removal and redirect resolution for complex Korean media sites.

### 3. AI Intelligence
- **Gemini 1.5 Flash Integration**: Real-time summarization of collected news.
- **Custom Briefing**: Voice/STT commanded analysis for specific user requests.

## Technical Milestones
- [x] Scoped Storage & Permission system (Android 10~14+).
- [x] Background-only floating recording control.
- [x] Bluetooth-aware playback service.
- [x] Side-navigation integrated Audio UI.
- [x] Hybrid redirect resolution (Protobuf, Meta-refresh, WebView).

## Future Roadmap
- [ ] **Rich UI Components**: Markdown rendering for AI reports.
- [ ] **Performance Tuning**: Memory optimization for long-running recording sessions.
- [ ] **Visual Data**: Graphs showing daily news consumption and practice statistics.
