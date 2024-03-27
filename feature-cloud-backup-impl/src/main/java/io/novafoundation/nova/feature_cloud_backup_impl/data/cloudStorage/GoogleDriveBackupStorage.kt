package io.novafoundation.nova.feature_cloud_backup_impl.data.cloudStorage

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.requireActivity
import io.novafoundation.nova.common.utils.InformationSize
import io.novafoundation.nova.common.utils.InformationSize.Companion.bytes
import io.novafoundation.nova.common.utils.systemCall.SystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GoogleDriveBackupStorage(
    private val contextManager: ContextManager,
    private val systemCallExecutor: SystemCallExecutor,
    private val oauthClientId: String,
    private val backupFileName: String = "novawallet_backup",
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
) : CloudBackupStorage {

    private val drive: Drive by lazy {
        createGoogleDriveService()
    }

    override suspend fun hasEnoughFreeStorage(neededSize: InformationSize): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val remainingSpaceInDrive = getRemainingSpace()

            remainingSpaceInDrive >= neededSize
        }
    }

    override suspend fun isCloudStorageServiceAvailable(): Boolean {
        return googleApiAvailabilityProvider.isAvailable()
    }

    override suspend fun isUserAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(contextManager.getApplicationContext())

        account != null
    }

    override suspend fun authenticateUser(): Result<Unit> = withContext(Dispatchers.IO) {
        val systemCall = GoogleSignInSystemCall(contextManager, oauthClientId, driveScope())
        systemCallExecutor.executeSystemCall(systemCall)
    }

    override suspend fun checkBackupExists(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching { checkBackupExistsUnsafe() }
            .recoverCatching {
                when (it) {
                    is UserRecoverableAuthException -> it.askForConsent()
                    is UserRecoverableAuthIOException -> it.cause?.askForConsent()
                    else -> throw it
                }

                checkBackupExistsUnsafe()
            }
    }

    private fun checkBackupExistsUnsafe(): Boolean {
        val result = drive.files().list()
            .setQ(backupNameQuery())
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        return result.files.isNotEmpty()
    }

    private fun getRemainingSpace(): InformationSize {
        val about = drive.about().get().setFields("storageQuota").execute()
        val totalSpace: Long = about.storageQuota.limit
        val usedSpace: Long = about.storageQuota.usage
        val remainingSpace = totalSpace - usedSpace

        return remainingSpace.bytes
    }

    private suspend fun UserRecoverableAuthException.askForConsent() {
        systemCallExecutor.executeSystemCall(RemoteConsentSystemCall(this))
    }

    private fun backupNameQuery(): String {
        return "name = '" + backupFileName.replace("'", "\\'") + "' and trashed = false"
    }

    private fun createGoogleDriveService(): Drive {
        val context = contextManager.getApplicationContext()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(driveScope()))
        credential.selectedAccount = account!!.account

        return Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("Nova Wallet")
            .build()
    }

    private fun driveScope(): String = DriveScopes.DRIVE
}

private class GoogleSignInSystemCall(
    private val contextManager: ContextManager,
    private val oauthClientId: String,
    private val scope: String
) : SystemCall<Unit> {

    companion object {

        private const val REQUEST_CODE = 9001
    }

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(oauthClientId)
            .requestScopes(Scope(scope))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(contextManager.requireActivity(), signInOptions)
        val signInIntent = googleSignInClient.signInIntent

        return SystemCall.Request(
            intent = signInIntent,
            requestCode = REQUEST_CODE
        )
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Unit> {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)

        return try {
            task.getResult(ApiException::class.java)

            Result.success(Unit)
        } catch (e: ApiException) {
            Result.failure(e)
        }
    }
}

private class RemoteConsentSystemCall(
    private val consentException: UserRecoverableAuthException,
) : SystemCall<Unit> {

    companion object {

        private const val REQUEST_CODE = 9002
    }

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val intent = consentException.intent!!

        return SystemCall.Request(intent, REQUEST_CODE)
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Unit> {
        return if (resultCode == RESULT_OK) {
            Result.success(Unit)
        } else {
            Result.failure(consentException)
        }
    }
}
