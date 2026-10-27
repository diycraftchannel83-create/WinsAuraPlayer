# WinsAuraPlayer

**WinsAuraPlayer** adalah aplikasi pemutar musik Android dengan antarmuka Jetpack Compose, pustaka audio lokal, kontrol pemutaran, visualizer, dan pengaturan efek audio.

## Identitas aplikasi

| Properti | Nilai |
|---|---|
| Nama aplikasi (label Android) | `WinsAuraPlayer` |
| Nama proyek Gradle | `WinsAuraPlayer` |
| Package name / Application ID | `com.winsmediatama.winsauraplayer` |
| Android namespace | `com.example` |
| Modul aplikasi | `:app` |
| Bahasa utama | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Minimum Android | Android 7.0 (API 24) |
| Target SDK | API 36 |
| Version name saat ini | `1.0` |
| Version code saat ini | `1` |

> **Penting:** Package name yang dipakai Android untuk instalasi, pembaruan, dan identitas aplikasi adalah `com.winsmediatama.winsauraplayer` (nilai `applicationId` di `app/build.gradle.kts`). Nilai `namespace` `com.example` berbeda dan digunakan untuk namespace kode serta resource. Jangan mengubah `applicationId` jika ingin pembaruan aplikasi tetap kompatibel dengan instalasi yang sudah ada.

## Fitur

- Pemutar audio lokal berbasis `MediaPlayer`.
- Pustaka musik dari penyimpanan perangkat melalui MediaStore.
- Tampilan Now Playing, kontrol play/pause, next/previous, seek, dan repeat.
- Visualizer 32 pita yang mengambil data FFT dari sesi audio aktif melalui Android `Visualizer` API. Ketersediaan capture dapat berbeda menurut perangkat dan implementasi audio Android.
- Audio FX berbasis efek audio Android: equalizer, Bass Boost, Virtualizer, dan preset reverb, jika didukung perangkat.
- Preset equalizer dan pengaturan DSP.
- Tampilan lirik LRC tersinkronisasi jika data lirik tersedia.
- Pembuatan playlist cerdas berdasarkan trek yang tersedia di pustaka lokal.
- Integrasi iklan Google AdMob.
- Workflow GitHub Actions untuk pengujian dan pembuatan APK/AAB release.

## Struktur file penting

| File / folder | Kegunaan |
|---|---|
| `settings.gradle.kts` | Nama proyek Gradle dan deklarasi modul |
| `build.gradle.kts` | Konfigurasi Gradle tingkat proyek |
| `app/build.gradle.kts` | Application ID, namespace, SDK, versi, dependensi, dan signing release |
| `gradle/libs.versions.toml` | Versi plugin dan dependensi |
| `app/src/main/AndroidManifest.xml` | Permission dan deklarasi aplikasi/aktivitas |
| `app/src/main/res/values/strings.xml` | Nama aplikasi dan string UI |
| `app/src/main/java/com/example/MainActivity.kt` | Activity utama dan komposisi layar aplikasi |
| `app/src/main/java/com/example/audio/AudioPlayerManager.kt` | Pemutaran audio, DSP, progress, dan pengambilan FFT visualizer |
| `app/src/main/java/com/example/viewmodel/MusicViewModel.kt` | State UI, kontrol pemutar, pustaka, dan pemindaian media |
| `app/src/main/java/com/example/data/MusicCatalog.kt` | Pemindaian MediaStore dan penyimpanan pustaka lokal |
| `app/src/main/java/com/example/model/Track.kt` | Model data trek musik |
| `app/src/main/java/com/example/model/DspSettings.kt` | Model pengaturan DSP dan preset |
| `app/src/main/java/com/example/ui/screens/NowPlayingScreen.kt` | Layar pemutar dan informasi lagu |
| `app/src/main/java/com/example/ui/screens/LibraryScreen.kt` | Layar pustaka musik |
| `app/src/main/java/com/example/ui/screens/DspEqualizerScreen.kt` | Layar pengaturan equalizer dan efek audio |
| `app/src/main/java/com/example/ui/components/AudioVisualizerView.kt` | Rendering visualizer di Jetpack Compose |
| `app/src/test/java/com/example/ExampleUnitTest.kt` | Unit test logika aplikasi |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | Test berbasis Robolectric |
| `.github/workflows/release-android-runner-v2.yml` | Workflow pengujian, build APK/AAB, dan publikasi GitHub Release |

## Persyaratan

- JDK 17.
- Android SDK dan platform/build tools yang diperlukan oleh konfigurasi proyek.
- Gradle wrapper yang tersedia di repository.
- Koneksi internet untuk mengunduh dependensi saat pertama kali build.
- Untuk release bertanda tangan, konfigurasi keystore dan secrets GitHub yang valid.

## Build lokal

Dari root repository, jalankan:

```bash
# Jalankan unit test sebelum build
./gradlew testDebugUnitTest

# Build APK debug
./gradlew assembleDebug

# Build APK release
./gradlew assembleRelease

# Build Android App Bundle release
./gradlew bundleRelease
```

Di Windows PowerShell, gunakan `gradlew.bat` sebagai pengganti `./gradlew`.

Lokasi hasil build yang umum:
- APK debug: `app/build/outputs/apk/debug/`
- APK release: `app/build/outputs/apk/release/`
- AAB release: `app/build/outputs/bundle/release/`

Build release memerlukan konfigurasi signing yang benar. Jangan commit keystore, kata sandi, token, atau file rahasia ke repository.

## GitHub Actions dan release

Workflow release berada di:

` .github/workflows/release-android-runner-v2.yml `

Workflow tersebut menjalankan unit test sebelum build release. Jika test gagal, pembuatan APK/AAB dan publikasi release akan dihentikan. Buka tab **Actions** di GitHub untuk melihat log, status, dan file hasil workflow. Setelah workflow sukses, lihat tab **Releases** untuk aset APK/AAB yang dipublikasikan.

Pastikan secrets signing yang digunakan workflow sudah dikonfigurasi di **Settings → Secrets and variables → Actions**. Nama secrets yang dibutuhkan harus mengikuti konfigurasi workflow; jangan menaruh nilainya di README atau commit.

## Izin Android

Aplikasi meminta akses jaringan untuk fitur yang memerlukannya, izin pengaturan audio, dan izin membaca file audio perangkat. Di Android versi baru, pengguna perlu memberikan izin audio ketika diminta agar pemindaian pustaka dapat menemukan musik lokal.

## Catatan pengembangan

- Efek audio Android berbeda dukungannya antar perangkat; keberadaan kontrol di UI tidak menjamin semua efek didukung oleh hardware/ROM.
- Android `Visualizer` API dapat dibatasi atau gagal diinisialisasi pada beberapa perangkat/sesi audio. Aplikasi harus tetap dapat memutar musik jika visualizer tidak tersedia.
- Uji pemutaran, equalizer, bass boost, reverb, visualizer, izin media, dan perilaku pergantian lagu di perangkat Android fisik sebelum distribusi.
- Saat menerbitkan update ke Google Play, pertahankan `applicationId` dan signing key yang sama, serta naikkan `versionCode`.

## Repository

https://github.com/diycraftchannel83-create/WinsAuraPlayer
