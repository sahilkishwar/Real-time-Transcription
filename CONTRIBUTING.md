# Contributing to Procucev AI Real-time Transcription Service

Thank you for your interest in contributing! This document details guidelines and instructions for submitting contributions to this codebase.

---

## 🧭 Code of Conduct

We are committed to providing a friendly, safe, and welcoming environment for all contributors. Please treat fellow maintainers with mutual respect and professionalism.

---

## 🛠️ Development Setup

### Prerequisites
- **Java SE Development Kit (JDK) 21** or higher
- **Git 2.40+**
- Modern Web Browser (Chrome / Edge / Firefox) with WebRTC audio capture enabled

### Getting Started
```bash
# 1. Clone the repository
git clone https://github.com/sahilkishwar/Real-time-Transcription.git
cd Real-time-Transcription

# 2. Compile and run test suite
./mvnw clean test

# 3. Launch local development server
./mvnw spring-boot:run
```
Visit `http://localhost:8080` to view the Studio and Dashboard.

---

## 🌿 Branching Strategy

- **`main`**: Production-ready code, always kept green and deployable.
- **`feature/<name>`**: New capabilities and enhancements.
- **`fix/<bug-description>`**: Bug fixes and patches.
- **`docs/<topic>`**: Documentation updates.

---

## 💬 Commit Convention

We enforce the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` A new feature or capability
- `fix:` A bug fix
- `docs:` Documentation-only changes
- `test:` Adding or updating automated tests
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `ci:` Changes to CI/CD workflows and configuration
- `devops:` Docker, orchestration, or proxy updates
- `chore:` Maintenance tasks, dependency updates

---

## 🧪 Testing Guidelines

Before creating a Pull Request, verify that all unit and integration tests execute successfully:

```bash
# Execute entire test suite
./mvnw test

# Run a specific test class
./mvnw test -Dtest=LanguageDetectionServiceTest
```

---

## 🚀 Submitting a Pull Request (PR)

1. Fork the repository and create your feature branch from `main`.
2. Ensure all changes include accompanying tests where applicable.
3. Verify formatting, clean imports, and zero compiler warnings.
4. Open a PR with a clear description of the problem solved and test coverage added.
