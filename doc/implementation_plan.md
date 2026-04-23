# KDailyUtil Implementation Plan (Updated)

## Goal
Build a robust news collection and shadowing system with integrated audio management.

## Key Components

### 1. News Shadowing (practice)
- Prioritize high-quality editorials/columns.
- Adaptive shadowing loop: TTS -> Wait (Adaptive) -> Record -> Monitor.
- Driver-safe UI.
- Organized storage in `Download/KDailyUtil/뉴스_쉐도잉`.

### 2. Audio Capture & Playlist Management
- Multi-source recording (Internal/MIC).
- Folder-based playlist organization.
- Background playback stability (MediaSession, WakeLock).
- Recycle Bin (Trash) and Hidden files support.

### 3. News Extraction Engine
- Hybrid redirect resolution (Protobuf, Meta-refresh, WebView).
- Clean text extraction with noise removal.

## Technical Progress
- [x] Base navigation and project structure.
- [x] Gemini-powered news summarization.
- [x] Audio Capture Service with floating controls.
- [x] Reliable news body extraction for major Korean media.
- [x] News Shadowing feature with recording subfolder.
- [x] Audio playlist/folder system.
- [x] Runtime permission management (API 10~13+).
- [x] Crash resilience for recording and UI.

## Future Plans
- [ ] Statistical visualization for shadowing practice.
- [ ] Improved playlist name editing UI.
- [ ] Advanced anti-crawling handling for specific media.
