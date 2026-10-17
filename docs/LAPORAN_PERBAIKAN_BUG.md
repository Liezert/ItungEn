# Laporan perbaikan bug (prompt 17 Okt 2026)

Sumber: [PROMPT_PERBAIKAN_BUG.md](PROMPT_PERBAIKAN_BUG.md). Setiap perbaikan A/B diawali tes yang gagal di kode lama, lalu lulus setelah diperbaiki.

- **Unit test:** `gradlew.bat :app:testDebugUnitTest`, 46 lulus.
- **Instrumented test:** 52 lulus di Realme 13+ (Android 16, 1080×2400), dijalankan lewat `am instrument`. Paket tes dan app debug dihapus lagi setelahnya.
- **Uji manual:** di HP dengan build `bench`.

## A. Prioritas tinggi

| # | Status | Perbaikan | Tes |
|---|---|---|---|
| A1 | Diperbaiki | Kategori jadi satu baris chip di atas keypad dan selalu terlihat; keypad dan kartu nominal dipadatkan. Dicek di HP. | `BugFixUiTest.addTransaction_categoryIsVisibleWithoutScrolling` |
| A2 | Diperbaiki | Ganti jenis tidak lagi memilihkan kategori; tombol menunggu "Pilih kategori dulu". | `EditTypeAndSearchTest.switchingTypeWhileEditing_doesNotPickACategory` |
| A3 | Diperbaiki | Rencana bulanan/mingguan menyimpan tanggal mulai (`startedOn`). Siklus pertama yang dimulai di tengah jalan diprorata: jatah, minggu ini, dan status dihitung sejak tanggal itu. Rencana lama tanpa tanggal mulai tetap penuh. | `BudgetTest.planStartedMidCycle_isProrated` |
| A4 | Diperbaiki | Ganti periode mengonversi nominal (× hari periode baru ÷ lama, dibulatkan ke Rp 1.000) dan memberi tahu. | `BudgetTest.periodSwitch_convertsAmounts` |
| A5 | Diperbaiki | Seluruh panel `LiquidBottomTabs` menerima sentuhan. Dicek di HP. | `BugFixUiTest.navbarGap_doesNotTapThrough` |
| A6 | Diperbaiki | Nominal divalidasi langsung dari isian; pesannya tampil di kartu nominal (keypad tidak bergeser). Galat VM dihapus saat isian berubah, begitu juga di sheet Buat Target dan Tambah Dana. | `BugFixUiTest.addTransaction_amountErrorIsLive_andKeypadStaysPut` (sheet Goals tanpa tes UI khusus) |
| A7 | Diperbaiki | `rememberIndonesianDatePickerState` (locale id-ID di state) dipakai di ketiga pemilih tanggal. Dicek di HP. | `BugFixUiTest.datePicker_isIndonesian` |

## B. Prioritas sedang

| # | Status | Perbaikan | Tes |
|---|---|---|---|
| B1 | Diperbaiki | Pengaturan hanya dibuka bila dialog izin tidak akan muncul lagi; penolakan biasa cukup diberi keterangan. | `NotificationAskTest` |
| B2 | Diperbaiki | Semua nominal Beranda disamarkan: jatah, minggu ini, batas kategori, target, riwayat, dan catat cepat. Dicek di HP. | `BugFixUiTest.hiddenBalance_hidesEveryAmountOnHome` |
| B3 | Diperbaiki | Pesan menyebut penyebabnya (setoran target / tabungan / tagihan); bila setoran target, kartu membuka Goals. Pengeluaran hari ini tetap tampil. | `BudgetTest.unrealisticPlan_namesTheCause` |
| B4 | Sebagian | Setoran > target ditolak; chip saran tidak melebihi sisa target maupun saldo. **Ditunda:** ubah target dan tarik sebagian dana adalah fitur baru, masuk F3. | `GoalRulesTest.validateGoal_planAboveTarget` |
| B5 | Diperbaiki | Kembali ke atas saat cari/filter berubah; header "N transaksi cocok"; cari nominal ("15.000", "15000", "Rp 15.000"); kartu daftar mengisi sampai navbar. Dicek di HP. | `BugFixUiTest.clearingSearch_returnsListToToday`, `EditTypeAndSearchTest.search_matchesAmount` |
| B6 | Diperbaiki | Dua akar: (1) local tema/hemat/latar di :glass statis, jadi kaca yang dilewati rekomposisi tidak membaca nilai baru → kini `compositionLocalOf`; (2) `drawBackdrop` tidak menggambar ulang permukaannya saat parameternya berubah → warna/latar dibaca dari `State` di dalam lambda gambar. Dicek di HP, gelap↔terang. | `BugFixUiTest.themeSwitch_repaintsEveryCard` |
| B7 | Diperbaiki | Semua `maxLines = 1` memakai elipsis; nama kategori utuh di baris chip. | `TextOverflowTest.everySingleLineText_hasOverflow` |
| B8 | Diperbaiki | Warna kalender dibanding jatah harian rata-rata dari rencana: abu ≤ separuh, oranye ≤ jatah, merah lewat. Tanpa anggaran warnanya netral. | `HabitsTest.dayTone_comparesToAllowance`, `BudgetTest.averageDailyAllowance_fromPlan` |
| B9 | Diperbaiki | "Tidak jajan" hanya bisa ditandai untuk hari ini dan kemarin (UI dan ViewModel); tanda lama tetap bisa dibatalkan. | `HabitsTest.noSpend_canOnlyBeMarkedTodayOrYesterday` |

## C. Prioritas rendah

| # | Status | Catatan |
|---|---|---|
| C1 | Diperbaiki | Nol tunggal boleh setelah operator, jadi "5 ÷ 0" menampilkan "Tidak bisa dibagi 0" (`AmountKeypadTest`). |
| C2 | Diperbaiki | Hasil ≤ 0: "Hasilnya harus lebih dari 0"; pembulatan: "= Rp 4 (dibulatkan)" (`AmountKeypadTest`). |
| C3 | Diperbaiki | `formatRupiah` memakai spasi tak-putus (`RupiahPrefix`) (`TransactionRulesTest`, `SummariesTest`). |
| C4 | Diperbaiki | Chip Belanja memakai ikon tas. |
| C5 | Diperbaiki | Baris Pengaturan diberi label "Rencana anggaran". |
| C6 | Diperbaiki | Tab aktif yang diketuk dari sub-layar kembali ke akar tab. Ketukan itu ditangkap indikator, jadi `LiquidBottomTabs` mendapat `onTabReselected`. Dicek di HP. |
| C7 | Diperbaiki | Baris transfer (Semua Transaksi dan Beranda) membuka Goals. Dicek di HP. |
| C8 | Diperbaiki | Setoran ke target memakai ikon target abu-abu; panah masuk hijau hanya untuk dana yang kembali. |
| C9 | Tidak diubah | FAB melayang memang menutupi konten di bawahnya saat diam; `bottomClearance` sudah memberi ruang agar baris terakhir bisa di-scroll keluar dari bawah FAB. |
| C10 | Diperbaiki | Selisih kecil tampil "(<1%)". |
| C11 | Diperbaiki | Streak hanya tampil di bulan berjalan. |
| C12 | Diperbaiki | Hari mendatang dibacakan "belum terjadi". |
| C13 | Diperbaiki | Bila uang fleksibel sudah habis: "Uang untuk belanja siklus ini sudah habis, N hari sebelum uang masuk lagi." |
| C14 | Bukan bug | Jam hanya tampil bila transaksi dicatat pada tanggalnya sendiri (`clockLabel`). "Kemarin" tanpa jam berarti dicatat hari ini dengan tanggal kemarin, jadi jam pencatatannya tidak bermakna. |
| C15 | Diperbaiki | Tutup/Kembali dengan isian yang belum disimpan meminta konfirmasi ("Buang isian?" / "Simpan perubahan anggaran?"). Dicek di HP. |
| C16 | Diperbaiki | Label Profil menjadi "SALDO UTAMA". |

## Ditunda

- **F1–F9** (rekomendasi fitur) belum dikerjakan; F3 (ubah target, tarik sebagian) mencakup sisa B4.
- **Belum diuji di HP:** kunci aplikasi, ekspor/pulihkan sungguhan, isi notifikasi, sinkron Firestore.
