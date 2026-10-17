package com.liezert.itungen.ui.account

import androidx.annotation.StringRes
import com.liezert.itungen.R
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
import com.liezert.itungen.data.demo.seedDemoData
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
    /** Pesan galat/aksi akun (resource); null = tidak ada. */
    @StringRes val message: Int? = null,
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
            R.string.login_no_google_account
        } catch (_: AccountUnavailableException) {
            R.string.login_google_unconfigured
        } catch (_: Exception) {
            R.string.login_failed_network
        }
    }

    /** [create] true = daftar akun baru. */
    fun signInWithEmail(email: String, password: String, create: Boolean) = run {
        val address = email.trim()
        if (address.isEmpty() || password.isEmpty()) return@run R.string.login_fill_fields
        try {
            container.account.signInWithEmail(address, password, create)
            onSignedIn()
        } catch (e: CancellationException) {
            throw e
        } catch (_: AccountUnavailableException) {
            R.string.login_unconfigured
        } catch (_: FirebaseAuthWeakPasswordException) {
            R.string.login_weak_password
        } catch (_: FirebaseAuthUserCollisionException) {
            R.string.login_email_taken
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            if (create) R.string.login_bad_email else R.string.login_wrong_credentials
        } catch (_: FirebaseAuthInvalidUserException) {
            R.string.login_wrong_credentials
        } catch (_: FirebaseNetworkException) {
            R.string.login_offline
        } catch (_: Exception) {
            R.string.login_failed_later
        }
    }

    private fun onSignedIn(): Int? {
        container.onboarded = true
        // Data tamu di HP ini ikut terunggah, lalu data dari HP lain ditarik. Di scope aplikasi:
        // layar Masuk langsung ditutup setelah login, jadi viewModelScope-nya ikut dibatalkan.
        container.syncInBackground()
        return null
    }

    fun continueAsGuest() {
        container.onboarded = true
    }

    /** Tamu + data contoh, untuk mencoba app tanpa mencatat dulu. */
    fun continueWithDemo() {
        container.onboarded = true
        container.appScope.launch { seedDemoData(container) }
    }

    /** "Coba lagi" setelah sinkron gagal; penyebabnya tampil dari status sinkron. */
    fun retrySync() = run {
        container.sync.sync()
        null
    }

    fun signOut() = run {
        if (container.sync.signOut()) null
        else R.string.sign_out_blocked
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    /** Menjalankan aksi akun satu per satu; resource teks yang dikembalikan tampil sebagai pesan. */
    private fun run(action: suspend () -> Int?) {
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
