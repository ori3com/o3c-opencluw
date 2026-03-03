# Contributing to OpenClaw Assistant

First off, thanks for taking the time to contribute! 🎉

The following is a set of guidelines for contributing to OpenClaw Assistant. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Contributing Code](#contributing-code)
  - [Improving Documentation](#improving-documentation)
  - [Translating](#translating)
- [Development Setup](#development-setup)
  - [Prerequisites](#prerequisites)
  - [Building the Project](#building-the-project)
  - [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [Styleguides](#styleguides)
  - [Git Commit Messages](#git-commit-messages)
  - [Kotlin Style Guide](#kotlin-style-guide)
  - [UI/UX Guidelines](#uiux-guidelines)
- [Community](#community)

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

### New to the project?

- 📖 Read the [README](README.md) to understand what OpenClaw Assistant does
- 🎥 Watch the [demo video](https://x.com/i/status/2017914589938438532)
- 🔧 Try building and running the app locally
- 🐛 Look for [good first issues](../../labels/good%20first%20issue) to get started

### Looking for ideas?

Check out our [roadmap](../../discussions/categories/roadmap) and [feature requests](../../discussions/categories/feature-requests) to see what the community needs.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

**Before submitting:**
- 🔍 Check if the bug has already been reported in [Issues](../../issues)
- 🔄 Try the latest version to see if it's already fixed

**When submitting:**
- **Use a clear and descriptive title** for the issue
- **Describe the exact steps to reproduce** the problem
- **Provide specific examples** to demonstrate the steps
- **Include device information:**
  - Android version
  - Device model
  - App version (found in Settings → About)
- **Include logs** if possible (Settings → Report Issue includes this automatically)

**Template:**
```markdown
**Description:**
Brief description of the bug

**Steps to Reproduce:**
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected Behavior:**
What you expected to happen

**Actual Behavior:**
What actually happened

**Screenshots:**
If applicable, add screenshots

**Device Info:**
- Android Version: [e.g. 14]
- Device: [e.g. Pixel 8]
- App Version: [e.g. 2.0.2]
```

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion.

**Before submitting:**
- 🔍 Check if the enhancement has already been suggested
- 💡 Consider if it aligns with the project's goals

**When submitting:**
- **Use a clear and descriptive title**
- **Provide a step-by-step description** of the suggested enhancement
- **Explain why this enhancement would be useful**
- **List alternatives you've considered**

### Contributing Code

We welcome code contributions! Here's how to get started:

#### Quick Start for Experienced Contributors

```bash
# 1. Fork and clone
git clone https://github.com/your-username/openclaw-assistant.git
cd openclaw-assistant

# 2. Create a branch
git checkout -b feature/your-feature-name

# 3. Make changes and commit
git commit -m "feat: add amazing feature"

# 4. Push and create PR
git push origin feature/your-feature-name
```

#### First-Time Contributors

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/openclaw-assistant.git
   cd openclaw-assistant
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/yuga-hashimoto/openclaw-assistant.git
   ```
4. **Create a branch**:
   ```bash
   git checkout -b feature/amazing-feature
   # or
   git checkout -b fix/annoying-bug
   ```
5. **Make your changes**
6. **Test your changes** (see [Running Tests](#running-tests))
7. **Commit with a clear message** (see [Git Commit Messages](#git-commit-messages))
8. **Push to your fork**:
   ```bash
   git push origin feature/amazing-feature
   ```
9. **Create a Pull Request** on GitHub

#### Pull Request Guidelines

- **Keep changes focused** - One feature/fix per PR
- **Update documentation** if needed
- **Add tests** for new functionality
- **Ensure CI passes** before requesting review
- **Link related issues** using `Fixes #123` or `Relates to #123`

### Improving Documentation

Documentation improvements are always welcome!

- Fix typos or unclear explanations
- Add examples to the README
- Improve code comments
- Translate documentation

### Translating

Help make OpenClaw Assistant accessible to more people:

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-XX/strings.xml` (replace XX with language code)
2. Translate the strings
3. Submit a PR

Current translations:
- 🇺🇸 English (default)
- 🇯🇵 Japanese

Want to add a new language? Open an issue first to coordinate.

## Development Setup

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 or newer
- **Android SDK** with API 34
- **Git**

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yuga-hashimoto/openclaw-assistant.git
   cd openclaw-assistant
   ```

2. **Open in Android Studio**:
   - File → Open → Select the project folder
   - Wait for Gradle sync to complete

3. **Configure local.properties** (optional):
   ```properties
   # For release builds
   storeFile=path/to/keystore.jks
   storePassword=yourpassword
   keyAlias=youralias
   keyPassword=yourpassword
   ```

4. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```

5. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Run all checks (CI does this)
./gradlew check
```

## Project Structure

```
openclaw-assistant/
├── app/src/main/java/com/openclaw/assistant/
│   ├── api/              # API clients (HTTP, WebSocket)
│   ├── camera/           # Camera capture functionality
│   ├── data/             # Data layer (Repository, DAO)
│   ├── gateway/          # Gateway connection, device identity
│   ├── node/             # Node capabilities (camera, location, SMS, screen)
│   ├── protocol/         # Protocol definitions
│   ├── service/          # Background services
│   ├── speech/           # Speech recognition and TTS
│   ├── ui/               # UI components (Compose)
│   └── utils/            # Utility classes
├── app/src/main/res/     # Resources (layouts, strings, etc.)
└── docs/                 # Additional documentation
```

## Styleguides

### Git Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

**Examples:**
```
feat(voice): add custom wake word support

fix(gateway): resolve reconnection issue after network change

docs(readme): update setup instructions for macOS
```

**Rules:**
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues after the first line: `Fixes #123`

### Kotlin Style Guide

We follow the [official Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- **Indentation**: 4 spaces
- **Line length**: 120 characters max
- **Naming**: CamelCase for classes, camelCase for functions/variables, UPPER_SNAKE_CASE for constants
- **Functions**: Prefer expression bodies for single-expression functions

**Example:**
```kotlin
// Good
fun calculateTotal(items: List<Item>): Double = 
    items.sumOf { it.price * it.quantity }

// Avoid
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price * it.quantity }
}
```

### UI/UX Guidelines

- Follow [Material 3](https://m3.material.io/) design principles
- Support both light and dark themes
- Ensure accessibility (content descriptions, proper contrast)
- Test on different screen sizes
- Keep text translatable (use `stringResource()`)

## Community

- 💬 [Discussions](../../discussions) - Ask questions, share ideas
- 🐛 [Issues](../../issues) - Report bugs, request features
- 🐦 [Twitter/X](https://x.com/yugahashimoto) - Follow for updates

## Recognition

Contributors will be:
- 🏆 Listed in the README (with permission)
- 🎉 Mentioned in release notes
- 🌟 Added to the "Contributors" section in the app (coming soon)

---

Thank you for contributing to OpenClaw Assistant! 🦞

Questions? Open a [Discussion](../../discussions) or reach out on [Twitter](https://x.com/yugahashimoto).
