# AGENTS.md

Android native app (belum diimplementasikan) untuk memindai screenshot resource game strategi mobile (Food/Wood/Stone/Gold), mendeteksi angka via OCR, menjumlahkan totalnya, dan menempelkan hasil ke gambar. Proyek ini bermula dari prototipe CLI Python (`resource_ocr.py`, Tesseract-based) yang sudah berfungsi dan menjadi acuan logika.

**Baca `prd.md` dulu sebelum mengerjakan apa pun** — dokumen itu adalah sumber kebenaran untuk scope, fitur, dan batasan produk.

## Status proyek

- **Ada:** `prd.md` (requirement lengkap), `resource_ocr.py` (prototipe CLI Python/Tesseract, jalan di Termux — bukan bagian dari app Android, hanya referensi logika)
- **Belum ada:** kode Android sama sekali. Tidak ada `build.gradle`, `AndroidManifest.xml`, atau struktur modul apa pun. Agen yang pertama menyentuh proyek ini kemungkinan akan membuat skeleton project dari nol.
- Jangan asumsikan struktur folder Android standar sudah ada — cek dulu dengan `ls`/`find` sebelum menulis path.

## Quick start (prototipe Python, referensi logika saja)

```bash
pip install pytesseract pillow --break-system-packages
python3 resource_ocr.py <path_gambar>              # proses satu gambar
python3 resource_ocr.py --watch <path_folder>       # pantau folder otomatis
```

Prototipe ini **tidak digunakan langsung di app Android** — OCR engine-nya (Tesseract CLI) tidak cocok untuk Android native. Lihat bagian "Keputusan teknis" di bawah.

## Environment kerja

Proyek ini dikerjakan langsung di HP menggunakan **Xed Editor**, bukan Android Studio/komputer. Belum ada toolchain build (Gradle dsb) disiapkan — fokus saat ini murni menulis kode. Implikasinya untuk agen:

- Jangan asumsikan ada Android Studio, Gradle, atau emulator yang bisa dipakai untuk verifikasi build. Kalau perlu memverifikasi kode, cek secara statis/baca ulang — jangan sarankan "coba jalankan Gradle build" sebagai langkah sebelum toolchain-nya memang sudah disiapkan.
- Path proyek kemungkinan besar ada di `/storage/emulated/0/...` (shared storage Android), bukan di filesystem internal biasa.

## FUSE filesystem quirk (Android `/sdcard/`)

Repo ini kemungkinan berada di storage HP (`/sdcard/` atau `/storage/emulated/0/...`), yang menggunakan **FUSE** — bukan filesystem Linux native. FUSE tidak mendukung **atomic rename**, sesuatu yang dibutuhkan Git untuk menulis object/commit dengan aman. Efeknya: perintah `git add`/`git commit` langsung di lokasi ini bisa gagal aneh atau merusak `.git/`.

**Aturan untuk agen:**
- Untuk operasi **tulis** (add, commit, merge, rebase), jangan jalankan `git` langsung di path FUSE. Gunakan salah satu pendekatan berikut, sesuaikan dengan tool yang tersedia di environment:
  - Salin/symlink repo ke filesystem internal (mis. home directory Termux `~/project`) sebelum melakukan operasi tulis Git, lalu sinkronkan hasilnya kembali ke `/sdcard/`
  - Atau, kalau tersedia wrapper khusus (seperti `gitc` pada proyek referensi), gunakan itu
- Untuk operasi **baca** (`status`, `log`, `diff`, `show`), `git` biasa umumnya aman dipakai langsung di path FUSE
- Jika menemukan `.git/` sudah dalam kondisi rusak (index corrupt, object hilang), curigai dulu penyebabnya rename non-atomic di FUSE sebelum coba solusi lain
- Belum ada wrapper script (`gitc` atau semacamnya) disiapkan di proyek ini — kalau operasi Git langsung bermasalah, tanyakan ke user dulu sebelum membuat/menerapkan workaround, karena ini menyentuh infrastruktur bukan cuma kode fitur.

## Scope produk (ringkas dari prd.md)

- **Khusus** 4 resource: Food, Wood, Stone, Gold — 2 kolom (From Items, Total Resources)
- Format angka pakai suffix K/M/B (`19.3M`, `764,942`), hasil ditampilkan tetap dalam format ringkas, **bukan** angka penuh berderet nol
- Di luar scope v1.0: game lain, resource lain (Gems/Speedups), akun cloud, fitur sosial, iOS
- Jangan menambah scope (misal dukungan multi-game) tanpa konfirmasi — itu ada di roadmap v2, bukan v1

## Keputusan teknis (wajib diikuti kecuali ada instruksi lain)

- **OCR engine:** gunakan ML Kit Text Recognition (on-device, offline) — bukan Tesseract CLI seperti prototipe. Alasan: native Android, tidak butuh binary eksternal, lebih ringan.
- **Parsing angka K/M/B:** port logika regex dari `resource_ocr.py` (`find_values`, `convert_value`, `format_compact`) — pola ini sudah teruji, jangan didesain ulang dari nol.
- **Rendering panel hasil ke gambar:** setara fungsi `append_result_panel` di prototipe — kanvas baru = gambar asli + panel di bawah berisi judul + 2 baris total, rata tengah. Gunakan Canvas API Android.
- **Penyimpanan riwayat:** Room/SQLite, lokal saja. Tidak ada backend/API eksternal di v1.0.
- **Privasi:** semua proses (OCR, hitung, simpan) harus on-device. Jangan menambahkan network call untuk memproses gambar pengguna.
- **Target platform:** Android 8.0 (API 26) ke atas.

## What NOT to do

- Jangan menulis ulang logika parsing K/M/B dari nol — port dari `resource_ocr.py` sebagai referensi behavior yang benar
- Jangan menambah fitur di luar `prd.md` §4.2 (Out of Scope) tanpa konfirmasi eksplisit ke user
- Jangan menambahkan dependency cloud/network untuk fitur inti (OCR, hitung, simpan) — ini harus offline-first
- Jangan menghapus atau mengganti isi `prd.md` saat mengerjakan kode — dokumen requirement dan kode adalah artefak terpisah
- Jangan membuat asumsi struktur project Android tanpa mengecek dulu apakah skeleton sudah ada

## Testing

Belum ada test framework. Saat membuat modul parsing (K/M/B, deteksi tabel resource), tulis unit test dari awal (mis. JUnit) menggunakan sample teks OCR yang sudah diketahui hasilnya benar — ambil contoh dari output nyata prototipe di `resource_ocr.py` sebagai fixture:
```
Food  19.3M  19.5M
Wood  20.4M  21.9M
Stone 5.8M   6.1M
Gold  1.1M   3.0M
```
Expected: Total From Items = 46.6M, Total Resources = 50.5M.

## Versioning

Belum ada skema versi ditetapkan. Saat app Android pertama kali dibuat, mulai dari `1.0.0` dan catat perubahan di `CHANGELOG.md` (format: `## [MAJOR.MINOR.PATCH] - YYYY-MM-DD`).
