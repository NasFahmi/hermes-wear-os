[11.45, 5/9/2026] Fahmi Byu: Berikut adalah Product Requirement Document (PRD) terstruktur yang dibagi menjadi dua bagian: Bagian A (Server/VPS Backend) dan Bagian B (Client App Android Wear OS).

---

📄 PRD: Hermes Wear OS Next-Gen Architecture (SSE Streaming + FCM Auto-Push)

- Author: Fahmi & Hermes
- Status: Draft / Ready for Implementation
- Target Platform: VPS Backend (FastAPI / Python) & Smartwatch Client (Android / Kotlin)

---

BAGIAN A: PRD BACKEND VPS (hermes-wearos)

1. Objective
Mengeliminasi masalah 504 Timeout dan Connection Abort, menyediakan responsif instan dengan token streaming, serta mengalirkan notifikasi real-time (hasil chat tertunda & cronjob) langsung ke pergelangan tangan via FCM.

---

2. System Architecture & Component Changes

2.1. Database Schema Update (/root/.hermes/wearos.db)
Update tabel devices dan notifications untuk menyimpan FCM Token dan history chat.
sql
-- Tambah kolom fcm_token pada tabel devices jika belum ada
ALTER TABLE devices ADD COLUMN fcm_token TEXT;

-- Tabel task tracking untuk status async/stream
CREATE TABLE IF NOT EXISTS chat_tasks (
    task_id TEXT PRIMARY KEY,
    device_id TEXT,
    prompt TEXT,
    response TEXT,
    status TEXT, -- PENDING, STREAMING, COMPLETED, DISCONNECTED_FALLBACK
    created_at INTEGER,
    completed_at INTEGER
);


2.2. Endpoint Requirements

A. POST /api/wear/chat/stream (New Streaming Endpoint)
* Protocol: Server-Sent Events (text/event-stream).
* Headers Wajib:
  * Authorization: Bearer <WEARO...EN>
  * X-Channel: wearos
  * X-Device-Id: <DEVICE_ID>
* Request Body:
  json
  {
    "message": "berapa pengeluaran di bulan agustus kemarin"
  }
  
* Event Stream Format:
  * data: {"type": "start", "taskId": "task-uuid-123"}\n\n
  * data: {"type": "token", "content": "Pengeluaran "}\n\n
  * data: {"type": "token", "content": "Agustus "}\n\n
  * data: {"type": "token", "content": "2026: Rp 1.938.972."}\n\n
  * data: {"type": "done", "fullResponse": "..."}\n\n

B. Connection Abort & FCM Fallback Engine
* Logic:
  1. Jika client menutup koneksi (layar jam mati) di tengah streaming:
     - Worker thread di FastAPI tetap berjalan di background sampai tuntas.
     - Status task diubah menjadi DISCONNECTED_FALLBACK.
  2. Saat respons selesai digenerate:
     - Backend mengambil fcm_token device dari database SQLite.
     - Backend mengirimkan payload FCM Data/Notification ke Firebase Admin SDK.

C. POST /api/wear/register (Updated)
* Menyimpan/memperbarui FCM token saat device registrasi/refresh token.
json
{
  "deviceId": "galaxy-watch-4-001",
  "deviceModel": "Galaxy Watch 4",
  "fcmToken": "c8xK...fcm_token_here"
}


D. Cronjob Delivery Bridge (send_wear_push.py)
* Helper script terintegrasi cronjob Hermes.
* Menerima input teks laporan dari cron (contoh: Morning Market Report / US Session Trade Plan), memformat teks ke <= 40 kata, menyimpannya ke database notifications, lalu menembakkannya ke seluruh device aktif via FCM.

---

3. Dependencies & Credentials VPS
* firebase-admin (Python package di virtualenv Hermes).
* /root/.hermes/firebase_service_account.json (Google Cloud Service Account Key).

---

BAGIAN B: PRD CLIENT WEAR OS (Android / Kotlin)

1. Objective
Menampilkan respons teks streaming secara realtime (animasi mengetik), mencegah freeze/timeout pada HTTP client, serta menangani notifikasi push native dari FCM saat aplikasi sedang tidak aktif.

---

2. Functional Requirements

2.1. Module Firebase Cloud Messaging (FCM)
* SDK: com.google.firebase:firebase-messaging-ktx
* Workflow:
  1. Pada MainActivity / App Startup: Ambil FCM Token device via FirebaseMessaging.getInstance().token.
  2. Panggil API POST /api/wear/register untuk mendaftarkan token ke VPS.
  3. Buat MyFirebaseMessagingService:
     - Tangani onMessageReceived(remoteMessage):
       - Buat Android Native Notification dengan channel Hermes Updates. (1/2)
[11.45, 5/9/2026] Fahmi Byu: - Aktifkan getar (Vibration Pattern) dan prioritas tinggi (High Priority Notification).
       - Saat notifikasi di-tap, buka aplikasi dan arahkan ke detail pesan.

2.2. Chat Streaming Screen (SSE Listener)
* HTTP Client: Gunakan okhttp3:okhttp-sse (OkHttp Server-Sent Events).
* UI/UX Lifecycle:
  1. User input chat (Voice / Text).
  2. Inisialisasi SSE Connection:
     kotlin
     val request = Request.Builder()
         .url("http://43.134.102.35:8000/api/wear/chat/stream")
         .addHeader("Authorization", "Bearer $API_TOKEN")
         .addHeader("X-Channel", "wearos")
         .addHeader("X-Device-Id", deviceId)
         .post(jsonBody)
         .build()
     
  3. Event Handling:
     - Event token: Langsung lakukan append teks ke state UI (Compose / TextView) secara realtime.
     - Event done: Tandai status streaming selesai.
  4. Ambient Mode Handling:
     - Jika layar mati / user keluar aplikasi sebelum event done, biarkan socket terputus secara wajar (OS akan menangani notifikasinya via FCM saat jawaban selesai).

---

3. Non-Functional Requirements
1. Baterai: Tidak ada background persistent WebSocket saat aplikasi ditutup. Semua event pasif ditangani via Google Play Services (FCM).
2. Latensi Render UI: Token pertama harus mulai muncul di layar jam dalam waktu < 1.5 detik sejak tombol send ditekan.
3. Format Teks: UI otomatis membatasi render maksimal teks agar pas dengan layar lingkaran smartwatch.

---

🚀 Implementation Checklist & Next Steps

1. Step 1 (User): Buat project Firebase di Google Firebase Console, buat Android App ID, download google-services.json (untuk Android) dan generate firebase_service_account.json (untuk VPS).
2. Step 2 (Hermes): Update wearos_gateway.py untuk SSE streaming endpoint + FCM push handler di VPS.
3. Step 3 (User): Pasang firebase-messaging & okhttp-sse pada project Android Studio Wear OS.
4. Step 4: Uji coba integrasi end-to-end (Streaming langsung + Tes tutup jam + Notifikasi cronjob). (2/2)