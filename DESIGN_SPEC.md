# ItungEn: Design Spec

| | |
|---|---|
| **Status** | Draft v0.1, menunggu review |
| **Tanggal** | 2026-09-24 |
| **Terkait** | [`PRD.md`](PRD.md) |

Dokumen ini menjelaskan *bagaimana* ItungEn dibangun. Bagian 1–5 mengikuti urutan pembahasan desain; §11 berisi keputusan yang masih perlu dikonfirmasi.

---

## 1. Teknologi dan kebutuhan sistem

### 1.1 Pilihan teknologi

| Aspek | Keputusan | Alasan |
|---|---|---|
| Bahasa dan UI | Kotlin + Jetpack Compose | Efek glass butuh akses ke shader dan gestur tingkat rendah; native paling dekat |
| Efek glass | Library [Backdrop (Kyant0/AndroidLiquidGlass)](https://github.com/Kyant0/AndroidLiquidGlass) | Backdrop blur dan refraksi untuk Compose; tidak menyertakan komponen tingkat tinggi, jadi komponen dibuat sendiri |
| Data | Room (SQLite) + DataStore | Offline-first, tanpa server |
| Latar belakang | WorkManager | Jadwal gaji otomatis |
| Font | Inter (di-bundle) | Sesuai desain Figma |
| Alternatif yang ditolak | Flutter; Compose Multiplatform | Flutter: keunggulan lintas-platform tidak terpakai (hanya Android). CMP: berlebihan untuk satu target |

### 1.2 Kebutuhan sistem

| Kebutuhan | Nilai |
|---|---|
| Perangkat pengembangan | Windows/macOS/Linux 64-bit, RAM minimal 8 GB (16 GB nyaman), SSD dengan ±20 GB ruang. Cek angka resmi di developer.android.com |
| IDE | Android Studio versi stable terbaru (JDK, Gradle, Kotlin sudah terbundel) |
| Pengujian | Langsung di Realme 13+ 5G lewat USB atau wireless debugging (tanpa emulator) |
| SDK | minSdk 33; compileSdk dan targetSdk versi stable terbaru |
| Versi kontrol | Git + GitHub, `.gitignore` template Android; `local.properties` dan keystore tidak pernah di-commit |
| Lebar layar acuan | 390 dp (1 px Figma ≈ 1 dp) |

Refraksi berbasis shader (AGSL) membutuhkan Android 13 ke atas, sehingga minSdk 33 masuk akal untuk aplikasi pribadi.

---

## 2. Arsitektur

### 2.1 Modul

Dua modul (Opsi B):

- **`:app`**: seluruh aplikasi (UI fitur, domain, data).
- **`:glass`**: library UI mandiri (token desain, `LiquidButton`, `FloatingNavBar`, pembungkus Backdrop). Modul ini **tidak tahu apa pun soal keuangan** dan tidak pernah mengimpor dari `:app`.

Pemecahan lebih lanjut (`:core:*`, `:feature:*`) ditunda sampai benar-benar dibutuhkan.

### 2.2 Struktur folder

```
ItungEn/
├─ app/src/main/java/.../itungen/
│  ├─ ItungEnApp.kt, MainActivity.kt
│  ├─ data/
│  │  ├─ local/          (database, dao, entity, datastore)
│  │  ├─ backup/         (export/import JSON, export CSV)
│  │  └─ repository/     (implementasi repository)
│  ├─ domain/
│  │  ├─ model/          (Wallet, Goal, Transaction, ...)
│  │  ├─ repository/     (interface)
│  │  └─ usecase/        (RecommendationEngine, RecurringGenerator, TransferToGoal)
│  ├─ ui/
│  │  ├─ navigation/
│  │  ├─ home/  insight/  goals/  profile/  transactions/  addtransaction/
│  │  │            (per fitur: Screen + ViewModel + UiState)
│  │  └─ theme/
│  └─ work/              (WorkManager: gaji otomatis)
├─ glass/src/main/java/.../glass/
│  ├─ backdrop/          (pembungkus library Backdrop)
│  ├─ components/        (LiquidButton, FloatingNavBar, GlassCard, ...)
│  └─ tokens/
├─ gradle/libs.versions.toml     (semua versi library di satu tempat)
├─ docs/                          (PRD.md, DESIGN_SPEC.md, screenshot)
└─ README.md
```

### 2.3 Aturan lapisan

- **Ketergantungan:** `ui → domain ← data`. Lapisan `domain` berisi Kotlin murni tanpa import Android.
- **Aliran data satu arah:** Room (DAO, `Flow`) → Repository → ViewModel (`StateFlow<UiState>`) → Compose. Layar hanya menampilkan state dan mengirim event.
- **Use case seperlunya:** CRUD sederhana boleh langsung ViewModel → Repository. Use case hanya untuk logika beraturan: rekomendasi, gaji otomatis, transfer ke goal.
- **Waktu diinjeksi:** logika domain menerima tanggal atau `Clock` sebagai masukan, tidak memanggil jam sistem, agar bisa diuji.
- **Dependency injection:** belum dipakai. Diputuskan saat masuk lapisan data (tahap 3).

---

## 3. Model data

### 3.1 Tabel

| Tabel | Kolom | Catatan |
|---|---|---|
| **wallets** | id, name, type (`MAIN` / `GOAL`), is_archived, created_at | Satu dompet `MAIN` ("Saldo ItungEn") dibuat otomatis saat pertama dibuka |
| **goals** | wallet_id (1:1 ke wallets), title, target_amount, target_date (opsional), planned_monthly, status (`ACTIVE` / `ACHIEVED` / `CANCELLED`) | Setiap goal punya dompetnya sendiri |
| **categories** | id, name, icon, color, type (`EXPENSE` / `INCOME`), is_default | Diisi kategori awal |
| **transactions** | id, wallet_id, category_id (kosong untuk transfer), type (`INCOME` / `EXPENSE` / `TRANSFER_OUT` / `TRANSFER_IN`), amount, note, date, transfer_group_id, recurring_rule_id, created_at | Inti aplikasi |
| **recurring_rules** | id, wallet_id, category_id, type, amount, note, day_of_month, start_date, end_date (opsional), last_generated_date, is_active | Gaji bulanan = satu rule `INCOME`. Tagihan tetap = rule `EXPENSE` |

### 3.2 Aturan data

- **Saldo tidak disimpan**, dihitung dari transaksi (satu sumber kebenaran).
- **`amount`** bertipe `Long`, selalu positif, rupiah bulat. Arah uang ditentukan oleh `type`.
- **Transfer ke goal** = dua baris berpasangan (`TRANSFER_OUT` di dompet utama, `TRANSFER_IN` di dompet goal) yang diikat `transfer_group_id`. Transfer **tidak dihitung sebagai pengeluaran atau pemasukan** di statistik dan rekomendasi.
- **`date`** disimpan sebagai tanggal lokal (epoch-day), terpisah dari `created_at`.
- **Gaji otomatis tahan duplikat:** unique index pada (`recurring_rule_id`, `date`). Jika tanggal gajian tidak ada di bulan itu (misalnya tanggal 31 di bulan 30 hari), transaksi dicatat di hari terakhir bulan.
- **Index:** `transactions(date)` dan `transactions(wallet_id, date)`.
- **Dompet tidak dihapus permanen**, hanya diarsipkan.
- **Total Saldo** di Beranda = saldo dompet utama. Dana yang sudah dialokasikan ke goal ditampilkan di layar Goals, bukan di Total Saldo (lihat §11).
- **DataStore** menyimpan pengaturan: `primarySalaryRuleId` dan `targetTabunganBulanan`. Tanggal gajian dibaca dari rule gaji (tidak diduplikasi di DataStore).

---

## 4. Alur kerja fitur utama

### 4.1 Gaji otomatis

Dua pemicu yang saling melengkapi:

1. **WorkManager periodik** (pemeriksaan harian) membuat transaksi untuk tanggal gajian yang sudah tiba.
2. **Catch-up saat app dibuka** memeriksa semua tanggal gajian yang terlewat sejak `last_generated_date` lalu mencatatnya.

Unique index membuat kedua jalur aman dijalankan berulang. Catch-up wajib ada karena sistem hemat baterai di HP Realme dapat menunda atau menghentikan tugas latar belakang.

### 4.2 Tambah Dana ke goal

Use case `TransferToGoal` berjalan dalam satu transaksi Room: validasi saldo dompet utama ≥ nominal, tulis dua baris transfer berpasangan, perbarui status goal menjadi `ACHIEVED` bila total terkumpul ≥ target. Gagal di tengah berarti tidak ada yang tertulis.

---

## 5. Mesin rekomendasi budget

Satu fungsi murni di `domain/usecase/RecommendationEngine`. Tidak ada Android, database, atau jam sistem di dalamnya.

### 5.1 Bahan hitungan

Rekomendasi dihitung per **siklus gajian** (dari gajian ke gajian berikutnya).

| Bahan | Sumber |
|---|---|
| Pemasukan siklus | Transaksi `INCOME` dalam siklus (bukan transfer) |
| Tagihan tetap | Semua `recurring_rules` `EXPENSE` aktif, **dicadangkan di awal siklus** meski belum tercatat |
| Target tabungan bulanan | DataStore |
| Setoran goals bulanan | Jumlah `planned_monthly` semua goal `ACTIVE` |
| Pengeluaran fleksibel | Transaksi `EXPENSE` **tanpa** `recurring_rule_id` |

Karena tagihan tetap dicadangkan di awal, angkanya tidak terhitung dua kali saat transaksinya muncul.

`planned_monthly` diisi **manual**. Jika goal punya `target_date`, aplikasi menampilkan **nilai saran** = (target − terkumpul) ÷ sisa bulan, yang bisa diterima atau diubah.

### 5.2 Rumus

```
pool           = pemasukan − tagihan_tetap − target_tabungan − setoran_goals
jatah_hari_ini = (pool − fleksibel_terpakai_sebelum_hari_ini) / sisa_hari_termasuk_hari_ini
jatah_minggu   = (pool − terpakai_sebelum_awal_minggu) / sisa_hari_di_awal_minggu
                 × hari_minggu_ini_dalam_siklus
```

- **Rolling:** boros kemarin menurunkan jatah hari ini, hemat menaikkannya.
- **Angka dibekukan di awal hari:** jatah hari ini dihitung dari kondisi *sebelum* hari ini; yang berubah sepanjang hari hanya *sisa* jatah.
- **Minggu** = Senin–Minggu, dipotong di batas akhir siklus.
- **Pembulatan** ke bawah ke kelipatan Rp 1.000.

**Contoh:** pemasukan 3.000.000, tagihan tetap 500.000, target tabungan 500.000, setoran goals 300.000 → pool 1.700.000. Siklus 30 hari: jatah harian awal ≈ Rp 56.000. Di hari ke-11, jika sudah terpakai 700.000, sisa pool 1.000.000 ÷ 20 hari = **Rp 50.000/hari**.

### 5.3 Kasus khusus

| Kasus | Perilaku |
|---|---|
| Pool ≤ 0 | Tidak menampilkan jatah negatif. Tampilkan pesan "target belum realistis" dan tabungan maksimal yang masih mungkin |
| Gaji belum tercatat | Pakai nominal dari rule gaji sebagai perkiraan sementara |
| Pemasukan tambahan (bonus, kiriman) | Masuk ke pool otomatis (dihitung dari transaksi `INCOME` aktual) |
| Pool habis di tengah siklus | Jatah hari ini 0 dan status `MELEBIHI` |
| Hari terakhir siklus | Pembagi minimal 1 |

### 5.4 Keluaran

pool; jatah dan sisa hari ini; jatah dan sisa minggu ini; proyeksi tabungan di akhir siklus (rata-rata pengeluaran saat ini × sisa hari); status `HEMAT` / `ON_TRACK` / `MELEBIHI`.

---

## 6. Desain antarmuka

Acuan visual: file Figma "Itungen" (7 layar). Tema **gelap saja** untuk MVP; token warna disiapkan agar tema terang mudah ditambahkan.

### 6.1 Token desain (diekstrak dari Figma)

| Elemen | Nilai |
|---|---|
| Latar | Gradien radial dari #1C1C1E (α0.4) di tengah atas ke hitam; efektif hampir hitam murni |
| Kartu kaca | Fill putih α0.09, blur 15, border 1 px putih α0.14, radius 12, bayangan 0/12/36 hitam α0.6, highlight dalam 1 px putih α0.05 |
| Tombol bulat (top bar) | Gaya kartu kaca, pill 40×40 |
| Chip filter | Fill putih α0.05, border putih α0.1, pill. Aktif: putih solid, teks hitam |
| Navbar | 351×64, radius 50, blur 32, border 1 px rgba(65,71,84,0.3) |
| Teks | Judul #E2E2E2, utama putih, sekunder #8E8E93, tab tidak aktif rgba(192,198,214,0.7) |
| Aksen | Biru #0A84FF, cyan #64D2FF, hijau #32D74B (pemasukan), merah #FF453A (pengeluaran), oranye #FF9F0A (peringatan), nav aktif Beranda #FFAF4E |
| Progress bar | Track putih α0.1 tinggi 8 pill; isi gradien #0A84FF→#64D2FF, glow biru α0.3 |
| Font | Inter (Regular, Semi Bold, Bold) |
| Skala tipografi (usulan) | Hero saldo 32sp, judul layar 20sp, isi 16sp, label 12sp |

Warna aksen per tab di navbar (Kategori/Insight, Goals, Profil) terlihat berbeda di Figma; nilai hex final ditetapkan saat playground.

### 6.2 Sistem glass dua tingkat

| Tingkat | Dipakai di | Cara kerja |
|---|---|---|
| **Glass Riil** | Navbar, tombol "+", tombol liquid, bottom sheet | Backdrop blur dan refraksi sungguhan (library Backdrop) |
| **Glass Ringan** | Kartu, chip, tombol top bar, ikon bulat | Fill transparan + border + highlight dalam + bayangan; tanpa blur riil |

- **Anggaran GPU:** maksimal **3 permukaan Glass Riil** tampil bersamaan.
- **Keterbacaan:** teks di atas Glass Riil diberi tint gelap tipis agar kontras ≥ 4,5:1 saat konten berwarna lewat di belakangnya. Diverifikasi di HP asli.
- **Latar** tetap hampir hitam; navbar sudah tampak "kaca" karena konten kartu discroll di bawahnya.

### 6.3 Navigasi

- Empat tab: **Beranda, Insight, Goals, Profil**. ("Kategori" pada Figma diganti nama "Insight" karena isinya statistik pengeluaran per kategori; pengelolaan kategori ada di Profil → Kategori Kustom.)
- **Semua Transaksi** dibuka dari "Lihat Semua" di Beranda; navbar tetap tampil dan Beranda tetap aktif.
- **Tambah Transaksi** tampil penuh dengan tombol tutup (X), tanpa navbar.
- **Tambah Dana** dan **Buat Target** berupa bottom sheet. **Ubah Profil, Kategori Kustom, Ekspor/Impor** adalah sub-layar Profil.
- Navbar harus memperhitungkan inset navigasi sistem (jarak 17 dp dari bawah ditambah inset).

### 6.4 Floating navbar

**Tata letak:** navbar selebar penuh 351 dp × 64 dp; tiap tab berisi ikon 22 dp di atas label Inter Semi Bold 11. Tab aktif: ikon dan label warna aksen tab, pil beraksen (isi aksen α0.14, garis tepi aksen α0.45, radius 25). Tombol "+" berupa FAB bulat 56 dp Glass Riil bertint biru, melayang 16 dp di atas navbar dan rata kanan dengannya (pola FAB Telegram).

**Interaksi (hold-and-drag):**

| Fase | Perilaku |
|---|---|
| Sentuh | Indikator langsung "mengembang" (tanpa jeda tekan-lama) |
| Geser | Indikator mengikuti jari secara langsung, dibatasi di dalam pill. Saat bergerak cepat, bentuknya memanjang searah gerakan (spring) |
| Melewati batas tab | Satu haptic ringan per pergantian zona; label tab di bawah indikator menyala. Haptic segmen di Android 14+, cadangan efek standar di Android 13 |
| Lepas jari | Indikator menempel ke tab terdekat dengan spring, **layar baru berpindah saat lepas**. Tap cepat langsung berpindah |

- Posisi seret (`dragX`) hanya hidup di dalam komponen navbar; hanya tab yang *dikomit* saat lepas dikirim ke navigasi, sehingga menggeser tidak memicu komposisi ulang layar-layar di belakangnya.
- **Aksesibilitas:** tiap tab tetap punya aksi tap dan semantik "tab" untuk TalkBack; bila animasi sistem dimatikan, efek melar dinonaktifkan.

### 6.5 Tombol liquid

Saat ditekan: membesar ≈ 8–10% dengan spring yang sedikit memantul, sorotan tepi lebih terang, refraksi menguat; saat dilepas melewati ukuran normal sedikit lalu menetap. Nilai pasti (kekakuan spring, skala, kekuatan refraksi) ditetapkan di glass playground di HP.

### 6.6 Inventaris layar

| Layar | Isi | Status di Figma |
|---|---|---|
| **Beranda** | Kartu Total Saldo, **kartu Jatah Hari Ini** (sisa jatah, jatah minggu ini, status), kartu FYI/status, chip filter, ringkasan Target, Riwayat Terakhir | Ada, **kartu Jatah Hari Ini perlu digambar** |
| **Semua Transaksi** | Pencarian, chip kategori, daftar dikelompokkan per hari | Ada |
| **Tambah Transaksi** | Toggle Pengeluaran/Pemasukan (indikator geser), nominal, kategori, nama opsional, tanggal, **pilihan dompet**, **toggle "ulangi tiap bulan"**, tombol Simpan | Ada; **pilihan dompet dan toggle ulang perlu ditambah** |
| **Insight** | Total pengeluaran siklus, bar segmen per kategori, daftar kategori dengan progres | Ada (sebagai "Kategori") |
| **Goals** | Kartu target utama, "Tambah Dana", goal lain, keadaan kosong | Ada |
| **Profil** | Profil lokal, statistik, menu pengaturan | Ada; **"Keluar" dihapus**, "Bergabung" = tanggal pertama pakai app, menu Notifikasi disembunyikan sampai fitur ada |
| **Tambah Dana** (sheet) | Nominal dari dompet utama ke goal | **Perlu digambar** |
| **Buat Target** (sheet) | Nama, target, tanggal (opsional), `planned_monthly` + nilai saran | **Perlu digambar** |
| **Ubah Profil, Kategori Kustom, Ekspor/Impor Data** | Sub-layar Profil | **Perlu digambar** (P1 untuk Ubah Profil dan Kategori Kustom) |

Perubahan kecil pada Tambah Transaksi: kategori "Transfer" pada Pemasukan diganti nama **"Kiriman"** agar tidak tertukar dengan transfer antar-dompet.

---

## 7. Penanganan error dan validasi

### 7.1 Validasi input

| Field | Aturan |
|---|---|
| Nominal | Hanya angka; ditampilkan dengan pemisah ribuan; wajib > 0; ada batas atas wajar agar tidak melimpah `Long` |
| Kategori | Wajib untuk pemasukan dan pengeluaran |
| Tanggal | Tidak boleh di masa depan |
| Tambah Dana | Nominal ≤ saldo dompet utama; jika tidak, ditolak dengan pesan jelas |
| Buat Target | `target_amount` > 0; `planned_monthly` ≥ 0 |

Pengeluaran **boleh** membuat saldo dompet utama negatif (uang nyata tidak menunggu aplikasi), tetapi ditandai jelas di UI; hanya transfer ke goal yang diblokir.

### 7.2 Operasi atomik dan penghapusan

- Semua operasi multi-baris (transfer, impor, penghapusan berpasangan) memakai transaksi Room.
- Menghapus satu sisi transfer menghapus pasangannya (via `transfer_group_id`).
- Menghapus kategori yang dipakai: transaksi dipindah ke kategori "Lainnya".
- Membatalkan/menghapus goal: saldo dompet goal dikembalikan ke dompet utama (transfer pasangan) sebelum goal diarsipkan.

### 7.3 Penyajian error

- ViewModel mengekspos state error yang eksplisit; UI menampilkan pesan singkat (toast kaca atau snackbar) tanpa crash.
- Kegagalan database ditampilkan sebagai pesan umum dengan opsi coba lagi.
- Pekerjaan gaji otomatis yang gagal dicoba ulang dengan backoff; catch-up saat app dibuka menjadi jaring pengaman.

### 7.4 Migrasi database

- `exportSchema = true`; berkas skema JSON di-commit ke repo.
- Setiap perubahan skema disertai `Migration` eksplisit dan diuji dengan `MigrationTestHelper`.
- **Tidak pernah** memakai `fallbackToDestructiveMigration` (akan menghapus data keuangan).

---

## 8. Backup dan pemulihan

| Format | Fungsi | Isi |
|---|---|---|
| **JSON** | Cadangan penuh dan pemulihan | Semua tabel + `schemaVersion` + `exportedAt` |
| **CSV** | Analisis di spreadsheet (hanya ekspor) | Daftar transaksi |

- Berkas dibuat dan dibaca lewat **Storage Access Framework** (pilih lokasi sendiri; tanpa izin penyimpanan).
- **Impor JSON** = mengganti seluruh data, setelah dialog konfirmasi. Divalidasi (versi skema dikenali, struktur lengkap) lalu ditulis dalam **satu transaksi database**; gagal berarti data lama utuh.
- Berkas cadangan **tidak dienkripsi**; UI mengingatkan pengguna agar menyimpannya di tempat aman.
- Pengaturan DataStore (`primarySalaryRuleId`, target tabungan) ikut dicadangkan.

---

## 9. Strategi pengujian

| Lapisan | Alat | Yang diuji |
|---|---|---|
| **Domain** (JVM, tanpa perangkat) | JUnit | `RecommendationEngine` (semua kasus §5.3), `RecurringGenerator` (tanggal 31 di bulan 30 hari, Februari kabisat, catch-up beberapa bulan, idempoten), `TransferToGoal` (invarian saldo), format dan parsing rupiah |
| **Data** (instrumented, DB in-memory) | AndroidX Test | Query DAO, unique index anti-duplikat, atomisitas transfer, uji migrasi |
| **UI** (instrumented) | Compose UI Test | Alur utama: tambah transaksi, Tambah Dana, tap dan geser navbar |
| **Manual di HP** | Checklist + profiler GPU | Kelancaran glass (target 120 Hz), kontras teks, haptic, bentuk navbar dan posisi FAB "+" |

- Tidak ada target angka cakupan kode; fokus pada domain karena di sanalah bug uang paling mahal.
- **CI (GitHub Actions):** build debug, unit test, dan lint pada setiap push. Bernilai untuk portofolio dan murah.

---

## 10. Log keputusan

| Keputusan | Pilihan |
|---|---|
| Stack | Kotlin + Jetpack Compose (native) |
| minSdk | 33 |
| Data | Offline-first: Room + DataStore; tanpa akun/cloud |
| Modul | `:app` + `:glass` |
| Tema | Gelap dulu; token siap untuk tema terang |
| Setoran goal | `planned_monthly` manual + nilai saran |
| Backup | JSON (pulihkan) + CSV (ekspor) manual |
| Glass | Dua tingkat; ≤ 3 permukaan riil bersamaan |
| Navbar | Indikator langsung mengikuti jari; layar pindah saat lepas |
| Tombol "+" | FAB bulat 56 dp di kanan atas navbar (menggantikan tata letak "+" di samping navbar) |
| Nama tab | "Kategori" → "Insight" |
| Profil | Tanpa "Keluar"; "Bergabung" = tanggal pertama pakai; Notifikasi disembunyikan dulu |

---

## 11. Keputusan yang masih terbuka

1. **Definisi Total Saldo.** Default: saldo dompet utama saja (dana yang sudah masuk goal tidak dihitung). Alternatif: tampilkan total semua dompet.
2. **Tombol hamburger dan ikon riwayat di top bar** (Figma) belum punya fungsi yang ditentukan. Default: ikon riwayat membuka Semua Transaksi; hamburger dihapus sampai ada isinya.
3. **Android Auto Backup.** Default: dimatikan agar satu-satunya cadangan adalah ekspor manual yang terkontrol.
4. ~~**Muat tidaknya label "Beranda"** pada navbar yang menyusut.~~ Selesai: navbar kini selebar penuh (≈ 86 dp per tab) karena tombol "+" pindah ke atas navbar.
5. **Data contoh** Figma tidak konsisten (Sepatu Lari 90% vs 56%; Rp 340.000 disebut mingguan dan bulanan). Satu set data contoh yang konsisten dibuat untuk pratinjau dan tes.
6. **Hex final warna aksen per tab** dan nilai animasi (spring, refraksi) ditetapkan di glass playground.
7. **Injeksi dependensi** (Hilt atau manual) diputuskan pada tahap 3.
8. **Kerapian Figma:** buat variabel Figma dari token §6.1, ganti nama layer otomatis, hapus layer ganda di navbar layar Kategori.
