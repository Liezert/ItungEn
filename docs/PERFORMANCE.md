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
