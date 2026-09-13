# Hermes Wear OS Gateway ⌚🤖

[![Platform](https://img.shields.io/badge/Platform-Wear%20OS%203.0%2B%20(API%2030%2B)-blue.svg?logo=android&logoColor=white)](https://developer.android.com/wear)
[![Language](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose%20for%20Wear-green.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/training/wearables/compose)
[![DI](https://img.shields.io/badge/DI-Hilt-orange.svg)](https://dagger.dev/hilt/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Hermes Wear OS Gateway** adalah aplikasi *standalone smartwatch client* dalam ekosistem **Hermes AI Assistant**. Aplikasi ini dirancang khusus untuk interaksi cepat, ringkas, dan *hands-free* langsung dari pergelangan tangan Anda (misal: Samsung Galaxy Watch, Google Pixel Watch, TicWatch).

---

## 🚀 Fitur Utama

- 🎙️ **Hands-Free Direct Voice Input (Gemini-Style)**:
  - Otomatis membuka mikrofon saat aplikasi dibuka (*cold launch voice*).
  - Sinkronisasi status engine Google SpeechRecognizer dengan haptic feedback presisi saat mic benar-benar siap merekam.
  - Tampilan teks *live partial transcription* secara *real-time*.
  - Auto-send cerdas: pesan terkirim otomatis setelah jeda hening bicara (2.2 detik) tanpa perlu menyentuh layar.
  - Fallback ke dialog sistem Wear OS jika diperlukan.
- 💬 **Tampilan Khusus Layar Bundar (Watchface UX)**:
  - Antarmuka dioptimalkan untuk layar kecil dengan `ScalingLazyColumn`, `RotaryScroll`, dan tipografi *high-contrast* Hermes.
  - Format respon AI yang padat, to-the-point, dan mudah dibaca sepintas.
- 🔔 **Real-Time Push Notifications**:
  - Terintegrasi dengan Firebase Cloud Messaging (FCM) untuk penerimaan alert server, status cron job, dan pengingat penting secara instan.
- 🌐 **Standalone Application**:
  - Berjalan mandiri langsung di jam tangan via Wi-Fi atau koneksi data tanpa wajib membuka companion app di smartphone.
- ⚙️ **Pengaturan Fleksibel**:
  - Konfigurasi gateway server Hermes, pemilihan bahasa pengenalan suara (Bahasa Indonesia `id-ID` atau English `en-US`), serta autentikasi API token.

---

## 🛠️ Tech Stack & Arsitektur

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose for Wear OS (`androidx.wear.compose`)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture Layering
- **Dependency Injection**: Dagger Hilt
- **Asynchronous / Reactive**: Kotlin Coroutines & `StateFlow`
- **Speech Engine**: Android `SpeechRecognizer` API + On-Device Hardware Accelerator (Android 12+)
- **Networking**: OkHttp, WebSocket, Retrofit
- **Cloud Messaging**: Firebase Cloud Messaging (FCM)
- **Local Storage**: Jetpack DataStore Preferences

---

## 📁 Struktur Direktori

```text
hermes-wear-os/
├── app/
│   ├── src/main/java/com/hermes/wearos/
│   │   ├── core/                  # Utility, autentikasi, dan konfigurasi global
│   │   ├── data/                  # Repository, network API, dan local data sources
│   │   ├── di/                    # Modul Hilt Dependency Injection
│   │   ├── domain/                # UseCase dan model bisnis
│   │   ├── presentation/          # Jetpack Compose Screens, ViewModel, & UI Theme
│   │   │   ├── screens/           # AssistantScreen, SettingsScreen, dsb.
│   │   │   ├── theme/             # HermesColors, HermesTypography
│   │   │   └── viewmodel/         # ChatViewModel, VoiceViewModel, dsb.
│   │   ├── services/              # Speech Recognizer & FCM Background Services
│   │   └── HermesWearApplication.kt
│   ├── src/main/res/              # Drawable vektor, layout, dan resources
│   ├── google-services.json.example # Template konfigurasi Firebase
│   └── build.gradle.kts
├── gradle/                        # Gradle wrapper files
├── run_wearos.bat                 # Script one-click build & run (Emulator/ADB)
├── prd.md                         # Product Requirements Document
└── README.md
```

---

## ⚙️ Persiapan & Instalasi

### 1. Prasyarat
- **Android Studio** (Koala / Ladybug / Meerkat atau yang lebih baru)
- **JDK 17** atau **JDK 21**
- Perangkat fisik **Wear OS 3.0+** (API Level 30+) atau **Wear OS Emulator** (Round Watch)

### 2. Konfigurasi Firebase
Aplikasi menggunakan Firebase Cloud Messaging untuk notifikasi real-time:
1. Gandakan file template konfigurasi:
   ```bash
   cp app/google-services.json.example app/google-services.json
   ```
2. Isi file `app/google-services.json` dengan kredensial project Firebase Anda dari Firebase Console.
   > **Catatan**: File `app/google-services.json` sudah dimasukkan ke `.gitignore` demi keamanan kunci API.

---

## 🏃 Menjalankan Aplikasi

### Opsi A: Menggunakan Script Otomatis (Windows)
Tersedia script praktis untuk kompilasi dan peluncuran ke emulator/device:
```cmd
run_wearos.bat
```

### Opsi B: Menggunakan Gradle CLI
Untuk kompilasi APK Debug:
```bash
./gradlew assembleDebug
```

Untuk menjalankan unit test:
```bash
./gradlew testDebugUnitTest
```

---

## ⌚ Deployment ke Jam Tangan Fisik via Wireless Debugging

1. **Aktifkan Developer Options di Jam Tangan**:
   - Masuk ke **Settings** > **System** > **About** > ketuk **Build number** 7 kali.
2. **Aktifkan Wireless Debugging**:
   - Buka **Settings** > **Developer options** > aktifkan **Wireless debugging**.
   - Pastikan jam tangan dan PC Anda terhubung ke jaringan Wi-Fi yang sama.
3. **Lakukan Pairing (Hanya sekali)**:
   - Masuk ke **Pair new device with pairing code**.
   - Catat IP, Port Pairing, dan 6 digit kode.
   - Jalankan di terminal PC:
     ```bash
     adb pair <IP_JAM>:<PORT_PAIRING> <KODE_6_DIGIT>
     ```
4. **Hubungkan & Install**:
   - Lihat **IP address & Port** di halaman utama Wireless debugging.
   - Hubungkan ADB:
     ```bash
     adb connect <IP_JAM>:<PORT_KONEKSI>
     ```
   - Pasang dan jalankan APK:
     ```bash
     adb -s <IP_JAM>:<PORT_KONEKSI> install -r app/build/outputs/apk/debug/app-debug.apk
     adb -s <IP_JAM>:<PORT_KONEKSI> shell am start -n com.hermes.wearos/.presentation.MainActivity
     ```

---

## 📄 Lisensi & Kontributor

- **Pemilik Proyek**: Nasrul Fahmi
- Proyek ini dirilis di bawah lisensi **[MIT License](LICENSE)** — bebas digunakan, dimodifikasi, dan didistribusikan untuk keperluan personal maupun komersial.
- Dikembangkan sebagai bagian dari ekosistem **Hermes AI Assistant**.
