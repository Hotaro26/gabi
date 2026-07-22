# Contributing to Gabi

First off, thank you for considering contributing to Gabi. It is people like you that make Gabi such a great tool. Gabi is an open-source project built with love by a teenage developer, and we welcome all kinds of contributions, from bug reports to new features.

This document outlines our guidelines for contributing. Following them helps us work together effectively and keeps the project healthy.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [What We're Looking For](#what-were-looking-for)
3. [Ways to Contribute](#ways-to-contribute)
   - [Reporting Bugs and Requesting Features](#reporting-bugs-and-requesting-features)
   - [Improving Documentation](#improving-documentation)
   - [Code Contributions](#code-contributions)
4. [Getting Started with the Codebase](#getting-started-with-the-codebase)
   - [Project Tech Stack](#project-tech-stack)
   - [Setting Up Your Development Environment](#setting-up-your-development-environment)
   - [Building the Project](#building-the-project)
5. [Pull Request Process](#pull-request-process)
6. [Style Guides](#style-guides)
   - [Git Commit Messages](#git-commit-messages)
   - [Kotlin and Compose Code Style](#kotlin-and-compose-code-style)
   - [Python Code Style](#python-code-style)
7. [Recognition](#recognition)

## Code of Conduct

This project and everyone participating in it is governed by the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainer.

## What We're Looking For

We welcome contributions that improve Gabi in any way. Here are some areas we are particularly focused on:

- **Bug Fixes:** Squashing bugs and improving stability.
- **Feature Enhancements:** Adding support for new sites, download engines, or improving existing features.
- **UI/UX Improvements:** Making the Material 3 interface even more fluid and intuitive.
- **Performance Optimizations:** Speeding up downloads, reducing app size, or improving battery usage.
- **Documentation:** Improving this guide, the README, or adding inline code comments.

## Ways to Contribute

### Reporting Bugs and Requesting Features

The most immediate way to help is by providing feedback.

1. **Check Existing Issues:** Before creating a new issue, please search the [Issues tab](https://github.com/Hotaro26/gabi/issues) to see if it has already been reported or is being worked on.
2. **Create a New Issue:** If your issue is new, please open a new issue. Use the provided templates where possible.
   - **For Bugs:** Provide a clear title, steps to reproduce the bug, what you expected to happen, and what actually happened. Include your Android version and the app version.
   - **For Feature Requests:** Explain the feature and the problem it solves. Describe how you envision it working in the app.

### Improving Documentation

Good documentation is crucial. You can contribute by:

- Fixing typos or clarifying confusing sections in the README.md.
- Adding more detailed instructions for building from source.
- Creating or improving wiki pages.

You can propose documentation changes directly via a Pull Request.

### Code Contributions

1. **Find an Issue:** Look for issues labeled `good-first-issue` or `help-wanted`. These are good starting points.
2. **Claim the Issue:** Comment on the issue to let others know you are working on it.
3. **Discuss Major Changes:** For significant new features or architectural changes, please open an issue first to discuss your idea with the maintainer. This ensures your work aligns with the project's direction.

## Getting Started with the Codebase

Here is how to set up the Gabi development environment.

### Project Tech Stack

- **Language:** Kotlin (UI/Android), Python (Backend Download Logic)
- **UI Framework:** Jetpack Compose (Material 3)
- **Android/Python Integration:** [Chaquopy](https://chaquo.com/chaquopy/)
- **Download Engines:** yt-dlp, gallery-dl, NewPipe Extractor, Cobalt API
- **Networking:** Ktor Client
- **Database:** Room (for download history)
- **Image Loading:** Coil

### Setting Up Your Development Environment

1. **Prerequisites:**
   - Android Studio Jellyfish (or newer) is recommended.
   - Android SDK 34 installed.
   - Python 3.9+ (Chaquopy manages this, but it helps to have it installed).

2. **Clone the Repository:**
   ```bash
   git clone https://github.com/Hotaro26/gabi.git
   cd gabi
   ```

3. **Open the Project:**
   - Open Android Studio and select "Open an Existing Project". Navigate to the `gabi` directory and open it.
   - Android Studio will sync the project. This may take a few minutes.

### Building the Project

- The project uses Gradle for building.
- Chaquopy will automatically download and set up the necessary Python dependencies (yt-dlp, gallery-dl) during the first build.
- To build the APK, you can run the `assembleDebug` or `assembleRelease` Gradle task from Android Studio or the command line:
   ```bash
   ./gradlew assembleDebug
   ```

## Pull Request Process

We use the standard GitHub Pull Request (PR) workflow.

1. **Create a Branch:** Create a new branch for your work.
   ```bash
   git checkout -b feature/your-feature-name
   ```
   or
   ```bash
   git checkout -b fix/the-bug-you-fix
   ```

2. **Write Your Code:** Make your changes, ensuring you follow the style guides below.

3. **Test Thoroughly:**
   - Test your changes on an Android emulator and, if possible, a physical device.
   - Verify that your changes do not break existing functionality.
   - Ensure the download engines (yt-dlp, gallery-dl) still work for core use cases.

4. **Commit Your Changes:** Write clear, descriptive commit messages (see [style guide](#git-commit-messages)).

5. **Push and Open a PR:**
   - Push your branch to your fork and open a Pull Request against the `main` branch of the `Hotaro26/gabi` repository.
   - Provide a clear title and description of your changes in the PR, referencing the issue it addresses.

6. **PR Review:** Your PR will be reviewed by the maintainer. Be open to feedback and make necessary changes.

## Style Guides

### Git Commit Messages

- Use the present tense ("Add feature", not "Added feature").
- Use the imperative mood ("Move cursor to...", not "Moves cursor to...").
- Start the commit message with a capital letter.
- Limit the first line to 72 characters or less.
- Reference issues and pull requests after the first line.

Example: `Fix crash on instant download when clipboard is empty`

### Kotlin and Compose Code Style

- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use Jetpack Compose for all new UI components.
- Write declarative UI and try to keep components stateless where possible.
- Use Material 3 components and theming.
- Prefer `val` over `var` for immutability.
- Use descriptive naming for variables and functions.

### Python Code Style

- For any Python code (in `backend_logic`), follow [PEP 8](https://pep8.org/).
- Use meaningful variable and function names.

## Recognition

All contributors will be recognized. We deeply appreciate every contribution, whether it is a bug report, a documentation fix, or a major feature. Your efforts help make Gabi better for everyone. We will add your name to a `CONTRIBUTORS.md` file or in the release notes for the version you contribute to.

Thank you for being a part of the Gabi project. If you have any questions, you can reach out to the developer, hotaro, on Discord (`oi.hotaro`).
