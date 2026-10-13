package com.liezert.itungen.ui.account

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.liezert.itungen.AppContainer
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.data.sync.AccountUnavailableException
import com.liezert.itungen.data.sync.AccountUser
import com.liezert.itungen.data.sync.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val user: AccountUser? = null,
    val sync: SyncStatus = SyncStatus.Idle,
    /** Perubahan lokal yang belum terunggah. */
    val pending: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Dipakai layar Masuk dan kartu akun di Profil. */
class AccountViewModel(private val container: AppContainer) : ViewModel() {
    private val local = MutableStateFlow(AccountUiState())

    val uiState: StateFlow<AccountUiState> = combine(
        container.account.user,
        container.sync.status,
        container.pendingChanges,
        local,
    ) { user, sync, pending, l -> l.copy(user = user, sync = sync, pending = pending) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState(user = container.account.user.value))

    /** [activityContext] = LocalContext Activity, untuk pemilih akun Google. */
    fun signIn(activityContext: Context) = run {
        try {
            container.account.signIn(activityContext)
            onSignedIn()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GetCredentialCancellationException) {
            null // pengguna menutup pemilih akun
        } catch (_: NoCredentialException) {
            "Belum ada akun Google di HP ini. Tambahkan lewat Pengaturan."
        } catch (_: AccountUnavailableException) {
            "Login Google belum dikonfigurasi di aplikasi ini."
        } catch (_: Exception) {
            "Gagal masuk. Periksa koneksi internet lalu coba lagi."
        }
    }

    /** [create] true = daftar akun baru. */
    fun signInWithEmail(email: String, password: String, create: Boolean) = run {
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) return@run "Isi email dan kata sandi."
        try {
            container.account.signInWithEmail(address, password, create)
            onSignedIn()
        } catch (e: CancellationException) {
            throw e
        } catch (_: AccountUnavailableException) {
            "Login belum dikonfigurasi di aplikasi ini."
        } catch (_: FirebaseAuthWeakPasswordException) {
            "Kata sandi terlalu lemah. Gunakan minimal 6 karakter."
        } catch (_: FirebaseAuthUserCollisionException) {
            "Email ini sudah terdaftar. Silakan masuk."
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            if (create) "Format email tidak valid." else "Email atau kata sandi salah."
        } catch (_: FirebaseAuthInvalidUserException) {
            "Email atau kata sandi salah."
        } catch (_: FirebaseNetworkException) {
            "Tidak ada koneksi internet. Coba lagi."
        } catch (_: Exception) {
            "Gagal masuk. Coba lagi nanti."
        }
    }

    private fun onSignedIn(): String? {
        container.onboarded = true
        // Data tamu di HP ini ikut terunggah, lalu data dari HP lain ditarik. Di scope aplikasi:
        // layar Masuk langsung ditutup setelah login, jadi viewModelScope-nya ikut dibatalkan.
        container.syncInBackground()
        return null
    }

    fun continueAsGuest() {
        container.onboarded = true
    }

    fun syncNow() = run {
        if (container.sync.sync()) null else "Gagal sinkron. Periksa koneksi internet lalu coba lagi."
    }

    fun signOut() = run {
        if (container.sync.signOut()) null
        else "Masih ada catatan yang belum terunggah. Sambungkan internet dulu agar tidak hilang."
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    /** Menjalankan aksi akun satu per satu; teks yang dikembalikan tampil sebagai pesan. */
    private fun run(action: suspend () -> String?) {
        if (local.value.busy) return
        viewModelScope.launch {
            local.update { it.copy(busy = true, message = null) }
            val message = action()
            local.update { it.copy(busy = false, message = message) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AccountViewModel((this[APPLICATION_KEY] as ItungEnApp).container) }
        }
    }
}
