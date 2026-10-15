# Performa ItungEn

Perangkat ukur: **Realme RMX5000 (Realme 13+ 5G), Android 16, layar 120 Hz**, tersambung USB (mengisi daya).
Semua angka di HP memakai build **`bench`** (salinan rilis, paket `com.liezert.itungen.bench`, data terpisah dari
app harian) kecuali disebut lain. Median dari 10 kali (start) atau 4–5 kali gestur (frame).

## Cara mengukur

| Apa | Cara |
|---|---|
| Ukuran APK | `gradlew.bat :app:assembleDebug :app:assembleRelease :app:assembleBench`, ukuran file di `app/build/outputs/apk/` |
| Waktu build | `gradlew.bat clean` lalu tugas yang sama, diukur dengan `Measure-Command` (PowerShell) |
| Start dingin | `am force-stop` lalu `am start -W` (TotalTime = frame pertama). "Siap dipakai" = baris `Fully drawn` dari `ReportDrawnWhen` di Beranda (data pertama tampil). Mode kompilasi dipaksa dengan `cmd package compile -f -m verify` (seperti baru dipasang dari APK) atau `-m speed-profile`. Putaran pertama setelah install dibuang karena HP masih sibuk |
| Frame | `dumpsys gfxinfo <paket> reset`, lalu `input swipe` naik-turun (scroll) atau geser navbar Beranda↔Profil, lalu `dumpsys gfxinfo`. Anggaran frame 120 Hz = 8,3 ms |
| Data besar | `DataScaleTest` (androidTest, database in-memory 30.000 transaksi, build debug): waktu memuat + menghitung ulang agregat Beranda/Insight setelah satu perubahan. Build bench juga bisa diisi data uji: `adb shell am broadcast -n com.liezert.itungen.bench/com.liezert.itungen.BenchSeedReceiver --ei count 30000` |
| Compose | `gradlew.bat :app:assembleBench -PcomposeReports` → `app/build/compose_compiler/` |

Build `bench` butuh `app/src/bench/google-services.json` lokal (salinan `app/google-services.json` dengan nama paket
`com.liezert.itungen.bench`; tidak di-commit) agar biaya inisialisasi Firebase saat start tetap sama dengan rilis.

## Baseline (Fase A, sebelum perubahan)

### Build dan APK

| Ukuran | Nilai |
|---|---|
| APK debug | 58,98 MB |
| APK release (tanpa R8, belum ditandatangani) | 45,74 MB |
| Font `inter_variable.ttf` | 876 KB |
| Build bersih `assembleDebug` | 149 s |
| Build bersih `assembleRelease` (termasuk lintVital) | 354 s |
| `testDebugUnitTest` | 23 s, 34 tes hijau |
| `lintDebug` | 160 s, **gagal: 2 error** (NewApi `LocalDate.ofInstant` butuh API 34 padahal minSdk 33; `local.properties` tidak di-escape), 21 peringatan |

### Start dingin (bench)

| Data | Mode | Frame pertama | Siap dipakai |
|---|---|---|---|
| Data contoh (±150 transaksi) | verify | 465 ms | 685 ms |
| Data contoh | speed-profile | 427 ms | 531 ms |
| 30.000 transaksi | verify | 476 ms | 1.499 ms |
| 30.000 transaksi | speed-profile | 471 ms | 1.441 ms |

### Frame (bench, 30.000 transaksi, tema gelap)

| Layar / gestur | Frame jank | p50 | p90 | p99 |
|---|---|---|---|---|
| Beranda, scroll | 31,4% | 20 ms | 34 ms | 38 ms |
| Geser navbar Beranda↔Profil | 7,5% | 13 ms | 22 ms | 53 ms |
| Semua Transaksi, scroll | 2,8% | 10 ms | 13 ms | 31 ms |
| Pengaturan, scroll | 64,3% | 34 ms | 40 ms | 44 ms |

### Data (DataScaleTest, 30.000 transaksi, build debug)

| Langkah | Median |
|---|---|
| Muat semua transaksi + agregat Beranda + Insight (dijalankan ulang setiap ada satu perubahan) | 598 ms |

### Permukaan kaca yang tampil bersamaan (analisis kode)

Setiap permukaan = satu `drawBackdrop` (layer offscreen + blur + lensa + vibrancy). Kaca di dalam layar membiaskan
latar `AppBackground` yang polos hitam/terang (plus vinyet di Beranda/Semua Transaksi). Saat scroll, posisi setiap
kartu berubah sehingga backdrop-nya direkam ulang tiap frame.

| Layar | Kartu kaca | Tombol/toggle kaca | Navbar + FAB + blur atas | Total di layar |
|---|---|---|---|---|
| Beranda | 2–3 (Jatah, Saldo, Target/Riwayat) | 3–4 (riwayat, catat cepat) | 3 + 1 + 1 | ±11 |
| Semua Transaksi | 2 (cari, daftar) | 1 + chip 4–5 | 3 + 1 | ±11 |
| Insight | 2 | 1 + toggle (3) | 3 + 1 | ±10 |
| Pengaturan | 6–8 kartu/baris | 3–7 tombol, 3 toggle (2 lapis) | 3 + 1 | ±18 |

### Compose (laporan compiler)

| Metrik | Nilai |
|---|---|
| Composable restartable / skippable | 83 / 83 (strong skipping aktif) |
| Parameter tidak stabil | 41 (terutama `LocalDate`, `List<…>`, UiState yang memuatnya) |

## Sesudah (Fase C–D)

Pengukuran HP dilakukan **A/B bergantian** (build Fase A vs build akhir, dipasang ulang dan diisi 30.000 transaksi
yang sama) pada sesi yang sama. HP sedang panas (Thermal Status 3, kulit ±43°C) di kedua putaran, jadi angka absolutnya
lebih lambat dari baseline yang diukur saat dingin; yang dibandingkan adalah selisihnya.

### Ringkasan

| Metrik | Sebelum | Sesudah | Keterangan |
|---|---|---|---|
| APK release | 45,74 MB | 6,23 MB | R8 + shrinkResources + locale in/en + font subset |
| Font Inter | 876 KB | 160 KB | subset Latin + tanda baca, sumbu bobot 400–700 tetap variabel |
| Build bersih debug | 149 s | 16 s (build cache terisi), 252 s (cache kosong) | parallel + build cache + configuration cache; build tanpa perubahan 4 s |
| Hitung ulang Beranda+Insight, 30.000 transaksi (debug) | 598 ms | 152 ms | kueri SQL teragregasi/berjendela; rincian: jendela 60 hari 71 ms, total bulan 9 ms, per kategori 13 ms |
| Start dingin, frame pertama (verify) | 470 ms | 479 ms | tidak berubah (dalam noise) |
| Start dingin, **siap dipakai** (verify) | 1.456 ms | 690 ms | Beranda tidak lagi memuat seluruh riwayat |
| Start dingin, frame pertama (speed-profile) | 498 ms | 417 ms | baseline profile kode app + R8 |
| Start dingin, siap dipakai (speed-profile) | 1.430 ms | 640 ms | |
| Scroll Beranda: jank / p50 / p90 / p99 | 36,6% / 21 / 38 / 48 ms | 24,0% / 19 / 28 / 36 ms | kaca di dalam layar tanpa rekaman latar & efek |
| Scroll Pengaturan: jank / p50 / p90 / p99 | 54,2% / 34 / 40 / 44 ms | 40,3% / 22 / 26 / 28 ms | |
| Geser navbar: jank / p50 / p90 / p99 | 9,1% / 15 / 25 / 101 ms | 10,2% / 15 / 23 / 93 ms | navbar sengaja tetap kaca riil (tidak berubah) |
| Parameter Compose tidak stabil | 41 | 9 (semuanya parameter default `viewModel`) | `compose_stability.conf` (java.time, List, Map, Set) |
| Tarik sinkron selama app terbuka (masuk akun) | tiap 60 s (±60×/jam) | saat app ke depan, min. jeda 3 menit, + saat jaringan kembali bila gagal | unggah tetap otomatis setiap ada perubahan; tidak ada kerja jaringan di latar |

Mode hemat (diukur saat HP dingin, Thermal Status 0, build bench): scroll Beranda **1,9%** jank (p50 10 ms),
scroll Pengaturan **2,7%** jank (p50 10 ms). Biaya yang tersisa di mode normal adalah kaca riil navbar, FAB, dan blur
status bar yang membiaskan isi layar yang sedang bergerak; itu inti tampilan Liquid Glass, jadi hanya dilepas di Mode
hemat.

### Kaca: apa yang berubah dan buktinya

- Kaca di dalam layar (kartu, tombol, toggle, segmen) dulu merekam latar ke layer lalu menjalankan blur + lensa +
  vibrancy **setiap frame** saat discroll, padahal latarnya hitam/terang polos atau vinyet halus — efeknya tidak
  mengubah piksel. Sekarang memakai `BackgroundBackdrop` (`PlainBackdrop`): latar digambar langsung di posisi kaca,
  efek dilewati. Highlight tepi, bayangan, warna permukaan, dan animasi tekan tetap.
- Bukti visual: screenshot Beranda (data sama, tema gelap) sebelum vs sesudah berbeda di **0,03% piksel**, seluruhnya
  di label "Uang masuk lagi" yang memang diganti di Fase B. Kaca tidak berubah sama sekali.
- Kaca riil yang tersisa per layar: navbar (3 lapis), FAB, blur status bar, plus toast/sheet/dialog saat tampil.
- Mode hemat (Pengaturan → Tampilan, otomatis saat Penghemat Baterai): semua kaca tanpa blur/lensa, tanpa blur status
  bar, dan isi layar tidak direkam. Ini perubahan visual yang terlihat, karena itu opsional.

### Build

| Ukuran | Sebelum | Sesudah |
|---|---|---|
| APK release (belum ditandatangani) | 45,74 MB (tanpa R8) | 6,23 MB |
| APK debug | 58,98 MB | 58,25 MB (debug tidak di-minify) |
| `testDebugUnitTest` / androidTest | 34 / 16 tes | 35 / 41 tes, semua hijau |
| `lintDebug` | 2 error, 21 peringatan | 0 error, 17 peringatan (sisanya: versi library lebih baru yang sengaja tidak dinaikkan, urutan parameter Modifier dari kode repo Kyant0, ikon aplikasi belum ada) |
| APK bench (release + pengisi data, ditandatangani debug) | 45,77 MB | 6,21 MB |

### Yang dicoba tetapi tidak dipertahankan (tidak ada manfaat terukur)

| Percobaan | Hasil | Keputusan |
|---|---|---|
| Inisialisasi Firebase malas untuk tamu (`FirebaseInitProvider` dimatikan) | frame pertama 468–481 ms vs 470 ms, siap 677–708 ms vs 692 ms | dibatalkan: biaya init Firebase di HP ini dalam noise |
| WorkManager on-demand (tanpa `androidx.startup` initializer) | frame pertama verify 479 vs 470 ms (sama) | dibatalkan |
| Ganti WorkManager → AlarmManager | tidak dicoba: initializer-nya terbukti tidak berbiaya terukur; WorkManager juga mengurus penjadwalan ulang setelah reboot | tetap WorkManager |
| Ganti Material3 (hanya Text, DatePicker, SwipeToDismissBox) | R8 sudah membuang kode Material3 yang tidak dipakai; menulis ulang DatePicker/SwipeToDismissBox berisiko lebih besar dari manfaatnya | tetap |
| Modul macrobenchmark + plugin baselineprofile | tidak dipasang: tes terhubung menghapus paket target setelah selesai (di HP harian) dan google-services menolak paket bench yang berbeda | baseline profile ditulis manual (`app/src/main/baseline-prof.txt`, pola wildcard untuk kode app; profil library ikut otomatis dari AAR) |

### Cara membuat ulang font subset

```bash
pip install fonttools
python -m fontTools.varLib.instancer Inter-Variable.ttf wght=400:700 opsz=drop -o limited.ttf
python -m fontTools.subset limited.ttf --layout-features='*' --output-file=inter_variable.ttf \
  --unicodes="U+0020-007E,U+00A0-00FF,U+0100-017F,U+2010-2027,U+2030-203A,U+20AC,U+2122,U+2190-2193,U+2212,U+00D7,U+00F7,U+25B2,U+25BC,U+25CF,U+2022"
```

### Belum diukur / perlu diuji sendiri di Realme 13+

- Frame setelah HP dingin penuh (Thermal Status 0) untuk build akhir mode normal, dan scroll Semua Transaksi setelah
  daftar dibuat per baris (baseline 2,8% jank).
- Konsumsi baterai/data sinkron untuk akun yang masuk (HP ini memakai mode tamu dan database Firestore belum dibuat).
- Profil GPU dengan Perfetto (yang dipakai di sini `dumpsys gfxinfo`).
