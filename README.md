<div align="center">
  <img width="1200" height="475" alt="Diorama Weather Banner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# 🌤️ Diorama Weather

A modern Android weather application featuring immersive 3D diorama-inspired visualizations, real-time weather insights, adaptive Material You design, and location-aware forecasts. The app transforms traditional weather reporting into an engaging visual experience through beautifully crafted miniature city landscapes, iconic landmarks, dynamic atmospheric effects, and premium UI animations.

Built with Android Studio, Jetpack Compose, and Gemini AI, Diorama Weather combines accurate weather intelligence with artistic miniature-world representations to deliver forecasts that are both informative and visually stunning.

## 📱 App Preview

<div align="center">
  <img src="Screenshot%202026-06-12%20223149.png" alt="Diorama Weather Home Screen" width="320">
</div>

<p align="center">
  <em>Interactive 3D diorama weather visualization featuring AI-generated landmark scenery, real-time forecasts, and Material You design.</em>
</p>

---

## ✨ Features

### 🌍 Smart Location-Based Forecasts

* Weather information tailored to the selected city or region
* Real-time weather condition reporting
* Dynamic weather descriptions

### 🌡️ Detailed Weather Insights

* Current temperature and feels-like temperature
* Humidity and wind speed information
* Weather condition summaries
* Dynamic atmospheric visualization

### ⏰ Hourly Forecast

* Detailed 5-hour weather forecast
* Temperature trends throughout the day
* Condition changes displayed in real time

### 📅 Multi-Day Forecast

* Extended 5-day weather outlook
* Daily high and low temperatures
* Forecasted weather conditions

### 🏙️ AI-Generated Diorama Scenes

* Landmark-inspired miniature city landscapes
* Dynamic skyline generation
* Weather-adaptive visual themes
* Layered horizontal 3D-perspective diorama rendering

### 🎨 Material You Design

* Modern Android Material 3 interface
* Adaptive theming support
* Clean and responsive layouts
* Smooth animations and transitions

### 🧩 Premium Adaptive App Icon

* Custom-designed 3D weather diorama icon
* Material You adaptive icon support
* Rich dark-gradient background
* Optimized for Android launcher compatibility

### ⚡ Gemini AI Integration

* AI-powered weather scene generation
* Dynamic landmark selection
* Context-aware visual storytelling

---

## 🏗️ Technology Stack

| Technology      | Purpose                         |
| --------------- | ------------------------------- |
| Kotlin          | Android Application Development |
| Jetpack Compose | Modern UI Framework             |
| Material 3      | Design System                   |
| Gemini AI       | Scene & Weather Generation      |
| Android Studio  | Development Environment         |
| Gradle          | Build Automation                |

---

## 🎯 Design Philosophy

Diorama Weather reimagines traditional weather applications by representing each location as a miniature 3D world. Instead of presenting weather data as simple cards and charts, the application creates visually rich diorama scenes that combine landmarks, architecture, and environmental elements unique to the selected location.

Each generated scene adapts to current weather conditions through:

* Dynamic sky gradients
* Weather-based color palettes
* Atmospheric effects
* Landmark-specific styling

The result is a weather experience that feels informative, immersive, and memorable.

---

## 🚀 Run Locally

### Prerequisites

* Android Studio (Latest Stable Version)
* Gemini API Key
* Android Emulator or Physical Android Device

### Setup Instructions

1. Clone the repository

```bash
git clone https://github.com/Soulstrife2k69/Weather-App-2.0.git
```

2. Open Android Studio

3. Select **Open** and choose the project directory

4. Allow Android Studio to sync Gradle and resolve dependencies

5. Create a `.env` file in the project root directory

```env
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

6. Remove the following line from:

```text
app/build.gradle.kts
```

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

7. Build and run the application on an Android Emulator or Physical Device

---

## 📂 Project Structure

```text
Weather-App-2.0
│
├── app/
├── assets/
├── gradle/
├── .env.example
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
└── metadata.json
```

---

## 📸 Highlights

* Immersive 3D diorama weather visualization
* Real-time weather reporting
* AI-powered landmark generation
* Dynamic city-inspired scenery
* Material You adaptive design
* Custom adaptive launcher icon
* Responsive Android experience

---

## 🤖 AI Studio Project

View the original AI Studio project:

https://ai.studio/apps/2f5a5355-4618-48d4-bc78-9ef28fbf0f82

---

## 👨‍💻 Author

**Shubhojit Nandy**

Computer Science Engineering Student (AI & ML)

GitHub: https://github.com/Soulstrife2k69

---

## 📄 License

This project is available for educational, learning, and portfolio purposes.

Feel free to explore, modify, and extend the application.
