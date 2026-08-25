# Hermes WearOS Gateway

## Product Requirements Document (PRD)

### Version

v1.0

### Status

Draft

### Owner

Nasrul Fahmi

---

# 1. Executive Summary

Hermes WearOS Gateway adalah channel baru dalam ekosistem Hermes yang memungkinkan pengguna berinteraksi langsung dengan Hermes melalui perangkat Wear OS seperti Galaxy Watch 4.

Gateway ini diperlakukan sebagai channel setara dengan Telegram, WhatsApp, Discord, Slack, dan Dashboard Web, namun memiliki optimasi khusus untuk layar kecil, interaksi cepat, dan penggunaan berbasis suara.

Fokus utama proyek bukan membangun dashboard baru, melainkan menghadirkan pengalaman Hermes yang cepat, ringkas, dan kontekstual di smartwatch.

---

# 2. Problem Statement

Saat ini pengguna harus:

* Membuka dashboard web
* Membuka Telegram
* Membuka WhatsApp
* Membuka perangkat desktop

untuk berinteraksi dengan Hermes.

Untuk kebutuhan cepat seperti:

* Status server
* Status cron
* Ringkasan agenda
* Ringkasan market
* Pertanyaan singkat

proses tersebut terlalu lambat.

Smartwatch memberikan akses instan, namun membutuhkan pengalaman yang berbeda dari channel lain karena keterbatasan layar dan metode input.

---

# 3. Product Goals

### Goal 1

Memungkinkan pengguna mengirim pertanyaan ke Hermes langsung dari Wear OS.

### Goal 2

Mengoptimalkan respons Hermes untuk layar smartwatch.

### Goal 3

Menyediakan notifikasi real-time dari Hermes ke Wear OS.

### Goal 4

Menyediakan cron delivery khusus Wear OS.

### Goal 5

Menyediakan input berbasis suara dan keyboard.

---

# 4. Non Goals

Fitur berikut tidak termasuk dalam MVP:

* TTS
* Voice call realtime
* Video call
* Dashboard management
* Gateway management
* Memory management
* Log viewer lengkap
* File upload
* Image generation

Seluruh fungsi administrasi tetap berada pada Dashboard Hermes.

---

# 5. User Personas

## Persona A — Developer

Kebutuhan:

* Status server
* Status deployment
* Status container
* Status cron

## Persona B — Trader

Kebutuhan:

* Market summary
* Daily briefing
* Alert market

## Persona C — Power User

Kebutuhan:

* Chat cepat dengan Hermes
* Agenda harian
* Notifikasi prioritas

---

# 6. Functional Requirements

## FR-001 Voice Input

User dapat berbicara menggunakan mikrofon Wear OS.

### Flow

```text
Tap Mic
↓
Google STT
↓
Preview
↓
Send
```

### Acceptance Criteria

* STT bahasa Indonesia didukung
* STT bahasa Inggris didukung
* Hasil STT dapat diedit

---

## FR-002 Keyboard Input

User dapat mengetik menggunakan keyboard Wear OS.

### Acceptance Criteria

* Keyboard bawaan Wear OS didukung
* Pesan dapat dikirim ke Hermes

---

## FR-003 Review Before Send

User dapat meninjau hasil STT sebelum dikirim.

### Flow

```text
Voice
↓
STT
↓
Preview
↓
Edit
↓
Send
```

### Acceptance Criteria

* Edit tersedia
* Retry tersedia
* Send tersedia

---

## FR-004 Chat With Hermes

User dapat mengirim pertanyaan ke Hermes.

### Acceptance Criteria

* Respons diterima
* Respons ditampilkan
* Riwayat lokal tersedia

---

## FR-005 WearOS Response Mode

Hermes harus menggunakan prompt khusus Wear OS.

### Rules

* Maksimal 50 kata
* Fokus pada kesimpulan
* Tidak menampilkan reasoning
* Tidak menampilkan analisis panjang

### Example

Normal:

```text
Analisis 500 kata...
```

Wear:

```text
BTC masih bullish.
Support utama 118k.
Belum ada sinyal reversal.
```

---

## FR-006 Push Notification

Hermes dapat mengirim notifikasi ke Wear OS.

### Notification Types

#### Critical

```text
VPS Down
```

#### Important

```text
Cron Failed
```

#### Informational

Opsional.

---

## FR-007 Wear Only Cron

Cron tertentu dapat dikirim khusus ke Wear OS.

### Example

```yaml
cron:
  target:
    - wearos
```

### Use Cases

* Daily Briefing
* Market Summary
* Agenda Reminder
* Deployment Alert

---

## FR-008 Quick Actions

Shortcut bawaan.

### Actions

* Market
* Server
* Cron
* Agenda

---

## FR-009 Recent Chats

Menampilkan percakapan terakhir.

### Acceptance Criteria

* 5-10 chat terakhir
* Lokal cache

---

# 7. Non Functional Requirements

## Performance

### Response Time

Target:

```text
< 2 detik
```

untuk respons biasa.

---

## Battery

Target:

```text
< 2% per jam
```

saat idle.

---

## Security

* HTTPS only
* API Token Authentication
* Token rotation support

---

## Reliability

Reconnect otomatis saat koneksi terputus.

---

# 8. System Architecture

```text
Wear OS
   │
   │ HTTPS / WebSocket
   ▼
Wear Gateway
   │
   ▼
Hermes Core
   │
   ├── Agents
   ├── Memory
   ├── Tools
   ├── Cron
   └── Notifications
```

---

# 9. API Design

## Send Message

### Request

```http
POST /api/wear/chat
```

```json
{
  "message": "Status server?",
  "channel": "wearos"
}
```

### Response

```json
{
  "response": "Server normal. CPU 21%. RAM 58%."
}
```

---

## Device Registration

```http
POST /api/wear/register
```

---

## Fetch Notifications

```http
GET /api/wear/notifications
```

---

## Cron Feed

```http
GET /api/wear/cron
```

---

# 10. Hermes Configuration

```yaml
channels:
  wearos:

    enabled: true

    response:
      max_words: 50
      reasoning: false

    notifications:
      critical: true
      important: true
      informational: false

    history:
      recent_messages: 10

    input:
      voice: true
      keyboard: true

    review_before_send: true
```

---

# Design System

## Design Principles

### Fast

Maksimal 1-2 tap.

### Glanceable

Informasi harus dapat dipahami dalam < 3 detik.

### Minimal

Hindari teks panjang.

### Wear Native

Mengikuti pola Wear OS Material Design.

---

# Colors

## Primary

```text
#0284C7
```

## Success

```text
#059669
```

## Warning

```text
#F59E0B
```

## Error

```text
#DC2626
```

## Background

```text
#0F172A
```

## Surface

```text
#1E293B
```

---

# Typography

## Title

14sp

## Body

12sp

## Caption

10sp

---

# Components

## Ask Button

```text
🎤 Ask Hermes
```

---

## Send Button

```text
Send
```

---

## Edit Button

```text
Edit
```

---

## Retry Button

```text
Retry
```

---

## Notification Card

```text
⚠ Cron Failed

Tap for detail
```

---

## Status Card

```text
Server

CPU 21%
RAM 58%
```

---

# Project Structure

```text
wearos-gateway/

├── app/
│
├── data/
│   ├── api/
│   ├── repository/
│   └── models/
│
├── domain/
│   ├── usecases/
│   └── entities/
│
├── presentation/
│   ├── screens/
│   ├── components/
│   ├── navigation/
│   └── theme/
│
├── services/
│   ├── speech/
│   ├── notification/
│   └── websocket/
│
└── core/
    ├── network/
    ├── auth/
    └── utils/
```

---

# MVP Scope

## Included

* Voice STT
* Keyboard Input
* Review Before Send
* Chat
* Wear Tuned Responses
* Push Notifications
* Wear Only Cron
* Recent Chats

## Excluded

* TTS
* Realtime Voice
* Dashboard Management
* Gateway Management
* Log Viewer
* Memory Manager
* File Upload

---

# Success Metrics

### Daily Active Usage

Minimal 5 interaksi per hari.

### Message Success Rate

> 99%

### Notification Delivery Rate

> 95%

### Average Response Length

< 50 kata

### Crash Free Sessions

> 99.5%
