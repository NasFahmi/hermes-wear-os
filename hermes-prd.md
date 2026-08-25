# Hermes WearOS — Integration & Configuration PRD

## Dokumen Spesifikasi Konfigurasi & Integrasi Hermes Core Backend

### Versi
v1.0

### Status
Official / Ready for Implementation

### Target Ekosistem
* **Client**: Hermes WearOS Gateway (Android / Galaxy Watch 4+)
* **Backend**: Hermes Core Server & Gateway Dispatcher
* **Channel**: `wearos`

---

# 1. Ringkasan Eksekutif (Executive Summary)

Dokumen ini adalah Product Requirements Document (PRD) teknis yang secara khusus mengatur **spesifikasi integrasi, kontrak API, skema autentikasi, serta parameter konfigurasi** antara **Hermes WearOS Gateway** dan **Hermes Core Backend**.

Integrasi ini bertujuan untuk menghubungkan perangkat smartwatch secara mulus, hemat daya (battery-friendly), aman (HTTPS/WSS + Bearer Token), serta mampu memberikan respons cepat (< 2 detik) yang telah disesuaikan dengan limitasi layar Wear OS (maksimal 50 kata).

---

# 2. Arsitektur Koneksi & Protokol

```text
┌────────────────────────────────────────────────────────┐
│               Wear OS Device (Smartwatch)              │
│  [Speech STT]  [Wear Compose UI]  [DataStore Config]   │
└──────────────────────────┬─────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           │ HTTP/REST (Port 8000/443)     │ WebSocket (WSS)
           │ • /api/wear/chat              │ • /ws (Live Events)
           │ • /api/wear/server            │ • Push Notifications
           │ • /api/wear/notifications     │ • Reconnect Exponential
           │ • /api/wear/cron              │
           ▼                               ▼
┌────────────────────────────────────────────────────────┐
│                 Hermes Core Gateway                    │
│   • Auth Interceptor (`X-Channel: wearos`, Bearer)     │
│   • Response Formatter (Wear Limit: ≤ 50 words)        │
│   • Agent Dispatcher (Hermes Tools, Memory, Cron)      │
└────────────────────────────────────────────────────────┘
```

---

# 3. Konfigurasi Hermes Core (`hermes.yaml`)

Di sisi server Hermes Core, gateway Wear OS didaftarkan sebagai channel resmi setara dengan Telegram/Discord dengan parameter pemangkasan respons khusus:

```yaml
# ========================================================
# HERMES CORE CONFIGURATION FOR WEAR OS CHANNEL
# ========================================================

channels:
  wearos:
    # Mengaktifkan gateway Wear OS
    enabled: true
    
    # Konfigurasi pembatasan karakter & prompt tuning
    response:
      max_words: 50              # Maksimal 50 kata untuk efisiensi layar jam
      reasoning: false           # Tidak menyertakan chain-of-thought internal
      format: "plain_text"       # Hindari tabel markdown lebar atau HTML kompleks
      include_timestamp: true
    
    # Pengaturan notifikasi push ke Smartwatch
    notifications:
      critical: true             # Push langsung (VPS down, security alert)
      important: true            # Push harian (Cron fail, target price hit)
      informational: false       # Opsional / diabaikan untuk menghemat baterai
      max_queue_size: 20
    
    # Konfigurasi Cron Feed khusus Wear OS
    cron:
      enabled: true
      target_tag: "wearos"
      default_feed_limit: 10
    
    # Autentikasi & Batasan Akses
    security:
      require_auth: true
      token_header: "Authorization"
      channel_header: "X-Channel"
      rate_limit:
        requests_per_minute: 60
        burst: 10
```

---

# 4. Kontrak API & Spesifikasi Endpoint (API Contract)

Semua request dari Wear OS wajib menyertakan header standar berikut:

```http
Authorization: Bearer <API_TOKEN>
X-Channel: wearos
Content-Type: application/json
```

---

## 4.1. Endpoint: Kirim Pesan / Tanya Hermes (Chat)

Mengirim pertanyaan berbasis suara (STT) atau teks ke Hermes dan menerima respons ringkas.

* **Endpoint**: `POST /api/wear/chat`
* **Content-Type**: `application/json`

### Request Body:
```json
{
  "message": "Status server?",
  "channel": "wearos"
}
```

### Response Body (200 OK):
```json
{
  "response": "Server normal. CPU 21%. RAM 58%. Uptime 14 hari.",
  "timestamp": 1723872000000
}
```

### Status Code & Error:
* `200 OK`: Berhasil diproses.
* `400 Bad Request`: Payload kosong atau tidak valid.
* `401 Unauthorized`: Token API tidak valid atau expired.
* `500 Internal Server Error`: Backend Hermes gagal memproses prompt.

---

## 4.2. Endpoint: Status Server & Health Monitor

Menampilkan widget cepat CPU, RAM, Disk, dan Uptime pada layar Home Wear OS.

* **Endpoint**: `GET /api/wear/server`

### Response Body (200 OK):
```json
{
  "cpu": 21.4,
  "ram": 58.7,
  "disk": 42.0,
  "uptime": 1209600,
  "status": "NORMAL"
}
```

*Enum Status: `NORMAL`, `WARNING`, `CRITICAL`, `OFFLINE`*

---

## 4.3. Endpoint: Feed Notifikasi (Notifications Feed)

Mengambil riwayat alert aktif untuk ditampilkan di layar *Notifications*.

* **Endpoint**: `GET /api/wear/notifications`

### Response Body (200 OK):
```json
[
  {
    "id": "notif-001",
    "type": "CRITICAL",
    "title": "VPS Down",
    "body": "VPS Production SG-1 tidak merespon ping 5 menit terakhir.",
    "timestamp": 1723871940000,
    "actionUrl": null
  },
  {
    "id": "notif-002",
    "type": "IMPORTANT",
    "title": "Cron Failed",
    "body": "Backup database harian gagal pada langkah kompresi.",
    "timestamp": 1723868400000,
    "actionUrl": null
  }
]
```

*Enum Type: `CRITICAL`, `IMPORTANT`, `INFORMATIONAL`*

---

## 4.4. Endpoint: Feed Cron Khusus Smartwatch (Wear Cron Feed)

Mengambil ringkasan briefing terjadwal yang ditargetkan untuk Wear OS.

* **Endpoint**: `GET /api/wear/cron`

### Response Body (200 OK):
```json
[
  {
    "id": "cron-101",
    "name": "Daily Briefing",
    "message": "BTC stabil di $118k. Server load 18%. Agenda: Meeting 14:00.",
    "timestamp": 1723852800000,
    "target": "wearos"
  }
]
```

---

## 4.5. Endpoint: Registrasi Perangkat (Device Registration)

Mendaftarkan `deviceId` smartwatch untuk routing push notification khusus.

* **Endpoint**: `POST /api/wear/register`

### Request Body:
```json
{
  "deviceId": "samsung-gw4-98af12",
  "deviceModel": "Galaxy Watch4",
  "platform": "wearos"
}
```

---

# 5. Konfigurasi Client Wear OS (`ApiConfig.kt` & DataStore)

Pada aplikasi client, parameter koneksi diatur melalui struktur konfigurasi berikut:

### 5.1. Default Configuration (`ApiConfig.kt`)
```kotlin
object ApiConfig {
    // Alamat default server Hermes aktif
    const val DEFAULT_BASE_URL = "http://43.134.102.35:8000/"
    const val DEFAULT_WS_URL = "ws://43.134.102.35:8000/ws"
    const val DEFAULT_API_TOKEN = "d0422c491b333cdb7287318bdd7bc6ab"

    // Timeout jaringan smartwatch
    const val CONNECT_TIMEOUT = 15_000L  // 15 detik
    const val READ_TIMEOUT = 30_000L     // 30 detik

    // Metadata channel
    const val WEAR_CHANNEL = "wearos"
    const val MAX_RESPONSE_WORDS = 50
}
```

### 5.2. DataStore Keys (`AuthManager.kt`)
| Kunci Preference | Tipe | Nilai Default | Deskripsi |
| :--- | :--- | :--- | :--- |
| `server_url` | `String` | `http://192.168.1.50:8000/` | Base URL Hermes Core aktif |
| `api_token` | `String` | `null` | Token otentikasi Bearer |
| `voice_language`| `String` | `id-ID` | Bahasa input suara (`id-ID` / `en-US`) |
| `device_id` | `String` | Auto-generated UUID | ID unik jam tangan |

---

# 6. Matriks Lingkungan & Panduan Deployment (Environment Matrix)

| Lingkungan | Alamat Target Server | Protokol | Catatan Konfigurasi |
| :--- | :--- | :--- | :--- |
| **Android Studio Emulator** | `http://10.0.2.2:8000/` | HTTP Cleartext | `10.0.2.2` adalah alias host mesin pengembang dari emulator Android. |
| **Smartwatch Fisik (Wi-Fi Lokal)** | `http://192.168.X.X:8000/` | HTTP Cleartext | Smartwatch dan laptop server harus berada di 1 jaringan Wi-Fi yang sama. |
| **Server Production / VPS** | `https://hermes.domainanda.com/` | HTTPS (TLS 1.3) | Menggunakan sertifikat SSL/TLS valid; WSS untuk WebSocket. |
| **Cloudflare Tunnel / Ngrok** | `https://xxxx.trycloudflare.com/` | HTTPS Reverse Proxy | Cocok untuk uji coba jarak jauh tanpa membuka port router. |

---

# 7. Penanganan Kesalahan & Ketahanan Jaringan (Reliability & Failover)

1. **Auto Reconnect WebSocket**:
   - Jika koneksi WebSocket terputus, client melakukan reconnect otomatis dengan exponential backoff: 5s, 10s, 15s, 20s, 25s (maksimal 5 kali percobaan).
2. **Offline Fallback & Room Local Cache**:
   - Riwayat chat dan respons terakhir disimpan ke database Room lokal (`hermes_chat.db`).
   - Jika jaringan mati, riwayat tetap dapat dibaca secara instan.
3. **Timeout & Battery Optimization**:
   - HTTP Timeout dibatasi maksimal 30 detik untuk menghindari pengurasan baterai jika server mengalami hang.
   - Background polling hanya dijalankan saat aplikasi dibuka (on-demand refresh).
4. **Token Invalidation (401 Handling)**:
   - Jika server mengembalikan HTTP 401, client menampilkan indikator *Offline / Auth Required* dan memberikan opsi input ulang token di menu Settings.

---

# 8. Checklist Verifikasi Integrasi (Verification Checklist)

Sebelum deploy ke jam tangan pengguna, lakukan pengujian berikut:

- [ ] **Ping & Healthcheck**: Akses `GET /api/wear/server` mengembalikan status CPU & RAM.
- [ ] **Voice Query**: Tekan *🎤 Ask Hermes* di smartwatch, rekam "Status server", pastikan respons muncul dalam < 2 detik.
- [ ] **Review Before Send**: Pastikan tombol *Edit*, *Retry*, dan *Send* berfungsi sebelum mengirim prompt suara.
- [ ] **Quick Actions**: Uji tombol *Market*, *Server*, *Cron*, dan *Agenda* membuka chat dan menampilkan data akurat.
- [ ] **Settings Connection Test**: Tekan *🔍 Tes Koneksi* di layar Settings smartwatch dan pastikan menghasilkan label `✓ Terhubung`.
