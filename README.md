# RCalc — Resource Scanner Calculator

Aplikasi Android untuk memindai screenshot resource game strategi mobile, mendeteksi angka via OCR, dan menghitung total resource otomatis.

## Fitur

- **Scan dari galeri** — pilih screenshot yang sudah ada
- **Deteksi 4 resource** — Food, Wood, Stone, Gold
- **2 kolom** — From Items & Total Resources, format K/M/B
- **OCR on-device** — ML Kit Text Recognition, offline, tanpa internet
- **Hasil instan** — tampil dalam format ringkas + gambar hasil dengan panel total
- **Riwayat** — simpan & lihat riwayat scan (Room/SQLite)
- **Bagikan** — simpan ke galeri atau share ke WhatsApp/Telegram

## Panduan Build

```bash
git clone https://github.com/Counteiner/rss-calculator.git
cd rss-calculator
gradle wrapper --gradle-version 8.5
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Kotlin** — bahasa utama
- **ML Kit Text Recognition** — OCR engine
- **Room** — database lokal
- **Canvas API** — rendering panel hasil
- **Gradle 8.5 / AGP 8.2.2**
- **Target Android 8.0+ (API 26)**

## Lisensi

Dirilis di bawah lisensi MIT. Lihat [LICENSE](LICENSE) untuk detail.

Hak cipta © 2026 Arya Maulana Rahmatullah.
