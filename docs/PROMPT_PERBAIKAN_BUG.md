# Prompt: Perbaikan bug ItungEn hasil uji di HP (17 Okt 2026)

> Salin seluruh isi file ini ke AI/developer yang akan memperbaiki. Semua temuan di bawah sudah direproduksi di HP sungguhan; akar masalah yang disebut "terkonfirmasi di kode" sudah dicek di sumbernya.

---

## Peran dan konteks

Kamu adalah developer Android senior yang akan memperbaiki bug di **ItungEn**, aplikasi pencatat keuangan pribadi.

- **Repo:** `C:\MyProject\ItungEn`, branch `itungen-optimasi`, commit terakhir `9ff1e0d`.
- **Stack:** Kotlin 2.3, Jetpack Compose (BOM 2026.03), Material3, Room v8, WorkManager, Firebase Auth + Firestore. Modul `:app` dan `:glass` (komponen kaca, library Backdrop).
- **Perangkat uji:** Realme 13+ (RMX5000), Android 16, 1080×2400, density 480 (bawaan), font scale 1.0. Bahasa sistem HP: Inggris.
- **Build uji:** varian `bench` (`com.liezert.itungen.bench`), dipasang bersih, masuk sebagai **tamu**, lalu diisi `BenchSeedReceiver` (data contoh + 400 transaksi acak).

## Aturan kerja

1. Untuk setiap bug, **tulis dulu tes yang gagal** (unit test untuk logika, Compose UI test untuk UI), lalu perbaiki, lalu buktikan tesnya lulus.
2. Perbaiki di **akar masalah**, bukan di gejala. Kalau satu pola salah muncul di banyak tempat (misalnya pesan galat yang tidak hilang), perbaiki polanya di semua tempat.
3. Jangan ubah tampilan kaca (glass) kecuali bugnya memang di situ. Jangan pakai `fallbackToDestructiveMigration`.
4. Jangan jalankan `connectedAndroidTest` di HP pengguna karena datanya ikut terhapus. Pakai `am instrument` dan hapus hanya paket tes.
5. Satu commit per kelompok perbaikan, pesan commit dalam bahasa Indonesia.
6. Di akhir, laporkan per bug: **diperbaiki / ditunda (alasan)**, plus nama tesnya.

---

## A. Prioritas TINGGI (data salah, angka menyesatkan, atau fitur inti tersembunyi)

### A1. Pilihan kategori tersembunyi di layar Tambah/Ubah Transaksi
- **Reproduksi:** Beranda → tombol + . Di layar 1080×2400 @480dpi, kotak kategori (Makanan, Transportasi, …) **sama sekali tidak terlihat**; ia ada di bawah kartu nominal di dalam `verticalScroll`, tertutup keypad. Tidak ada petunjuk bahwa layar bisa di-scroll.
- **Akibat:** pengguna menyimpan transaksi tanpa pernah melihat kategori, sehingga semuanya otomatis masuk kategori pertama ("Makanan" / "Gaji"). Insight dan batas kategori jadi salah.
- **Lokasi:** `ui/addtransaction/AddTransactionScreen.kt` sekitar baris 176–189. Komentar di sana bilang "Muat satu layar tanpa scroll", padahal kenyataannya tidak muat.
- **Harapan:** kategori selalu terlihat tanpa scroll di layar 1080×2400 @480dpi, misalnya satu baris chip horizontal di atas keypad, keypad yang lebih rapat, atau kategori sebagai langkah setelah nominal.
- **Terima bila:** ada UI test yang memastikan node kategori "Makanan" `isDisplayed()` saat layar dibuka dengan ukuran 411×891dp.

### A2. Mengganti jenis transaksi saat Ubah diam-diam memindahkan kategori
- **Reproduksi:** Semua Transaksi → ketuk "Ojek" (Pengeluaran, Transportasi) → ubah nominal → ketuk **Pemasukan** → Simpan Perubahan. Hasilnya "Ojek · **Gaji** · +Rp 20.000".
- **Akar (terkonfirmasi di kode):** `AddTransactionViewModel.selectType` mengosongkan `categoryId`, lalu `uiState` mengisi ulang dengan `categories.firstOrNull()`. Karena A1, pengguna tidak melihat bahwa kategorinya berubah.
- **Harapan:** saat jenis diganti, kategori **tidak dipilih otomatis**. Tampilkan "Pilih kategori." (`CATEGORY_REQUIRED` sudah ada) sampai pengguna memilih sendiri. Minimal, kategori yang terpilih harus terlihat.

### A3. Jatah harian membengkak bila anggaran diatur di tengah siklus (satu akar, tiga gejala)
- **Reproduksi:** tanggal 17 Okt, atur anggaran Bulanan, gajian tanggal 25, pemasukan 3.000.000, tagihan 500.000, tabungan 300.000. Muncul:
  1. **Jatah hari ini Rp 275.000**, padahal kalau dibagi rata satu siklus seharusnya sekitar Rp 73.000/hari. Seluruh pool sebulan dibagi ke 8 hari yang tersisa.
  2. **Sisa minggu ini Rp 1.159.000**, padahal minggu itu tinggal 2 hari (Sab–Min). Angka ini lebih besar dari 2 × jatah harian, jadi kedua angka saling bertentangan.
  3. Status langsung **"Hemat"**, karena 22 hari sebelum aplikasi dipakai dihitung sebagai hari "tanpa belanja".
- **Akar (terkonfirmasi di kode):** `domain/usecase/Budget.kt` `computeBudget` menganggap rencana berlaku sejak `cycle.start` dan tidak tahu kapan pengguna mulai mencatat. `weekAllowance` memakai `weekFrom = max(weekStart, cycleStart)`, dan `fullDays`/`expectedBefore` menghitung hari sebelum aplikasi dipakai.
- **Harapan:** di siklus pertama, tanyakan "Sisa uangmu sekarang berapa?" (paling jujur), atau prorata pool dari tanggal mulai mencatat. Mulai tren/status dari tanggal itu, dan potong minggu pertama dari tanggal itu juga. Simpan tanggal mulai di rencana yang tersinkron (`SyncedSettings`) dan di cadangan JSON.
- **Terima bila:** unit test `computeBudget` untuk rencana yang dibuat tanggal 17 dengan siklus 25–24 menghasilkan jatah ≈ (sisa uang ÷ 8 hari), `weekAllowance` ≤ jatah × hari tersisa di minggu itu, dan status bukan HEMAT di hari pertama.

### A4. Ganti periode Bulanan → Mingguan membawa angka tanpa konversi
- **Reproduksi:** Pengaturan → Jatah harian. Saat Bulanan berisi 3.000.000 / 500.000 / 300.000, ketuk **Mingguan**. Label berubah jadi "per minggu", tetapi angkanya tetap 3.000.000 / 500.000 / 300.000. Rencana jadi sekitar 4× lebih besar dan pratinjau langsung menampilkan Rp 439.000/hari.
- **Harapan:** konversi otomatis (× 7/30 atau sebaliknya) dengan pemberitahuan, atau kosongkan field dan minta diisi ulang. Hal yang sama berlaku untuk "Tak tentu".

### A5. Ketukan di sela tombol navbar tembus ke konten di belakangnya
- **Reproduksi:** Insight, scroll sampai sel kalender tanggal 17 berada tepat di bawah navbar, lalu ketuk area kosong navbar di antara "Goals" dan "Profil" (x≈784, y≈2250 px). **Detail kalender tanggal 17 terbuka**, artinya ketukan diteruskan ke konten di belakang kaca.
- **Akibat:** salah ketuk tanpa disadari, misalnya membuka atau menandai hari, atau menekan baris transaksi yang ada di belakang navbar.
- **Lokasi:** `glass/.../components/LiquidBottomTabs.kt`. Kontainer kapsul tidak mengonsumsi pointer; hanya item tab yang bisa diklik.
- **Harapan:** seluruh area kapsul navbar (dan FAB) mengonsumsi sentuhan, misalnya dengan `Modifier.pointerInput { awaitEachGesture { … consume } }` di kontainer, atau `clickable(enabled=false)` / `indication=null` di sepanjang kapsul.
- **Terima bila:** ada UI test yang mengetuk koordinat sela navbar di atas tombol uji, dan tombol uji tidak terpicu.

### A6. Pesan galat tidak hilang setelah input dibetulkan (pola di semua formulir)
- **Reproduksi:**
  - Tambah Transaksi: ketik 999.999.999.999 × 99 lalu `=`, lalu Simpan. Muncul "Nominal terlalu besar.". Tekan **C** atau ketik nominal valid 25.000: pesan **tetap ada**.
  - Buat Target: nama kosong → "Isi nama target.". Isi nama: pesan tetap ada sampai disimpan.
  - Tambah Dana: nominal > saldo → "Saldo utama tidak cukup.". Kecilkan nominal: pesan tetap ada.
- **Akar (terkonfirmasi di kode):** di Tambah Transaksi, nominal adalah state lokal Compose, sedangkan `error` ada di ViewModel dan hanya dibersihkan oleh `selectCategory`, `selectType`, `selectDate`. Pola yang sama ada di `GoalSheets`.
- **Harapan:** galat dihapus setiap kali field yang bersangkutan berubah.
- **Efek samping yang ikut diperbaiki:** saat pesan muncul, keypad bergeser ±96px ke atas sehingga tombol pindah di bawah jari (tap berikutnya bisa salah tombol). Sediakan ruang tetap untuk pesan, atau tampilkan pesan di dalam kartu nominal.

### A7. Pemilih tanggal masih berbahasa Inggris (perbaikan sebelumnya belum mengena)
- **Reproduksi:** Tambah Transaksi → ketuk tanggal. Judulnya "October 2026", hari "S M T W T F S", label aksesibilitas "Saturday, 17 October 2026". Hanya judul atas "17 Okt 2026" yang berbahasa Indonesia. Terjadi di HP yang bahasa sistemnya Inggris.
- **Akar (terkonfirmasi di kode):** locale `DatePicker` ditentukan oleh `rememberDatePickerState(locale = …)` saat state dibuat. State itu dibuat di `AddTransactionScreen.kt:140`, `BudgetScreen.kt:265`, dan `GoalSheets.kt:115`, **di luar** `CompositionLocalProvider` yang memaksa `id-ID` di `Dialogs.kt:85-89`.
- **Harapan:** buat satu helper `rememberIndonesianDatePickerState(...)` yang meneruskan `locale = Locale.forLanguageTag("id-ID")`, lalu pakai di ketiga tempat. Override `LocalConfiguration` di `Dialogs.kt` bisa dihapus kalau sudah tidak perlu.

---

## B. Prioritas SEDANG (UX menyesatkan atau privasi)

### B1. Penolakan izin notifikasi pertama langsung melempar ke Pengaturan sistem
- **Reproduksi:** Pengaturan → Pengingat harian → dialog izin → **Don't allow**. Aplikasi langsung membuka halaman "Allow notifications" milik sistem.
- **Akar (terkonfirmasi di kode):** `SettingsScreen.kt:124-129`, callback `RequestPermission` memanggil `openNotificationSettings` untuk setiap `!granted`. Komentarnya bilang "ditolak permanen", tapi tidak ada pengecekan.
- **Harapan:** buka pengaturan hanya bila `shouldShowRequestPermissionRationale` bernilai false **dan** dialog sudah pernah ditolak sebelumnya. Kalau tidak, tampilkan penjelasan singkat dan biarkan toggle mati.

### B2. "Sembunyikan saldo" hanya menyembunyikan satu kartu
- **Reproduksi:** Beranda → ikon mata. Total Saldo / Masuk / Keluar menjadi "••••••", tetapi angka berikut tetap terlihat:
  - "Lewat Rp 55.000 dari jatah", "Sisa minggu ini".
  - "Makanan lewat batas: Rp 327.000 dari Rp 30.000".
  - "Rp 1.000.000 dari Rp 2.000.000" di target.
  - Semua nominal di Riwayat Terakhir.
- **Harapan:** saat aktif, semua nominal di Beranda dan Profil disamarkan, sesuai janji teksnya ("Nominal di Beranda dan Profil diganti titik"). Pertimbangkan juga Insight dan daftar transaksi, atau sebutkan dengan jujur bahwa layar itu tetap menampilkan angka.

### B3. Pesan "Anggaran belum realistis" menyalahkan hal yang salah
- **Reproduksi:** buat target "Laptop baru" dengan rencana setoran Rp 2.500.000/bulan sementara pemasukan 3 juta dan tagihan 500 ribu. Beranda menampilkan "…Tabungan maksimal yang masih mungkin: Rp 0/bulan". Penyebab sebenarnya adalah **setoran target**, bukan tabungan. Seluruh kartu jatah harian juga hilang.
- **Harapan:** sebutkan komponen yang membuat anggaran minus (mis. "Setoran target Rp 2.500.000 melebihi sisa uang Rp 2.200.000") dan arahkan ke tempat yang benar (Goals). Tetap tampilkan pengeluaran hari ini walau anggarannya tidak realistis.

### B4. Validasi target tabungan kurang
- Rencana setoran per bulan boleh **lebih besar dari target dananya** (2.500.000/bulan untuk target 2.000.000) tanpa peringatan.
- Chip cepat "Rencana Rp 2.500.000" di sheet Tambah Dana menawarkan nominal yang melebihi sisa target dan saldo.
- Belum ada cara **mengubah** target (nama/nominal/tanggal) atau **menarik sebagian** dana. Satu-satunya pilihan adalah "Batalkan target" (semua dana kembali). Ini kendala nyata untuk dana darurat.

### B5. Setelah mengosongkan pencarian, daftar tidak kembali ke atas
- **Reproduksi:** Semua Transaksi → cari "hiburan" (hasil teratas 21 Sep) → hapus teks pencarian. Daftar menampilkan **21 SEP 2026** di paling atas; HARI INI baru muncul setelah scroll naik. Pengguna mengira transaksi hari ini hilang.
- **Akar (dugaan kuat):** `LazyColumn` di `TransactionsScreen.kt:232` mempertahankan posisi scroll berdasarkan key item pertama yang terlihat.
- **Harapan:** saat query atau filter kategori berubah, panggil `listState.scrollToItem(0)`.
- **Masalah terkait di layar yang sama:**
  - Header "44 transaksi bulan ini" tidak berubah saat sedang mencari/memfilter.
  - Pencarian tidak mencocokkan nominal ("15000" → "Tidak ada transaksi yang cocok").
  - Kartu daftar berhenti sekitar 75% tinggi layar dan menyisakan area hitam kosong di bawahnya. Biarkan daftar mengisi sampai navbar, dengan `contentPadding` bawah.

### B6. Kartu "DATA" di Pengaturan tertinggal satu tema
- **Reproduksi:** Pengaturan → matikan/nyalakan Mode gelap. Kartu DATA (Ekspor JSON/CSV/Pulihkan) tetap memakai warna **tema sebelumnya**: kartu putih dengan tombol hitam di mode gelap, kartu hitam di mode terang. Kondisi ini bertahan ≥3 detik dan baru benar setelah di-scroll.
- **Dugaan:** warna permukaan `GlassCard` yang berisi `LiquidButton` dibaca di lambda gambar atau layer yang tidak diinvalidasi saat tema berubah (backdrop latar polos di-cache). Kartu lain yang hanya berisi toggle ikut berubah dengan benar.
- **Harapan:** semua kartu berganti tema di frame yang sama. Tambahkan screenshot test untuk kedua tema.

### B7. Teks dipotong tanpa tanda "…"
- **Reproduksi:** catatan "Makan siang warteg" tampil di Riwayat Terakhir sebagai "Makan siang" saja; kata terakhir hilang tanpa elipsis.
- **Lokasi:** `HomeScreen.kt:423` dan banyak `Text(..., maxLines = 1)` lain tanpa `overflow = TextOverflow.Ellipsis`, misalnya di `HomeScreen`, `InsightScreen:302/320`, `ProfileScreen`, `SettingsScreen:410`, `GoalSheets:232`, `AppNav:306`, dan `UndoToast:64`.
- **Harapan:** semua teks satu baris memakai `TextOverflow.Ellipsis`. Label kategori "Transport…" di grid tambah transaksi sebaiknya muat utuh (perkecil font atau izinkan 2 baris).

### B8. Warna kalender menyiratkan boros padahal tidak
- **Reproduksi:** bulan dengan satu pengeluaran Rp 25.000 (di bawah jatah Rp 275.000): sel tanggal itu berwarna **merah penuh**.
- **Akar (terkonfirmasi di kode):** `Habits.kt` `heatLevel` membagi relatif terhadap hari terbesar bulan itu, jadi hari terbesar selalu level 4 (merah).
- **Harapan:** ukur terhadap jatah harian kalau anggaran ada (≤50% hijau/netral, ≤100% kuning, >100% merah). Pakai skala relatif hanya kalau anggaran belum diatur, dengan warna netral, bukan merah.

### B9. Streak dan "tidak jajan" bisa ditandai mundur tanpa batas
- **Reproduksi:** Insight → kalender → ketuk 14, 15, 16 Okt → "Tandai tidak jajan". Streak naik dari 2 ke 4. Hari sebelum aplikasi dipasang dan sebelum akun dibuat juga bisa ditandai, jadi streak bisa digelembungkan sebulan penuh dalam satu menit.
- **Harapan:** hanya izinkan penandaan untuk hari ini dan kemarin, atau hari sejak tanggal mulai mencatat (lihat A3). Jangan tampilkan tombol tandai untuk hari sebelum tanggal pemasangan.

---

## C. Prioritas RENDAH (pemolesan)

| # | Temuan | Lokasi / petunjuk |
|---|---|---|
| C1 | Keypad: `5 ÷ 0` diam-diam menjadi 5, karena angka 0 setelah operator ditolak `keypadDigit` dan `=` dengan operator di akhir mengabaikannya. Tidak ada pesan "Tidak bisa dibagi nol". | `domain/usecase/AmountKeypad.kt` |
| C2 | Keypad: hasil negatif (`10 − 20`) membuat `=` tidak bereaksi dan Simpan nonaktif **tanpa pesan apa pun**. `7 ÷ 2` dibulatkan ke 4 tanpa tanda. | `AddTransactionScreen.kt` `onEquals`, `enabled` |
| C3 | Pemisah "Rp" dan angka memakai spasi biasa, sehingga baris bisa patah jadi "Rp⏎30.000" (terlihat di kartu kategori Insight). Pakai spasi tak-putus `\u00A0` di `formatRupiah`. | `domain/model/Rupiah.kt` |
| C4 | Ikon chip **Belanja** sama dengan **Lainnya** (titik tiga). | `ui/components/CategoryStyle.kt:31`: argumen ke-4 `ic_add_other` → ganti dengan ikon tas |
| C5 | Label Pengaturan "**Jatah harian**" menampilkan "Rp 3.000.000/bulan · uang masuk tanggal 25" (pemasukan, bukan jatah). Ganti label menjadi "Anggaran". | `SettingsScreen.kt` `BudgetPlan.summary()` |
| C6 | Mengetuk tab yang sedang aktif (Profil saat di Pengaturan) tidak kembali ke akar tab. | `ui/navigation/AppNav.kt` |
| C7 | Baris transfer ke target ("Ke Laptop baru") tidak bereaksi saat diketuk atau digeser, padahal petunjuk di atas daftar bilang "ketuk untuk mengubah, geser untuk menghapus". Beri umpan balik, misalnya toast "Kelola transfer di Goals" atau buka goal-nya. | `TransactionsScreen.kt` |
| C8 | Ikon transfer ke target adalah panah masuk **hijau** ↙, padahal uang keluar dari saldo utama (nominal "-Rp 450.000"). | ikon baris transfer di Home/Transactions |
| C9 | Saat diam, FAB + menutupi nominal baris terakhir Riwayat Terakhir ("+Rp 1.500…") dan sudut kartu. Beri ruang bawah atau geser FAB. | `HomeScreen` / `AppNav` |
| C10 | "Turun Rp 4.000 (0%) dibanding Juli": persen dibulatkan ke 0%. Tampilkan "<1%" atau satu desimal. | `InsightScreen` |
| C11 | Streak saat ini ("Tercatat 43 hari berturut-turut") tetap tampil saat melihat bulan lalu (Agustus), sehingga terkesan milik Agustus. | `SpendingCalendar.kt` |
| C12 | Aksesibilitas kalender: hari di masa depan dibacakan "tidak ada pengeluaran". Seharusnya "belum terjadi" atau dinonaktifkan. | `SpendingCalendar.kt` `DayCell` |
| C13 | Saat jatah sudah lewat, teks berbunyi "uang habis sekitar 17 Okt" padahal 17 Okt adalah hari ini. Gunakan "Uang fleksibel siklus ini sudah habis". | `HomeScreen` + `runsOutOn` di `Budget.kt` |
| C14 | Di Riwayat Terakhir, "Hari ini, 07:27" memakai jam tetapi "Kemarin" tidak. Seragamkan formatnya. | `HomeScreen.kt:404` |
| C15 | Keluar dari layar Anggaran setelah mengubah periode/angka tanpa menekan Simpan membuang perubahan **tanpa konfirmasi**. Hal yang sama terjadi saat menutup (X) layar Ubah Transaksi. | `BudgetScreen`, `AddTransactionScreen` |
| C16 | Profil menampilkan "TOTAL SALDO Rp 475.000" (saldo utama saja), padahal total uang pengguna Rp 1.475.000 termasuk dana di target. Ganti label menjadi "Saldo utama" atau tampilkan keduanya. | `ProfileScreen` |

---

## D. Sudah diuji dan berjalan baik (jangan dirusak)

- Masuk sebagai tamu, Beranda kosong, pesan kosong yang jelas.
- Simpan pengeluaran/pemasukan, tanggal kemarin, saldo dan total bulan ikut berubah.
- Validasi "Nominal terlalu besar", nama target kosong, dan saldo tidak cukup (pesannya muncul; masalahnya hanya tidak hilang, lihat A6).
- Tanggal di masa depan tidak bisa dipilih, dan "Bulan berikutnya" di Insight berhenti di bulan ini.
- Anggaran: peringatan "Tagihan + tabungan + setoran target melebihi uang masuk".
- Batas kategori 80% (oranye) dan lewat batas, tampil di Insight dan Beranda.
- Kalender: detail hari, tandai/batalkan tidak jajan.
- Buat target, tambah dana (50%), dialog batalkan target ("dana dikembalikan ke saldo utama").
- Ubah transaksi tersimpan dengan benar (selain A2).
- Geser hapus → Urungkan → scroll menjauh dan kembali: baris **tetap ada** (perbaikan `9ff1e0d` terbukti).
- Pencarian tidak peka huruf besar/kecil dan mencocokkan nama kategori.
- Catat cepat menampilkan toast "Tercatat: Makan siang · Urungkan".
- Mode gelap/terang (selain B6), Mode hemat bisa dinyalakan.

## E. Belum diuji di HP (perlu dicek terpisah)

- **Kunci aplikasi / biometrik:** pemilik HP tidak bisa standby.
- **Ekspor/pulihkan cadangan JSON dan CSV:** pemilih file sistem terbuka dengan benar, tetapi sengaja dibatalkan agar tidak menulis ke folder Download pribadi. Logikanya sudah dicakup `BackupTest`.
- **Isi dan aksi notifikasi pengingat:** izin notifikasi sengaja ditolak.
- **Masuk Google/email dan sinkron Firestore antar-HP:** butuh kredensial.
- **Rotasi layar, ukuran font besar, layar kecil (≤ 720p):** sangat mungkin memperparah A1 dan A6.

## F. Rekomendasi fitur (kerjakan **setelah** A dan B selesai)

Urutan berdasarkan dampak untuk pengguna utama (pelajar/mahasiswa/pekerja muda dengan uang saku atau gaji). Setiap fitur berangkat dari kendala yang benar-benar ditemui saat uji.

| Prioritas | Fitur | Kenapa perlu (bukti dari uji) | Cakupan minimal |
|---|---|---|---|
| 1 | **Sisa uang sekarang** saat mengatur anggaran pertama kali | Akar A3: tanpa angka ini, jatah siklus pertama pasti salah. | Satu field "Uang yang kamu pegang sekarang (semua dompet)", jadi pool siklus pertama. Simpan `startedOn`. Tersinkron + masuk cadangan JSON. |
| 2 | **Transaksi berulang / tagihan tetap otomatis** | Anggaran sudah punya "Tagihan tetap" (kos, listrik, cicilan), tetapi pengguna tetap harus mencatatnya manual dengan kategori Tagihan agar tidak memotong jatah. Mudah lupa, dan jatah jadi salah. | Daftar tagihan (nama, nominal, tanggal) → notifikasi "Kos Rp 800.000 jatuh tempo hari ini · Catat". Total tagihan = jumlah daftar ini (gantikan field angka tunggal). |
| 3 | **Ubah target & tarik sebagian dana** | B4: satu-satunya jalan keluar sekarang adalah "Batalkan target" (semua dana kembali). | Sheet Ubah (nama, target, tanggal, rencana) + "Ambil dana" dengan nominal → TRANSFER_IN ke saldo utama. |
| 4 | **Beberapa dompet** (Tunai, Bank, GoPay/OVO/DANA) + transfer antar-dompet | Sekarang hanya ada "saldo utama". Pengguna Indonesia biasanya memegang 2–4 tempat uang, jadi saldo di aplikasi tidak pernah cocok dengan kenyataan. | Model wallet sudah ada (`WalletEntity`). Tambah UI pilih dompet di Tambah Transaksi dan saldo per dompet di Beranda. Jatah harian tetap dari gabungan dompet non-target. |
| 5 | **Widget layar utama** "Jatah hari ini" + 2 tombol catat cepat | Inti aplikasi adalah "boleh belanja berapa hari ini?". Kalau jawabannya ada di layar utama, pencatatan naik tanpa perlu membuka aplikasi. | Glance widget 2×2: sisa jatah hari ini + chip Catat cepat (pakai `DemoData`/template yang sama). Hormati "Sembunyikan saldo". |
| 6 | **Kategori kustom** | Delapan kategori bawaan tidak mencakup kebutuhan umum (Kuota/Pulsa, Kos, Rokok/Kopi, Kirim ke ortu). Akibatnya banyak yang masuk "Lainnya" dan Insight jadi kurang berguna. | Tambah, ubah nama/ikon/warna, sembunyikan kategori. Uuid stabil untuk sinkron dan batas kategori. |
| 7 | **Pencarian dan filter lanjutan** | B5: nominal tidak bisa dicari, tidak ada filter jenis atau rentang tanggal. | Cocokkan nominal (abaikan titik), filter Pengeluaran/Pemasukan/Transfer, rentang tanggal, dan jumlah hasil di header. |
| 8 | **Ringkasan akhir minggu / akhir siklus** | Data streak, hari tidak jajan, dan batas kategori sudah ada, tetapi tidak pernah "dilaporkan balik" ke pengguna. | Notifikasi Minggu malam: "Minggu ini Rp X dari jatah Rp Y · kategori terboros: Makanan". Kartu ringkasan siklus di Insight saat siklus berganti. |
| 9 | **Catatan utang-piutang** ("pinjam teman") | Sangat umum di kalangan pelajar/mahasiswa. Tanpa fitur ini, uang yang dipinjamkan terbaca sebagai "pengeluaran" dan merusak jatah. | Jenis transaksi Pinjam/Dipinjam per orang, tidak masuk jatah harian, dengan tombol "Sudah dibayar". |

**Sengaja tidak direkomendasikan sekarang:**
- Scan struk / OCR: mahal dan akurasinya rendah untuk struk warung.
- Investasi/kurs: di luar fokus "uang harian".
- Fitur sosial/berbagi: risiko privasi untuk data keuangan.

## Definisi selesai

- Semua item A dan B diperbaiki atau ditunda dengan alasan tertulis. Item C boleh dikerjakan bertahap.
- Setiap perbaikan A/B punya tes yang gagal sebelum perbaikan dan lulus sesudahnya.
- `gradlew.bat testDebugUnitTest` dan instrumented test (lewat `am instrument`) semuanya lulus.
- Uji ulang manual di HP 1080×2400 untuk A1, A5, A6, A7, B5, dan B6, dengan screenshot sebelum/sesudah.
