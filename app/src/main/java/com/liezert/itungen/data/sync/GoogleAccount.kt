package com.liezert.itungen.data.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AccountUser(val uid: String, val name: String?, val email: String?)

/** Firebase belum dikonfigurasi (google-services.json belum ada) atau Google Sign-In belum diaktifkan. */
class AccountUnavailableException : Exception()

/** Akun Firebase Auth: login Google lewat Credential Manager (ID token-nya ditukar ke Firebase) atau email + kata sandi. */
class GoogleAccount(private val context: Context, private val auth: FirebaseAuth?) {

    // Dibaca saat runtime, bukan R.string: resource ini hanya ada bila plugin google-services aktif.
    @SuppressLint("DiscouragedApi")
    private val webClientId: String? = context.resources
        .getIdentifier("default_web_client_id", "string", context.packageName)
        .takeIf { it != 0 }
        ?.let(context::getString)

    val available: Boolean get() = auth != null && webClientId != null

    private val _user = MutableStateFlow(auth?.currentUser?.toUser())
    val user: StateFlow<AccountUser?> = _user.asStateFlow()

    init {
        auth?.addAuthStateListener { _user.value = it.currentUser?.toUser() }
    }

    /** [activityContext] harus Activity: Credential Manager menampilkan pemilih akun di atasnya. */
    suspend fun signIn(activityContext: Context) {
        val auth = auth ?: throw AccountUnavailableException()
        val clientId = webClientId ?: throw AccountUnavailableException()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Kredensial bukan akun Google"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    /** Email + kata sandi: [create] true = daftar akun baru, false = masuk ke akun yang sudah ada. */
    suspend fun signInWithEmail(email: String, password: String, create: Boolean) {
        val auth = auth ?: throw AccountUnavailableException()
        if (create) auth.createUserWithEmailAndPassword(email, password).await()
        else auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signOut() {
        auth?.signOut()
        // Agar pemilih akun muncul lagi saat masuk berikutnya, bukan langsung memakai akun terakhir.
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}

private fun FirebaseUser.toUser() = AccountUser(uid, displayName, email)
