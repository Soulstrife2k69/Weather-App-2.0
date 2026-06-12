<div align="center">
<img width="1200" height="475" alt="Diorama Weather Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# 🌤️ Diorama Weather

Diorama Weather is a modern Android weather application that transforms traditional forecasts into immersive miniature 3D diorama experiences. Instead of displaying plain weather cards, the app generates visually rich city-inspired landscapes featuring iconic landmarks, atmospheric lighting, dynamic weather conditions, and detailed forecasts.

Built with Android Studio and powered by Gemini, the application combines real-time weather intelligence with beautiful Material You design principles to create an engaging and premium weather experience.

## ✨ Features

* 🌍 Location-based weather reporting
* 🌡️ Real-time temperature and weather conditions
* ⏰ 5-hour detailed forecast
* 📅 5-day weather forecast
* 🏙️ AI-generated landmark and city diorama scenes
* 🎨 Material You adaptive theming
* 📱 Modern Android UI optimized for phones and tablets
* 🌤️ Dynamic sky gradients based on weather conditions
* 🧩 Custom adaptive app icon with premium 3D styling
* ⚡ Powered by Gemini AI

## 🏗️ Technology Stack

* Kotlin
* Android SDK
* Jetpack Compose
* Material 3
* Gemini API
* Android Studio

## 🎯 Design Philosophy

Diorama Weather focuses on transforming weather information into a visually memorable experience. Every location is represented as a miniature horizontal 3D-perspective diorama featuring landmarks, buildings, and environmental elements that reflect the character of the city while adapting to current weather conditions.

## 🚀 Run Locally

### Prerequisites

* Android Studio (latest stable version)
* Gemini API Key
* Android Emulator or Physical Android Device

### Setup

1. Clone this repository

```bash
git clone <repository-url>
```

2. Open Android Studio

3. Select **Open** and choose the project directory

4. Allow Android Studio to sync Gradle and resolve dependencies

5. Create a `.env` file in the project root and add:

```env
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

6. Remove the following line from:

```kotlin
app/build.gradle.kts
```

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

7. Build and run the application on an emulator or physical device

## 📸 Highlights

* Custom Material You Adaptive App Icon
* Rich dark gradient launcher background
* AI-generated weather narratives
* Dynamic landmark-based weather scenes
* Premium modern Android design language

## 🤖 AI Studio

View the original AI Studio project:

https://ai.studio/apps/2f5a5355-4618-48d4-bc78-9ef28fbf0f82

## 📄 License

This project is provided for educational and development purposes.
