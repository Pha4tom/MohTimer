# MohTimer

A minimalist, dark-themed interval timer for calisthenics, CrossFit, HIIT, and functional fitness workouts. Built entirely on Android with Kotlin.

No ads. No accounts. No subscriptions. Just a timer that does what it says.

## ✨ Features

### Timer Modes
- **EMOM** — Every Minute On a Minute. Set interval length and total rounds.
- **AMRAP** — As Many Rounds As Possible within a fixed time.
- **For Time** — Count up until you finish your workout.
- **Tabata** — Classic 20s work / 10s rest interval training.
- **Timer** — Simple countdown for any duration.

### Voice & Sound
- Text-to-speech voice cues: "Round one / Let's go", "Halfway there", "10 seconds", "Last round"
- Three synthesized beep themes: Classic, Digital, Alarm — generated in real time with `AudioTrack` (no files, no notification sounds)
- Loud triple-beep at 3-2-1 seconds for every round transition
- Optional vibration on countdown and round transitions
- Male / Female / System voice selection

### Customization
- Choose your timer accent color from 5 presets
- Adjust GET READY countdown duration (3s / 5s / 10s / 15s)
- Toggle voice cues, beeps, vibration, and notifications independently
- Keep-screen-on option for hands-free viewing during workouts
- Count up (instead of down) for AMRAP and EMOM

### Presets
- Save any configuration as a reusable preset
- Swipe to delete, with confirmation
- One-tap launch from the presets screen
- "Save as preset" button on every configuration screen

### Background Operation
- Foreground service with persistent notification
- Continues running when the app is minimized or the screen is off
- Wake lock ensures accurate timing even in deep sleep
- Tap the notification to jump back into the running timer
- Long-press the X in the timer to stop it completely

### Workout Modes Visual Identity
Each mode has its own accent color:
- AMRAP — Orange
- For Time — Blue
- EMOM — Magenta
- Tabata — Green
- Timer — Blue

## 📱 Screenshots

Coming soon.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Minimum SDK**: 26 (Android 8.0)
- **UI**: Material 3 + XML layouts
- **Storage**: SQLite for presets
- **Sound**: `AudioTrack` (synthesized beeps) + `TextToSpeech`
- **Background**: Foreground Service with notification
- **Concurrency**: `Handler`-based ticker loop

## 🏗️ Building

### Prerequisites
- Android SDK 34+
- JDK 17
- Gradle 8+

### From Android Studio
1. `File → Open` → select the project root
2. Sync Gradle
3. Run on a device or emulator

### From the command line
```bash
./gradlew assembleDebug
