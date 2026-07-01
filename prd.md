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
Aplikasi ini mendukung deteksi dua format tampilan resource yang umum ditemui di game:

**Format A — Tabel "Your Resources & Speedups"**
- 4 jenis resource: **Food, Wood, Stone, Gold**
- 2 kolom angka per resource: **From Items** dan **Total Resources**
- Format angka: angka desimal + suffix K/M/B (contoh: `19.3M`, `764,942`)

**Format B — Grid Shop/Marketplace (tab RESOURCES)**
- Grid berukuran **4 baris × 4 kolom** (16 kotak total):
  - Setiap **baris** mewakili satu jenis resource, urutan dari atas: **Food, Wood, Stone, Gold**
  - Setiap **kolom** mewakili varian/paket dengan nominal berbeda untuk jenis resource yang sama, terurut dari nominal terkecil (kolom 1, kiri) ke nominal terbesar (kolom 4, kanan)
- Setiap kotak berisi **2 angka**:
  - **Angka nominal paket** (ditampilkan besar, di tengah kotak) — contoh: `10,000`, `50,000`, `150,000`, `500,000`
  - **Badge jumlah dimiliki** (ditampilkan kecil, di pojok kanan-bawah kotak) — contoh: `1,174`, `118`, `6`, `3`
- Setiap kotak **selalu memiliki badge jumlah dimiliki**; badge ini wajib dideteksi terpisah dari angka nominal karena posisi dan ukurannya berbeda (font lebih kecil, area lebih sempit)
- Aplikasi menjumlahkan **total nilai resource** per jenis (nilai paket × jumlah dimiliki, dijumlahkan untuk seluruh 4 kotak dalam satu baris/jenis), lalu menampilkan total keseluruhan dari 4 jenis resource
- **Panel detail item di sisi kanan** (contoh: "Lvl 2 Resource Pack" beserta deskripsi, kontrol jumlah, tombol "USE") **tidak termasuk dalam cakupan deteksi** — aplikasi hanya memproses area grid resource, panel kanan diabaikan sepenuhnya
- Format angka: angka dengan pemisah ribuan (contoh: `10,000`, `2,796`) maupun singkat K/M/B bila muncul

Aplikasi mendeteksi otomatis format mana yang sesuai dengan screenshot yang dipindai (Format A atau Format B) sebelum melakukan parsing dan perhitungan.

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
- Mengenali secara otomatis format tampilan yang dipindai: **Format A** (tabel "Your Resources & Speedups") atau **Format B** (grid shop/marketplace tab RESOURCES)
- **Format A**: mengenali baris tabel resource (Food, Wood, Stone, Gold) beserta dua kolom angkanya (From Items, Total Resources)
- **Format B**: mengenali baris grid per jenis resource (Food, Wood, Stone, Gold), mengekstrak pasangan angka nilai-paket dan jumlah-dimiliki dari tiap kotak, serta membatasi area deteksi hanya pada grid kiri — mengabaikan panel detail item di sisi kanan layar
- Mengenali dan mengonversi format singkat K/M/B maupun angka dengan pemisah ribuan (bukan hanya satu jenis format angka)
- Menangani variasi hasil bacaan OCR yang kurang sempurna (misal salah baca "Gold" jadi "Cold") dengan tetap mengekstrak angka dengan benar

### 5.3 Perhitungan Total
**Format A:**
- Menjumlahkan otomatis kolom **From Items** (total dari 4 resource)
- Menjumlahkan otomatis kolom **Total Resources** (total dari 4 resource)

**Format B:**
- Menghitung total nilai per jenis resource (nilai paket × jumlah dimiliki, dijumlahkan seluruh kotak dalam satu baris)
- Menjumlahkan total keseluruhan dari 4 jenis resource (Food, Wood, Stone, Gold)

Kedua format menampilkan hasil dalam format ringkas (K/M/B), bukan angka panjang berderet nol.

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

### 8.1 Mesin OCR

**Strategi utama: Google ML Kit Text Recognition v2**
- Dipilih sebagai mesin OCR utama untuk v1.0 karena:
  - Akurasi tinggi (>95%) untuk teks digital UI dengan kontras jelas seperti angka resource game
  - On-device, gratis, tidak butuh koneksi internet — sesuai requirement privasi & offline
  - Ringan (~15-30MB tambahan), sesuai target ukuran app <50MB
  - Integrasi native Android (Kotlin/Java) tanpa perlu convert model, mempercepat waktu pengembangan
  - Didukung dan dimaintain langsung oleh Google dengan update rutin

**Preprocessing gambar sebelum OCR**
Untuk menaikkan akurasi, terutama pada teks kecil seperti badge angka "dimiliki" di sudut kotak grid (Format B), gambar diproses terlebih dahulu sebelum dikirim ke ML Kit:
- **Upscale** gambar 2x pada area yang akan di-OCR (khususnya badge angka kecil)
- **Peningkatan kontras/sharpening** untuk mempertegas batas karakter
- **Crop area** sesuai format yang terdeteksi (Format A: seluruh tabel; Format B: hanya grid kiri, mengecualikan panel detail item di kanan)

**Validasi pasca-OCR**
- Regex ketat untuk memvalidasi pola angka + suffix K/M/B atau pemisah ribuan (mengikuti pendekatan yang sudah terbukti pada prototipe Python)
- Hasil yang tidak cocok pola diperlakukan sebagai kegagalan deteksi (lihat section 5.6), bukan dipaksakan jadi angka yang salah

**Fallback plan: PaddleOCR (mobile/lite)**
- ML Kit dipilih sebagai solusi utama karena kemudahan integrasi dan kecukupan akurasi untuk kasus umum. Namun **PaddleOCR** (via PaddleLite/NCNN) dicatat sebagai kandidat fallback bila hasil testing lapangan menunjukkan ML Kit sering gagal pada kasus tertentu — khususnya teks kecil dan padat seperti badge angka di Format B, di mana algoritma deteksi teks PaddleOCR (DB algorithm) secara umum lebih unggul
- Keputusan migrasi ke PaddleOCR **tidak diambil di awal v1.0** karena effort integrasinya jauh lebih besar (perlu convert model ke format mobile, tidak ada library resmi plug-and-play seperti ML Kit) — dievaluasi hanya jika target akurasi 95% (section 7) tidak tercapai dengan ML Kit + preprocessing setelah testing pada sampel screenshot nyata
- Jika fallback diaktifkan, PaddleOCR diusulkan dipakai secara selektif hanya untuk area badge angka kecil di Format B, sementara ML Kit tetap dipakai untuk area teks lain — untuk menjaga ukuran app dan performa

### 8.2 Parsing & Rendering

- **Parsing hasil OCR**: logika regex untuk mendeteksi pola angka + suffix K/M/B (mengikuti pendekatan yang sudah terbukti pada prototipe Python)
- **Deteksi format & area crop untuk Format B**: sebelum OCR dijalankan, aplikasi mengidentifikasi batas area grid resource (kolom kiri) dan mengecualikan panel detail item di kolom kanan (judul item, deskripsi, kontrol jumlah, tombol "USE") dari area yang diproses, agar tidak ikut terbaca sebagai data resource
- **Pengelompokan hasil OCR ke grid (Format B)**: karena ML Kit mengembalikan teks per blok berdasarkan posisi (bukan per kotak grid), diperlukan logika tambahan untuk memetakan bounding box hasil OCR ke struktur grid 4 baris (jenis resource) × 4 kolom (varian nominal), lalu memasangkan setiap angka nominal paket dengan badge jumlah dimiliki pada kotak yang sama berdasarkan kedekatan posisi (angka nominal di tengah kotak, badge di pojok kanan-bawah kotak yang sama)
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
