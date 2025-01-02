# GFG GGV - Android Application

## Project Overview
This is an Android application built with modern Android development tools and practices, utilizing a modular architecture approach.

## Architecture & Project Structure
The project is structured into three main modules:

### 1. buildSrc
- Manages project dependencies using Kotlin DSL
- Centralizes dependency versions
- Makes dependency management type-safe

### 2. data
- Handles the data layer
- Contains Firebase API calls
- Manages data models and repositories
- Handles data source interactions

### 3. presentation
- Contains the UI layer
- Implements Jetpack Compose UI
- Contains ViewModels and UI states
- Handles user interactions

**Note:** Domain layer was intentionally omitted since this is a small, fast-build application. The business logic is minimal and handled directly between data and presentation layers.

## Tech Stack
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM
- **Dependency Injection:** Hilt
- **Navigation:** Voyager
- **Authentication:** Firebase Auth
- **Database:** Firebase Firestore
- **Storage:** Firebase Storage
- **Notifications:** Firebase Cloud Messaging
- **Image Loading:** Coil
- **Animations:** Lottie
- **Charts:** Vico
- **Async Operations:** Kotlin Coroutines & Flow
- **Serialization:** Kotlin Serialization
- **Excel Operations:** Apache POI

## Key Dependencies
- Jetpack Compose Material 3
- AndroidX Libraries
- Firebase Suite
- Hilt for DI
- Voyager Navigation
- Coil for Image Loading
- Lottie for Animations
- Vico for Charts
- Apache POI for Excel Operations

## Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17 or newer
- Android SDK with minimum API level 24

## Getting Started
1. Clone the repository:
```bash
git clone https://github.com/yourusername/gfg-ggv.git
