# Aturan R8 khusus ItungEn.
#
# Library yang dipakai (Room, Firebase Auth/Firestore, Credential Manager + Google ID, kotlinx.coroutines, Compose,
# Backdrop) sudah membawa aturan keep masing-masing (consumer rules). Kode ItungEn tidak memakai refleksi:
# dokumen Firestore dipetakan manual lewat Map (SyncMapping.kt), JSON cadangan lewat org.json, dan enum lewat
# valueOf yang dipahami R8. Receiver/Activity di manifest dipertahankan otomatis.
#
# Bila build rilis gagal saat dijalankan, periksa missing_rules.txt di app/build/outputs/mapping/release/.

# Baris & nama berkas tetap di stack trace agar laporan crash dari rilis bisa dibaca (dengan mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
