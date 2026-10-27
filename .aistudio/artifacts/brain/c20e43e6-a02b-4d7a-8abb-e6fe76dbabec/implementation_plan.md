# AuraSound DSP: Pemutar Musik Cerdas & Audio Studio

AuraSound DSP adalah aplikasi pemutar audio Android kelas audiofil dan studio portabel yang menggabungkan mesin pemrosesan suara canggih (10-Band Equalizer, Bass Booster, Dolby/3D Spatial Surround, Reverb), visualizer audio reaktif multi-mode (Spektrum Batang, Gelombang Neon, Circular Pulse), pemutar offline stabil ala JetAudio, integrasi katalog daring dengan sorotan artis **wins** (Nebraska/Texas underground hip-hop & lo-fi), pembuatan playlist cerdas bertenaga AI Gemini sesuai mood dan genre pengguna, lirik tersinkronisasi realtime (LRC format), serta penempatan iklan yang estetik dan tidak mengganggu navigasi.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Keputusan Terkonfirmasi dari Tanya-Jawab:**
> - **Katalog Daring & Artis Wins:** Aplikasi menyertakan katalog daring siap-putar terintegrasi dengan diskografi artis **wins** (*b.tape*, *chill*, *Love, wins*, *price*, *whoa*, *gotham*, dll.) serta stasiun streaming audio berkualitas tinggi, di samping pemutar pustaka lokal offline.
> - **Visualizer Utama:** Tampilan visualizer multi-mode yang dapat diubah sesuai preferensi (Spektrum Batang 32-bar dinamis, Gelombang Neon halus, dan Circular Pulse berdetak mengikuti ketukan bass).
> - **Sound Processing Engine:** DSP Studio lengkap mencakup 10-Band Graphic Equalizer (31Hz - 16kHz) dengan preset (Bass Boost, Rock, Jazz, Vocal, Hip-Hop, Acoustic), Bass Booster dinamis, 3D Spatial Virtualizer (efek surround ala Dolby Audio), serta Reverb Chamber/Hall simulation.
> - **Monetisasi & Iklan:** Interstitial/Splash ad selamat datang yang elegan dengan tombol lewati instan pada peluncuran pertama, dan banner iklan ramping di sisi bawah yang tersemat rapi di atas sistem navigasi tanpa menutupi mini-player atau kontrol pemutaran.
> - **AI Smart Playlist Generator:** Menggunakan Gemini AI untuk menganalisis suasana hati (mood: Santai, Fokus, Semangat, Melankolis, Workout, Malam Hari) dan preferensi genre, lalu secara otomatis menyusun antrean putar cerdas yang memadukan lagu-lagu lokal, streaming, dan trek unggulan dari artis Wins.

---

## 1. Overview & Core Concept

- **Tujuan Aplikasi:** Menghadirkan pengalaman mendengarkan musik terbaik di perangkat seluler dengan perpaduan kontrol audio profesional, visualisasi musik imersif, dan kecerdasan kurasi playlist instan.
- **Target Pengguna:** Penggemar musik yang menginginkan kualitas audio maksimal (bass bertenaga, surround jernih), penggemar musik Lo-Fi/Hip-Hop artis Wins, serta pengguna yang menyukai tampilan visualizer artistik dan lirik lagu berjalan.
- **Nilai Utama:**
  - Tidak hanya memutar audio, aplikasi memproses suara secara real-time dengan efek audio kelas studio (Android `Equalizer`, `BassBoost`, `Virtualizer`, dan `PresetReverb`).
  - Pemutar audio offline yang tangguh mendukung berbagai format file (MP3, FLAC, WAV, AAC, OGG) dengan pembacaan metadata ID3, cover album, dan folder navigation.
  - Playlist AI interaktif yang merespons mood dan cuaca/waktu pengguna, mengintegrasikan katalog artis Wins secara cerdas.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Ad Transition:** Pengguna membuka aplikasi -> dialog peluncuran estetis (Welcome Promo / Interstitial Ad) muncul sejenak dengan opsi "Lanjutkan ke Musik" atau hitung mundur otomatis -> masuk ke Beranda Musik.
2. **Browsing & Discovery:**
   - Tab **Pustaka (Offline & Koleksi):** Menampilkan trek perangkat, album, artis, dan folder penyimpanan lokal.
   - Tab **Daring & Wins:** Menampilkan diskografi khusus artis Wins (album *b.tape*, lagu *chill*, *whoa*, *step*), rilis populer, dan genre tags.
   - Tab **AI Playlist Generator:** Pengguna memilih mood (misal: "Santai di Sore Hari", "Fokus Belajar", "Energi Gym") & genre -> Gemini AI menganalisis dan menghasilkan playlist tematik yang memadukan trek offline, katalog daring, dan lagu Wins.
3. **Now Playing & Visualizer:**
   - Tap lagu apa pun untuk membuka **Player Layar Penuh**.
   - Cover art berputar lembut dengan efek visualizer di latar belakang atau mode visualizer layar penuh khusus.
   - Tombol toggle untuk berganti tipe visualizer: *32-Band Neon Spectrum*, *Liquid Waveform*, atau *Circular Bass Pulse*.
   - Tab/tombol **Lirik Tersinkronisasi**: Menampilkan lirik berbaris dengan highlight kata/baris berjalan sesuai timestamp lagu (LRC-style).
4. **DSP Studio & Dolby Effects:**
   - Buka panel **Equalizer & Effects** langsung dari Now Playing:
   - Tombol aktif/nonaktif efek secara instan (Bypass toggle).
   - Penggeser Bass Boost dengan persentase kekuatan (0% - 100%).
   - Penggeser Dolby 3D Virtualizer / Spatializer (surround sound effect).
   - 10-Band Graphic Equalizer dengan slider desibel halus dan preset instan.
   - Reverb effect selector: Off, Small Room, Medium Hall, Large Hall, Plate.

### Visual Identity & Theme

- **Aesthetic Direction:** Cyber-Studio Luxury Dark Theme. Latar belakang obsidian pekat (`#0B0E14`) dipadukan dengan aksen neon cyan elektrik (`#00F2FE`), magenta ultraviolet (`#4FACFE`), dan emas amber (`#FFB800`).
- **Surface & Material:** Kartu semi-transparan bergradasi kaca gelap (*glassmorphism* lembut dengan `tonalElevation` dan border halus), memberikan kedalaman visual saat visualizer musik bergerak di balik elemen.
- **Typography:**
  - Headings & Judul Lagu: Bold sans modern berdaya baca tinggi dengan pelacakan huruf rapat.
  - Controls, EQ Frequencies & Metadata: Monospace / Clean Geometric Sans untuk pembacaan akurat nilai desibel dan frekuensi Hertz (31Hz, 62Hz, 125Hz, ..., 16kHz).
- **Banner Iklan Non-Intrusif:** Diletakkan secara konsisten di bagian bawah layar dengan padding safe area `WindowInsets.navigationBars`, memastikan navigasi M3 bar dan tombol mini-player tetap leluasa ditekan tanpa risiko salah pencet (*accidental clicks*).

---

## 3. Key Product Decisions & Trade-Offs

- **Audio Engine (Jetpack Media3 / ExoPlayer & Android AudioFX):**
  - *Keputusan:* Menggunakan engine pemutaran audio modern berbasis Media3 ExoPlayer dipadukan dengan rangkaian Android AudioFX (`Equalizer`, `BassBoost`, `Virtualizer`, `PresetReverb`).
  - *Alasan:* Memberikan stabilitas pemutaran offline dan streaming online berlatar belakang (background playback), audio focus handling otomatis saat ada panggilan masuk, serta akses ke audio session ID untuk visualizer realtime.
- **Visualizer Engine Responsif:**
  - *Keputusan:* Menggunakan canvas Jetpack Compose berkinerja tinggi yang disinkronkan dengan pemutar audio dan FFT/amplitude data simulator/extractor, mendukung 60 FPS halus tanpa lag pada antarmuka.
- **AI Playlist Engine (Gemini 3.8 Flash):**
  - *Keputusan:* Menggunakan Gemini 3.8 Flash dengan JSON Schema terstruktur untuk memproses mood, waktu, dan genre, menghasilkan daftar putar yang mengintegrasikan lagu artis Wins secara relevan.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│                                                                        │
│ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐ │
│ │  Home / Explore  │ │  Now Playing &   │ │  Audio Studio / DSP      │ │
│ │  & Wins Library  │ │  Lyrics Screen   │ │  (10-Band EQ & Dolby 3D) │ │
│ └────────┬─────────┘ └────────┬─────────┘ └────────────┬─────────────┘ │
│          │                    │                        │               │
│          ▼                    ▼                        ▼               │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │               MusicPlayerViewModel (MVI / StateFlow)               │ │
│ └───────┬──────────────────────┬───────────────────────┬─────────────┘ │
└─────────┼──────────────────────┼───────────────────────┼───────────────┘
          │                      │                       │
          ▼                      ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────────────┐
│ AudioEngine      │   │ AudioVisualizer  │   │ AI Playlist Repository   │
│ Service (Exo)    │   │ Engine (Canvas)  │   │ (Gemini 3.8 Flash)       │
│ - Equalizer      │   │ - 32-Bar Bars    │   │ - Mood Analyzer          │
│ - BassBoost      │   │ - Neon Wave      │   │ - Wins Artist Integrator │
│ - Virtualizer 3D │   │ - Pulse Circle   │   │ - Smart Playlist Sync    │
└──────────────────┘   └──────────────────┘   └──────────────────────────┘
```

### Data Model & State

- `Track`: ID, judul, artis, nama album, durasi, URL audio/URI lokal, cover art URL/drawable, genre, lirik ber-timestamp (LRC).
- `EqualizerBand`: ID pita, frekuensi tengah (Hz), nilai desibel (-10dB s/d +10dB).
- `DspSettings`: Bass Boost level (0-1000), Virtualizer strength (0-1000), Reverb preset, EQ preset aktif, equalizer bands list.
- `LyricsLine`: Timestamp milidetik, teks lirik, status baris aktif.
- `AiPlaylistRequest`: Mood pengguna, genre favorit, intensitas tempo, riwayat artis.

---

## Tahapan Eksekusi Setelah Disetujui

1. **Konfigurasi Dependensi & Metadata:** Memperbarui `metadata.json` dan menambahkan library yang dibutuhkan (`Media3 ExoPlayer`, `Coil` untuk cover album).
2. **Audio Engine & DSP Processor:** Implementasi manajemen sesi audio, kontrol 10-Band EQ, Bass Booster, dan Dolby 3D Virtualizer.
3. **Katalog Musik & Diskografi Wins:** Menyediakan data streaming trek unggulan artis Wins dan pemindai lagu lokal/offline.
4. **Visualizer Komposisi Realtime:** Modul kanvas kustom dengan mode Spektrum Batang, Gelombang Neon, dan Circular Bass Pulse.
5. **Lirik Tersinkronisasi (LRC Parser & Karaoke Highlight):** Komponen teks gulir otomatis dengan sorotan lirik per baris.
6. **Gemini AI Smart Playlist Generator:** Integrasi rekomendasi otomatis berbasis mood dan genre dengan integrasi katalog Wins.
7. **Ad Banner & Interstitial Simulator:** Implementasi iklan selamat datang elegan dan bottom banner yang aman dari sentuhan navigasi.
8. **Verifikasi Kompilasi & Pengujian:** Memastikan seluruh kode lulus `compile_applet` tanpa eror.
