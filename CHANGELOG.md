# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-10-21

### Added
- **Puter.js Multilingual Whisper Integration**: Added zero-configuration, browser-driven OpenAI Whisper fallback engine capable of offline/in-browser multilingual transcription.
- **ExportService**: Added support for SubRip (`.srt`) subtitle export with dynamic timecodes, WebVTT (`.vtt`), and structured Markdown report downloads.
- **AudioProcessingUtility**: Added PCM 16-bit audio mathematical utilities for RMS volume measurement, silence threshold detection, and standard 44-byte WAV header synthesis.
- **Health Telemetry**: Added `/api/health` actuator-style diagnostic endpoint reporting JVM memory footprint, uptime, thread count, and database connection latency.
- **Automated CI Workflow**: Configured GitHub Actions CI pipeline executing automated builds and JUnit 5 test suites on every pull request and push to `main`.
- **Nginx Gateway**: Added multi-container production Docker Compose stack featuring an Nginx reverse-proxy gateway with bidirectional WebSocket upgrade support.
- **Documentation**: Comprehensive Architecture document (`docs/ARCHITECTURE.md`) and OpenAPI 3.0 specification (`docs/API_DOCUMENTATION.md`).

### Fixed
- **Speech Synthesis Network Loop**: Resolved browser Web Speech API `network` error loop by automatically falling back to language-agnostic speech models.
- **Render H2 Storage Permissions**: Reconfigured database persistence paths and file permissions for containerized cloud environments.
- **Export Endpoint Resolution**: Standardized export controller mapping to support multiple query parameters cleanly.

---

## [1.0.0] - 2026-10-21

### Initial Release
- Spring Boot 3.3.4 microservice architecture with Java 21.
- Real-time WebSocket audio streaming on `/ws/transcribe`.
- Hinglish code-mixing and language detection engine (`LanguageDetectionService`).
- JPA & H2 / PostgreSQL relational storage with complete CRUD endpoints.
- Modern Glassmorphism Studio UI & Analytics Dashboard.
