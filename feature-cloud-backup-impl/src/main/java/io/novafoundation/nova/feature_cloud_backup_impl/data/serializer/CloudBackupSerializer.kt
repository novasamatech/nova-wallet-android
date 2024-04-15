package io.novafoundation.nova.feature_cloud_backup_impl.data.serializer

import com.google.gson.GsonBuilder
import io.novafoundation.nova.common.utils.ByteArrayHexAdapter
import io.novafoundation.nova.common.utils.InformationSize
import io.novafoundation.nova.common.utils.InformationSize.Companion.megabytes
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_impl.data.EncryptedPrivateData
import io.novafoundation.nova.feature_cloud_backup_impl.data.ReadyForStorageBackup
import io.novafoundation.nova.feature_cloud_backup_impl.data.SerializedBackup
import io.novafoundation.nova.feature_cloud_backup_impl.data.UnencryptedPrivateData

interface CloudBackupSerializer {

    suspend fun neededSizeForBackup(): InformationSize

    suspend fun serializePrivateData(backup: CloudBackup): Result<SerializedBackup<UnencryptedPrivateData>>

    suspend fun serializePublicData(backup: SerializedBackup<EncryptedPrivateData>): Result<ReadyForStorageBackup>

    suspend fun deserializePublicData(backup: ReadyForStorageBackup): Result<SerializedBackup<EncryptedPrivateData>>

    suspend fun deserializePrivateData(backup: SerializedBackup<UnencryptedPrivateData>): Result<CloudBackup>
}

internal class JsonCloudBackupSerializer() : CloudBackupSerializer {

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayHexAdapter())
        .create()

    companion object {

        private val neededSizeForBackup: InformationSize = 10.megabytes
    }

    override suspend fun neededSizeForBackup(): InformationSize {
        return neededSizeForBackup
    }

    override suspend fun serializePrivateData(backup: CloudBackup): Result<SerializedBackup<UnencryptedPrivateData>> {
        return runCatching {
            val privateDataSerialized = gson.toJson(backup.privateData)

            SerializedBackup(
                publicData = backup.publicData,
                privateData = UnencryptedPrivateData(privateDataSerialized)
            )
        }
    }

    override suspend fun serializePublicData(backup: SerializedBackup<EncryptedPrivateData>): Result<ReadyForStorageBackup> {
        return runCatching {
            ReadyForStorageBackup(gson.toJson(backup))
        }
    }

    override suspend fun deserializePublicData(backup: ReadyForStorageBackup): Result<SerializedBackup<EncryptedPrivateData>> {
        return runCatching {
            gson.fromJson<SerializedBackup<EncryptedPrivateData>>(backup.value).also {
                // Gson doesn't fail on missing fields so we do some preliminary checks here
                requireNotNull(it.publicData)
                requireNotNull(it.privateData)

                // Do not allow empty backups
                require(it.publicData.wallets.isNotEmpty())
            }
        }
    }

    override suspend fun deserializePrivateData(backup: SerializedBackup<UnencryptedPrivateData>): Result<CloudBackup> {
        return runCatching {
            val privateData: CloudBackup.PrivateData = gson.fromJson<CloudBackup.PrivateData>(backup.privateData.unencryptedData).also {
                // Gson doesn't fail on missing fields so we do some preliminary checks here
                requireNotNull(it.wallets)
            }

            CloudBackup(
                publicData = backup.publicData,
                privateData = privateData
            )
        }
    }
}
