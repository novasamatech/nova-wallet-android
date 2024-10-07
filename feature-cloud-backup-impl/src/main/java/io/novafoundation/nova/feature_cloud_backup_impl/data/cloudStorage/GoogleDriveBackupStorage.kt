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
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.requireActivity
import io.novafoundation.nova.common.utils.InformationSize
import io.novafoundation.nova.common.utils.InformationSize.Companion.bytes
import io.novafoundation.nova.common.utils.mapErrorNotInstance
import io.novafoundation.nova.common.utils.systemCall.SystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_impl.BuildConfig
import io.novafoundation.nova.feature_cloud_backup_impl.data.ReadyForStorageBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal class GoogleDriveBackupStorage(
    private val contextManager: ContextManager,
    private val systemCallExecutor: SystemCallExecutor,
    private val oauthClientId: String,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
    private val debug: Boolean = BuildConfig.DEBUG
) : CloudBackupStorage {

    companion object {

        private const val BACKUP_MIME_TYPE = "application/json"
    }

    private val drive: Drive by lazy {
        createGoogleDriveService()
    }

    override suspend fun hasEnoughFreeStorage(neededSize: InformationSize): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatchingRecoveringAuthErrors {
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
        runCatchingRecoveringAuthErrors {
            checkBackupExistsUnsafe()
        }
    }

    override suspend fun writeBackup(backup: ReadyForStorageBackup): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingRecoveringAuthErrors {
            writeBackupFileToDrive(backup.value)
        }
    }

    override suspend fun fetchBackup(): Result<ReadyForStorageBackup> = withContext(Dispatchers.IO) {
        runCatchingRecoveringAuthErrors {
            val fileContent = readBackupFileFromDrive()

            ReadyForStorageBackup(fileContent)
        }.mapErrorNotInstance<_, FetchBackupError> {
            when (it) {
                is UserRecoverableAuthException,
                is UserRecoverableAuthIOException -> FetchBackupError.AuthFailed

                else -> it
            }
        }
    }

    override suspend fun deleteBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingRecoveringAuthErrors {
            deleteBackupFileFromDrive()
        }
    }

    private suspend fun <T> runCatchingRecoveringAuthErrors(action: suspend () -> T): Result<T> {
        return runCatching { action() }
            .recoverCatching {
                when (it) {
                    is UserRecoverableAuthException -> it.askForConsent()
                    is UserRecoverableAuthIOException -> it.cause?.askForConsent()
                    else -> throw it
                }

                action()
            }
    }

    private fun writeBackupFileToDrive(fileContent: String) {
        val contentStream = ByteArrayContent(BACKUP_MIME_TYPE, fileContent.encodeToByteArray())

        val backupInCloud = getBackupFileFromCloud()

        if (backupInCloud != null) {
            drive.files()
                .update(backupInCloud.id, null, contentStream)
                .execute()
        } else {
            val fileMetadata = File().apply { name = backupFileName() }

            drive.files().create(fileMetadata, contentStream)
                .execute()
        }
    }

    private fun readBackupFileFromDrive(): String {
        val outputStream = ByteArrayOutputStream()

        val backupFile = getBackupFileFromCloud() ?: throw FetchBackupError.BackupNotFound

        try {
            drive.files()
                .get(backupFile.id)
                .executeMediaAndDownloadTo(outputStream)
        } catch (e: HttpResponseException) {
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/416
            // Not handle 416 error, to handle it as corrupted backup
            if (e.statusCode != 416) {
                throw e
            }
        }

        return outputStream.toString()
    }

    private fun deleteBackupFileFromDrive() {
        val backupFile = getBackupFileFromCloud() ?: return

        drive.files()
            .delete(backupFile.id)
            .execute()
    }

    private fun checkBackupExistsUnsafe(): Boolean {
        return getBackupFileFromCloud() != null
    }

    private fun getBackupFileFromCloud(): File? {
        return drive.files().list()
            .setQ(backupNameQuery())
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
            .files
            .firstOrNull()
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
        return "name = '" + backupFileName().replace("'", "\\'") + "' and trashed = false"
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

    private fun driveScope(): String = DriveScopes.DRIVE_FILE

    private fun backupFileName(): String {
        return if (debug) {
            "novawallet_backup_debug.json"
        } else {
            "novawallet_backup.json"
        }
    }
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
