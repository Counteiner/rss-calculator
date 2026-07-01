# PRD: Resource Calculator - Aplikasi Deteksi & Penghitung Resource Game

**Versi:** 1.0
**Tanggal:** 1 Juli 2026
**Status:** Draft
**Platform Target:** Android (Native App)

---

## 1. Latar Belakang

Pemain game strategi mobile (contoh: game bertema kerajaan/peradaban dengan resource Food, Wood, Stone, Gold) sering perlu mengecek total resource yang dimiliki lewat popup "Your Resources & Speedups". Popup ini menampilkan dua kolom angka (**From Items** dan **Total Resources**) dalam format singkat (K/M/B) untuk tiap jenis resource.

Saat ini, pemain harus menjumlahkan angka-angka tersebut secara manual satu per satu, yang:
- Memakan waktu dan rawan salah hitung, terutama dengan format K/M/B campuran
- Tidak ada cara cepat untuk menyimpan atau membandingkan hasil hitungan dari waktu ke waktu
- Tidak praktis dilakukan berulang kali (misal untuk tracking progress harian/mingguan)

Sudah ada proof-of-concept berupa script Python (OCR berbasis Tesseract) yang berhasil membaca screenshot dan menghitung total resource secara otomatis. Prototipe ini perlu dikembangkan menjadi aplikasi Android native yang mudah dipakai tanpa command line.

## 2. Tujuan Produk

Membuat aplikasi Android yang memungkinkan pemain **memindai screenshot resource dalam game**, lalu secara otomatis **mendeteksi angka dan menghitung totalnya**, kemudian menampilkan hasilnya dengan jelas — tanpa perlu hitung manual atau alat command line.

### Tujuan Spesifik
- Mengurangi waktu pengecekan total resource dari hitung manual (~1-2 menit) menjadi instan (<5 detik)
- Menghindari human error dalam penjumlahan format K/M/B
- Menyediakan riwayat hasil pindai agar pemain bisa melihat perkembangan resource dari waktu ke waktu

## 3. Target Pengguna

- Pemain game strategi mobile bergenre "kingdom builder" (resource: Food, Wood, Stone, Gold) yang aktif mengelola banyak resource
- Pemain yang tergabung dalam aliansi/guild dan perlu melaporkan jumlah resource secara berkala
- Tidak memerlukan keahlian teknis — pengguna awam harus bisa memakainya tanpa instruksi rumit

## 4. Cakupan Produk (Scope)

### 4.1 Dalam Cakupan (In Scope)
Aplikasi ini **khusus** dirancang untuk mendeteksi tabel resource dengan format berikut:
- 4 jenis resource: **Food, Wood, Stone, Gold**
- 2 kolom angka per resource: **From Items** dan **Total Resources**
- Format angka: angka desimal + suffix K/M/B (contoh: `19.3M`, `764,942`)

### 4.2 Di Luar Cakupan (Out of Scope untuk v1.0)
- Deteksi resource dari game lain dengan tabel/layout berbeda
- Deteksi jenis resource selain Food/Wood/Stone/Gold (misal Gems, Speedups, dll — bisa jadi roadmap v2)
- Sinkronisasi cloud / akun pengguna
- Fitur sosial (share ke leaderboard publik, dsb) — hanya share gambar biasa
- Dukungan iOS (fokus Android dulu)

## 5. Fitur Utama

### 5.1 Ambil Gambar (Input)
- **Pilih dari galeri**: pengguna memilih screenshot yang sudah ada
- **Ambil screenshot langsung / kamera** (opsional v1.1): integrasi dengan tombol screenshot Android atau kamera untuk foto layar
- Preview gambar sebelum diproses

### 5.2 Deteksi Otomatis (OCR)
- Mendeteksi teks dan angka pada gambar menggunakan OCR on-device
- Mengenali baris tabel resource (Food, Wood, Stone, Gold) beserta dua kolom angkanya
- Mengenali dan mengonversi format singkat K/M/B (bukan hanya angka penuh)
- Menangani variasi hasil bacaan OCR yang kurang sempurna (misal salah baca "Gold" jadi "Cold") dengan tetap mengekstrak angka dengan benar

### 5.3 Perhitungan Total
- Menjumlahkan otomatis kolom **From Items** (total dari 4 resource)
- Menjumlahkan otomatis kolom **Total Resources** (total dari 4 resource)
- Menampilkan hasil dalam format ringkas (K/M/B), bukan angka panjang berderet nol

### 5.4 Tampilan Hasil
- Menampilkan hasil pindai dalam bentuk kartu ringkas: rincian per resource + total keseluruhan
- **Generate gambar hasil**: menempelkan panel ringkasan total (judul + 2 baris total) di bagian bawah gambar asli, rata tengah, sehingga bisa langsung disimpan/dibagikan sebagai satu gambar utuh
- Tombol simpan gambar hasil ke galeri
- Tombol bagikan (share) langsung ke aplikasi lain (WhatsApp, Telegram, dll) — umum dipakai untuk lapor ke aliansi/guild

### 5.5 Riwayat Pindai (History)
- Menyimpan daftar hasil pindai sebelumnya (tanggal, total From Items, total Total Resources)
- Pengguna bisa membuka kembali gambar hasil dari riwayat
- Opsi hapus riwayat (per item atau semua)

### 5.6 Penanganan Kegagalan Deteksi
- Jika OCR gagal mengenali tabel resource dengan format yang diharapkan, aplikasi menampilkan pesan yang jelas (bukan crash atau hasil kosong tanpa penjelasan)
- Menyarankan pengguna mengambil ulang screenshot dengan kualitas/pencahayaan lebih baik jika diperlukan

## 6. Alur Pengguna (User Flow)

1. Pengguna membuka aplikasi
2. Pengguna menekan tombol "Pindai Gambar"
3. Pengguna memilih screenshot dari galeri (atau ambil baru)
4. Aplikasi menampilkan preview gambar + indikator loading saat memproses
5. Aplikasi menampilkan hasil: tabel rincian per resource + total keseluruhan
6. Pengguna dapat:
   - Menyimpan gambar hasil (dengan panel total) ke galeri
   - Membagikan hasil ke aplikasi lain
   - Melihat kembali riwayat pindai sebelumnya

## 7. Kebutuhan Non-Fungsional

| Aspek | Kebutuhan |
|---|---|
| **Performa** | Proses OCR + hitung selesai dalam maksimal 5 detik untuk gambar resolusi standar HP (≤2400px) |
| **Offline** | Seluruh proses OCR & perhitungan berjalan on-device, tidak butuh koneksi internet |
| **Privasi** | Gambar yang dipindai tidak diunggah ke server manapun; semua data disimpan lokal di HP |
| **Kompatibilitas** | Android 8.0 (API 26) ke atas |
| **Ukuran aplikasi** | Target di bawah 50MB (termasuk model OCR) |
| **Akurasi OCR** | Target akurasi pembacaan angka minimal 95% pada screenshot resolusi normal tanpa blur |

## 8. Pertimbangan Teknis

- **Mesin OCR**: menggunakan library OCR on-device Android (misal Google ML Kit Text Recognition) alih-alih Tesseract CLI, karena lebih ringan dan native untuk Android, tidak butuh instalasi binary terpisah
- **Parsing hasil OCR**: logika regex untuk mendeteksi pola angka + suffix K/M/B (mengikuti pendekatan yang sudah terbukti pada prototipe Python)
- **Rendering gambar hasil**: menggambar panel teks di atas kanvas gambar (setara fungsi `append_result_panel` pada prototipe), diimplementasikan dengan Canvas API Android
- **Penyimpanan riwayat**: database lokal ringan (Room/SQLite) untuk menyimpan metadata hasil pindai

## 9. Metrik Keberhasilan

- Waktu rata-rata dari buka aplikasi sampai dapat hasil total: **< 10 detik**
- Tingkat keberhasilan deteksi tabel resource pada screenshot standar: **≥ 95%**
- Tidak ada crash pada alur utama (pindai → hasil → simpan/bagikan)

## 10. Roadmap Selanjutnya (Di Luar v1.0)

- Dukungan jenis resource tambahan (Gems, Speedups, dll)
- Dukungan multi-game (bisa pilih "profil game" dengan layout tabel berbeda)
- Perbandingan otomatis antara dua hasil pindai (misal selisih resource harian)
- Backup/restore riwayat via cloud (opsional, dengan persetujuan eksplisit pengguna)
- Dukungan iOS

## 11. Lampiran

**Referensi prototipe:** Script Python (`resource_ocr.py`) yang telah diuji menggunakan Tesseract OCR, berhasil mendeteksi dan menghitung total resource dari screenshot game dengan hasil:
- Total From Items: 46.6M
- Total Resources: 50.5M

Prototipe ini menjadi acuan logika parsing dan format output untuk pengembangan aplikasi native.
