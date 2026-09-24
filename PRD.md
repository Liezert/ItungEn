# ItungEn: Product Requirements Document (PRD)

| | |
|---|---|
| **Status** | Draft v0.1, menunggu review |
| **Tanggal** | 2026-09-24 |
| **Platform** | Android (Kotlin + Jetpack Compose), minSdk 33 |
| **Perangkat target** | Realme 13+ 5G |
| **Dokumen terkait** | [`DESIGN_SPEC.md`](DESIGN_SPEC.md) (arsitektur, data, UI, pengujian) |

---

## 1. Ringkasan

ItungEn adalah aplikasi Android untuk mencatat pengeluaran harian dan mengelola tabungan. Bedanya dengan pencatat pengeluaran biasa ada dua hal:

1. **Rekomendasi jatah belanja harian dan mingguan**, dihitung otomatis dari pemasukan, tagihan tetap, target tabungan, dan setoran ke target barang.
2. **Antarmuka bergaya Liquid Glass** (terinspirasi iOS) dengan floating navbar yang bisa ditahan dan digeser, serta tombol liquid yang responsif.

Aplikasi bekerja sepenuhnya **offline** (tanpa akun dan tanpa cloud). Data hanya ada di perangkat, dengan backup manual ke file.

## 2. Masalah dan tujuan

**Masalah.** Mencatat pengeluaran saja tidak membantu menghemat. Pengguna baru sadar sudah boros setelah uangnya habis, dan tidak tahu berapa yang aman dibelanjakan hari ini.

**Tujuan produk**

| # | Tujuan | Ukuran keberhasilan |
|---|---|---|
| G1 | Pencatatan cepat | Mencatat satu transaksi selesai dalam ≤ 10 detik dan ≤ 4 ketukan |
| G2 | Membantu menghemat | Jatah hari ini dan minggu ini selalu terlihat di Beranda dan koreksinya otomatis saat pengeluaran berubah |
| G3 | Menabung untuk barang tertentu | Saldo bisa dialokasikan ke target (goal) lewat "Tambah Dana" tanpa dihitung sebagai pengeluaran |
| G4 | Pengalaman visual yang menonjol | Navbar dan tombol liquid berjalan mulus di Realme 13+ (tanpa jank yang terasa) |
| G5 | Kode bersih dan layak portofolio | Arsitektur berlapis, komponen glass modular, logika inti teruji |

## 3. Pengguna dan konteks

- **Pengguna utama:** pembuat aplikasi sendiri, pemakaian harian di Realme 13+ 5G.
- **Pengguna sekunder:** pembaca repo GitHub (portofolio), yang menilai kerapian arsitektur, dokumentasi, dan kualitas UI.
- **Bahasa antarmuka:** Indonesia. **Mata uang:** Rupiah (IDR), bilangan bulat.

## 4. Ruang lingkup

### 4.1 Masuk MVP (P0)

| Fitur | Ringkasan |
|---|---|
| Catat transaksi | Pengeluaran dan pemasukan dengan nominal, kategori, nama (opsional), tanggal, dan dompet |
| Saldo | Total Saldo (dompet utama) dihitung dari transaksi |
| Gaji otomatis | Aturan berulang bulanan yang mencatat gaji sendiri, tahan duplikat dan tahan terlewat |
| Rekomendasi budget | Jatah hari ini dan minggu ini, sisa jatah, proyeksi tabungan, dan status (HEMAT / ON_TRACK / MELEBIHI) |
| Target (Goals) | Buat target barang, "Tambah Dana" dari dompet utama, progres, dan nominal bulanan yang direncanakan (manual, dengan nilai saran) |
| Riwayat | Daftar semua transaksi dengan pencarian dan filter kategori |
| Insight | Ringkasan pengeluaran per kategori pada siklus berjalan |
| Backup | Ekspor/impor JSON (pulihkan data) dan ekspor CSV (untuk spreadsheet) |
| Antarmuka Liquid Glass (gelap) | Floating navbar hold-and-drag, tombol "+" liquid, dua tingkat glass |

### 4.2 Setelah MVP (P1)

Kategori kustom, ubah profil lokal, tema terang, pengingat/notifikasi.

### 4.3 Di luar cakupan (sengaja)

- Akun pengguna, sinkronisasi cloud, dan server
- Multi-mata-uang, iOS, dan versi web
- Prediksi berbasis AI atau machine learning
- Anggaran per kategori, tag, dan lampiran struk
- Pengulangan selain bulanan

## 5. Kebutuhan fungsional utama

**F1. Pencatatan.** Nominal wajib > 0 dan berupa bilangan bulat rupiah. Kategori wajib untuk pemasukan dan pengeluaran. Tanggal tidak boleh di masa depan.
**F2. Gaji otomatis.** Aturan gaji mencatat transaksi pemasukan pada tanggal gajian. Jika app tidak dibuka atau proses latar belakang tertunda, transaksi yang terlewat dicatat saat app berikutnya dibuka, tanpa duplikat.
**F3. Rekomendasi.** Jatah dihitung ulang (rolling) dan dibekukan di awal hari, sehingga angka jatah hari ini tidak menyusut sepanjang hari; yang berubah hanya sisa jatahnya. Rumus lengkap ada di `DESIGN_SPEC.md` §5.
**F4. Goals.** "Tambah Dana" memindahkan saldo dari dompet utama ke dompet goal sebagai transfer (tidak dihitung pengeluaran). Nominal tidak boleh melebihi saldo dompet utama.
**F5. Backup.** Pemulihan dari file JSON menggantikan seluruh data setelah konfirmasi, dalam satu transaksi database (gagal berarti data lama utuh).

## 6. Kebutuhan non-fungsional

| Area | Kebutuhan |
|---|---|
| **Performa** | Gestur navbar dan tombol liquid tidak menjatuhkan frame secara terasa di 120 Hz. Maksimal 3 permukaan Glass Riil tampil bersamaan |
| **Privasi** | Semua data lokal. Tidak ada jaringan yang dibutuhkan (tanpa izin INTERNET pada MVP) |
| **Keandalan** | Migrasi database tidak pernah menghapus data. Operasi multi-tabel bersifat atomik |
| **Aksesibilitas** | Kontras teks ≥ 4,5:1, target sentuh ≥ 48dp, semantik TalkBack pada navbar, animasi mengikuti pengaturan sistem |
| **Kompatibilitas** | Android 13 (API 33) ke atas |
| **Kualitas kode** | Domain berupa Kotlin murni. Logika rekomendasi dan gaji otomatis memiliki unit test |

## 7. Tahapan

| Tahap | Isi | Selesai bila |
|---|---|---|
| **1. Setup** | Android Studio, proyek Compose minSdk 33, jalankan di Realme 13+, repo GitHub | "Hello World" tampil di HP dan commit pertama terdorong |
| **2. Glass playground** | Layar uji: navbar 4 tab hold-and-drag, tombol "+" liquid, daftar berwarna di belakangnya | Halus di HP; angka spring, haptic, dan blur ditetapkan |
| **3. Lapisan data** | Room (wallets, categories, transactions), layar tambah dan lihat transaksi | Transaksi tersimpan, saldo benar, ada test DAO |
| **4. Fitur bertahap** | Gaji otomatis → Goals dan transfer → mesin rekomendasi → Insight → Backup | Semua kebutuhan P0 terpenuhi |

## 8. Risiko

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Efek glass berat di GPU kelas menengah | Frame drop | Dua tingkat glass, anggaran 3 permukaan riil, uji di HP sejak tahap 2 |
| Background task ditunda sistem hemat baterai Realme | Gaji otomatis telat | Catch-up saat app dibuka, unique index anti-duplikat |
| Library glass bersifat komunitas dan bisa berubah | Build rusak | Pin versi, dibungkus di modul `:glass` agar mudah diganti |
| Pemula di Android Studio | Kecepatan belajar | Tahap bertahap, isolasi risiko terbesar (glass) lebih awal |
| Label tab terpanjang mungkin sempit saat navbar menyusut karena tombol "+" | Teks terpotong | Verifikasi di playground; cadangan: tombol "+" 56dp atau padding tab lebih kecil |

## 9. Pertanyaan terbuka

Daftar keputusan yang belum final ada di `DESIGN_SPEC.md` §11.
