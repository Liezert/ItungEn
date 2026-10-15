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

Aplikasi bekerja **offline-first**: semua fitur jalan tanpa internet dan tanpa akun (mode tamu). Masuk dengan Google/email bersifat opsional untuk menyimpan dan menyinkronkan catatan antar-HP (Firebase), dan cadangan manual ke file tetap tersedia.

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
| Rekomendasi budget | Kartu Jatah hari ini di Beranda: sisa jatah hari ini dan minggu ini, perkiraan sampai kapan uang cukup, status (HEMAT / ON_TRACK / MELEBIHI). Butuh setup sekali: pemasukan bulanan, tanggal gajian, tagihan tetap, target tabungan |
| Target (Goals) | Buat target barang, "Tambah Dana" dari dompet utama, progres, dan nominal bulanan yang direncanakan (manual, dengan nilai saran) |
| Riwayat | Daftar semua transaksi dengan pencarian dan filter kategori; geser untuk hapus, bisa diurungkan |
| Insight | Diagram lingkaran pengeluaran per kategori, pilihan bulan, perbandingan dengan bulan lalu |
| Backup | Ekspor/impor JSON (pulihkan data) dan ekspor CSV (untuk spreadsheet) |
| Antarmuka Liquid Glass | Tema gelap dan terang, floating navbar hold-and-drag, tombol "+" liquid, keypad kalkulator |
| Catat cepat | Template dari transaksi yang sering diulang (sekali ketuk), pintasan ikon "Catat transaksi", pengingat malam bila hari itu belum ada catatan |
| Akun & sinkron (opsional) | Masuk Google/email; sinkron otomatis; status dan penyebab gagal terlihat di Pengaturan |

### 4.2 Setelah MVP (P1)

Kategori kustom, ubah profil lokal, sinkron rencana anggaran antar-HP, pilihan jam pengingat, FAB "+" sembunyi otomatis saat daftar discroll.

### 4.3 Di luar cakupan (sengaja)

- Server sendiri (sinkron memakai Firebase)
- Gaji/transaksi berulang otomatis (dihapus: tanggal gajian nyata sering bergeser)
- Multi-mata-uang, iOS, dan versi web
- Prediksi berbasis AI atau machine learning
- Anggaran per kategori, tag, dan lampiran struk

## 5. Kebutuhan fungsional utama

**F1. Pencatatan.** Nominal wajib > 0 dan berupa bilangan bulat rupiah. Kategori wajib untuk pemasukan dan pengeluaran. Tanggal tidak boleh di masa depan.
**F2. Gaji otomatis.** Dihapus. Pemasukan bulanan diisi sekali di Anggaran sebagai perkiraan; pemasukan yang tercatat dipakai bila lebih besar.
**F3. Rekomendasi.** Jatah dihitung ulang (rolling) dan dibekukan di awal hari, sehingga angka jatah hari ini tidak menyusut sepanjang hari; yang berubah hanya sisa jatahnya. Rumus lengkap ada di `DESIGN_SPEC.md` §5.
**F4. Goals.** "Tambah Dana" memindahkan saldo dari dompet utama ke dompet goal sebagai transfer (tidak dihitung pengeluaran). Nominal tidak boleh melebihi saldo dompet utama.
**F5. Backup.** Pemulihan dari file JSON menambahkan catatan yang belum ada (dicocokkan lewat uuid, tidak menggandakan), dalam satu transaksi database (gagal berarti data lama utuh).

## 6. Kebutuhan non-fungsional

| Area | Kebutuhan |
|---|---|
| **Performa** | Gestur navbar dan tombol liquid tidak menjatuhkan frame secara terasa di 120 Hz. Maksimal 3 permukaan Glass Riil tampil bersamaan |
| **Privasi** | Semua data lokal. Tidak ada jaringan yang dibutuhkan (tanpa izin INTERNET pada MVP) |
| **Keandalan** | Migrasi database tidak pernah menghapus data. Operasi multi-tabel bersifat atomik |
| **Aksesibilitas** | Kontras teks ≥ 4,5:1, target sentuh ≥ 48dp, semantik TalkBack pada navbar, animasi mengikuti pengaturan sistem |
| **Kompatibilitas** | Android 13 (API 33) ke atas |
| **Kualitas kode** | Domain berupa Kotlin murni. Logika rekomendasi, keypad, dan pemetaan sinkron memiliki unit test |

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

## 9. Pertanyaan terbuka

Daftar keputusan yang belum final ada di `DESIGN_SPEC.md` §11.
