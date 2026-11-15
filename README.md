# XiaobaoYingYuan (小宝影院) APK 📺

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/architecture-Clean%20%2B%20MVVM-orange.svg)](https://developer.android.com/topic/libraries/architecture)
[![Website](https://img.shields.io/badge/website-live%20%E2%9C%A6-6C8CFF.svg)](https://nestchao.github.io/XiaobaoYingYuan_APK/)

**XiaobaoYingYuan (小宝影院) APK** is a modern, high-performance Android video streaming application designed for seamless playback of VOD (Video on Demand) content. Built from the ground up using **Kotlin**, **Jetpack Compose**, and **Clean Architecture with MVVM**, it offers a fluid, responsive, and native-feeling media experience on mobile and tablet devices.

---

## 📖 Table of Contents
- [About](#-about)
- [Aurora UI (v1.2.0)](#-aurora-ui-v120)
- [Key Features](#-key-features)
- [Architecture & Design Patterns](#-architecture--design-patterns)
- [Tech Stack & Libraries](#-tech-stack--libraries)
- [Data Flow & Scraping Engine](#-data-flow--scraping-engine)
- [Performance & Caching Strategy](#-performance--caching-strategy)
- [Getting Started](#-getting-started)
- [Releases](#-releases)

---

## ℹ️ About

**XiaobaoTV** acts as a client for aggregating and streaming video content by dynamically parsing VOD web interfaces. Unlike typical streaming clients that rely entirely on rigid, vulnerable APIs, XiaobaoTV utilizes a powerful Jsoup-based scraping and parsing pipeline to extract video metadata, stream links (including m3u8 streams), and play categories directly from the web. 

This hybrid approach, combined with robust multi-tier caching (Room DB + in-memory LRU) and ExoPlayer's optimized network streaming, delivers instant playback and zero-latency navigation.

---

## 🎨 Aurora UI (v1.2.0)

Version 1.2.0 is a full visual and interaction redesign — the **Aurora** design
language: a deep midnight canvas with slow-drifting aurora glows, frosted-glass
chrome, a single periwinkle→violet brand gradient, cinematic artwork and
spring-physics motion. It ships with light and dark palettes, a new adaptive
launcher icon and a reworked player.

**v1.3.0 — Aurora Player** adds a second player pass: movie-title top bar, a
five-button transport (episodes + ±10 s), screen lock, picture-in-picture,
aspect-ratio switching, a scrubbing time bubble, long-press 2× speed and a
slide-in episode panel on tablets. See
[docs/PLAYER_UPGRADE.md](docs/PLAYER_UPGRADE.md).

Highlights:

- **Adaptive navigation**: floating glass bottom pill on phones, an icon rail on
  portrait tablets and a labelled sidebar on large tablets.
- **Cinematic Home** with an infinite auto-advancing hero carousel (parallax
  artwork, glass CTAs), continue-watching rail, skeleton shimmer and
  pull-to-refresh.
- **Poster-first Detail** with a parallax backdrop, expandable synopsis, real
  source switching and an adaptive episode grid; two-pane on tablets.
- **Dark smoked-glass Player** with a hand-drawn buffered seek bar, animated
  volume/brightness medallion, episode bottom sheet, auto-next countdown ring
  and the original full gesture set.
- **Redesigned Category / Search / History / Settings**: animated filter pills,
  aurora search field with ranked discovery, date-grouped history with
  swipe-to-delete + undo, and a live theme studio with preview cards.

See [docs/UI_REDESIGN.md](docs/UI_REDESIGN.md) for the full design-system
breakdown, motion spec, tablet adaptation matrix and the screenshot gallery
(`docs/screenshots/`).

---

## ✨ Key Features

- 🏠 **Rich Home Screen**: Features trending carousels, categorized movie lists, and recent recommendations in a polished hero-style layout.
- 🔍 **Dynamic Search**: Scrapes custom video search results in real-time, bypassing typical server-side limitations.
- 🗂️ **Comprehensive Categories**: Fully paginated video listing with filter controls powered by Android Paging 3.
- 📜 **Local Search & Watch History**: Keeps track of your queries with database-backed automatic pruning.
- 🎬 **Premium Media Player**: Full-featured player built on ExoPlayer with dynamic controls:
  - Play/Pause, Fast Forward/Rewind, and Gesture controls.
  - Video stream source switcher (switching between different web players).
  - Episode selection drawer.
  - Auto-next episode play with visual countdown timer.
- 📱 **Adaptive UI**: Fully responsive layouts supporting both mobile and tablet devices — floating glass bottom navigation on phones, icon rail / labelled sidebar on tablets, two-pane detail and adaptive poster grids.
- ⚙️ **Custom Settings**: Adjust playback behavior, auto-next options, and general application preferences.

---

## 🏗️ Architecture & Design Patterns

The project strictly adheres to **Clean Architecture** combined with **MVVM (Model-View-ViewModel)**. This separates concerns, ensures high testability, and isolates the core business logic from framework-specific dependencies.

```
┌──────────────────────────────────────────────────────────┐
│                           ui/                            │  (Jetpack Compose, ViewModels)
└────────────────────────────┬─────────────────────────────┘
                             │ (Injects Use Cases)
                             ▼
┌──────────────────────────────────────────────────────────┐
│                         domain/                          │  (Business Logic, Models, Repository Interfaces)
└────────────────────────────▲─────────────────────────────┘
                             │ (Implements Interfaces)
                             ▼
┌──────────────────────────────────────────────────────────┐
│                          data/                           │  (Room, Retrofit, OkHttp, Jsoup, Cache managers)
└──────────────────────────────────────────────────────────┘
```

### Module Structure

- **`domain`**: Contains the pure business logic of the app.
  - `model/`: Plain Kotlin data classes representing entities like `VodContent`, `VideoSource`, `Episode`.
  - `repository/`: Interfaces defining contracts for data operations.
  - `usecase/`: Single-responsibility business cases.
- **`data`**: Houses concrete implementations of data access.
  - `remote/`: API definitions and HTTP communications.
  - `parser/`: Jsoup parsers tailored for scraping video structure.
  - `repository/`: Implements domain repositories, orchestrating caches, network, and database.
  - `cache/`: In-memory thread-safe caches (`DetailPageCache`, `PlaybackUrlCache`).
  - `local/`: Room Database configurations, Entities, and DAOs.
- **`ui`**: Jetpack Compose-driven UI layer.
  - Features package-by-feature organization (`home`, `detail`, `player`, `category`, `search`, `history`, `profile`).
  - Implements unidirectional data flow (UDF) via ViewModels exporting `StateFlow`.
- **`di`**: Hilt Dependency Injection modules keeping components decoupled and easy to configure.

---

## 🛠️ Tech Stack & Libraries

- **Language**: [Kotlin](https://kotlin.org)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Navigation**: Jetpack Navigation Compose
- **Asynchronous & Reactive**: Kotlin Coroutines and StateFlow/SharedFlow
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (KSP)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) (with 10MB HTTP disk cache)
- **HTML Scraping & Parsing**: [Jsoup](https://jsoup.org/)
- **Local Database**: [Room DB](https://developer.android.com/training/data-storage/room) for persistent local caching, settings, and histories
- **Media Playback**: [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) with native HLS streaming support
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (with 20% memory cache and 128MB disk cache)
- **Pagination**: [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) for infinite scrolling lists
- **Local Preferences**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Logging**: [Timber](https://github.com/JakeWharton/timber)

---

## 🔄 Data Flow & Scraping Engine

Because the API endpoints are frequently secured or change formats, the application integrates a robust **HTML scraping engine**:

1. **Detail Scraper (`VodDetailParser`)**: Extract details from `/movie/detail/{id}.html` to retrieve video specifications, tags, description, and cast list.
2. **Playpage Scraper (`PlayPageParser`)**: Fetches play page links, parses the multi-source video tabs, list of episodes, and extracts the direct `.m3u8` stream playlist directly from encrypted or obfuscated JavaScript variables.
3. **Show Scraper (`ShowPageParser`)** & **Search Scraper (`SearchPageParser`)**: Extracts movie list arrays from general navigation or search result grids.

---

## ⚡ Performance & Caching Strategy

Streaming video applications are extremely sensitive to latency. XiaobaoTV implements a meticulous caching architecture:

- 💾 **3-Tier Details Cache**: Network calls are decoupled using an in-memory LRU cache → Room SQLite DB → web network request fallback. Cached items in Room expire after a configurable 1-hour TTL.
- ⚡ **Prefetching Playback URLs**: The `DetailViewModel` preemptively resolves and decrypts streaming `.m3u8` playlists in the background as soon as the detail screen is opened. This allows ExoPlayer to load the stream instantly with zero spinner delay when the user clicks Play.
- 📂 **Network & Media Caching**: 
  - OkHttp enforces a 5-minute cache on the parsed HTML responses to avoid hammering the servers.
  - ExoPlayer relies on a custom 200MB disk cache to buffer media packets smoothly and reduce network overhead on cellular connections.

---

## 🚀 Getting Started

### Prerequisites
- JDK 17
- Android Studio Ladybug (or newer)
- Android SDK 35 (Target) / Android SDK 26 (Minimum)

### Build Commands

You can build the app via the command-line using Gradle:

```bash
# Clean build environment
./gradlew clean

# Build Debug APK (installs on emulator)
./gradlew assembleDebug

# Build optimized Release APK (Proguard/R8 enabled)
./gradlew assembleRelease

# Install Debug APK directly to a connected device
./gradlew installDebug
```

The compiled APK will be available in:
`app/build/outputs/apk/release/app-release.apk` (for Release builds) or `app/build/outputs/apk/debug/app-debug.apk` (for Debug builds).

---

## 📦 Releases

Official releases, including pre-compiled APKs, are published on the [GitHub Releases](https://github.com/nestchao/XiaobaoYingYuan_APK/releases) section of this repository.

To install:
1. Enable **Install Unknown Apps** in your Android Security Settings.
2. Download the latest `app-release.apk` from the Releases tab.
3. Open and install!

---

*Disclaimer: This app is a video scraper client that extracts information from publicly available web sources. We do not host or upload any media content.*
