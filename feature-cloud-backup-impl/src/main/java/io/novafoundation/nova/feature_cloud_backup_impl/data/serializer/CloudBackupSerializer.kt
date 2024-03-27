package io.novafoundation.nova.feature_cloud_backup_impl.data.serializer

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.InformationSize
import io.novafoundation.nova.common.utils.InformationSize.Companion.megabytes
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

interface CloudBackupSerializer {

    suspend fun neededSizeForBackup(): InformationSize

    suspend fun serializeBackup(backup: CloudBackup): Result<UnencryptedBackupData>

    suspend fun deserializeBackup(backup: UnencryptedBackupData): Result<CloudBackup>
}

internal class JsonCloudBackupSerializer(
    private val gson: Gson,
): CloudBackupSerializer {

    companion object {

        private val neededSizeForBackup: InformationSize = 10.megabytes
    }

    override suspend fun neededSizeForBackup(): InformationSize {
        return neededSizeForBackup
    }

    override suspend fun serializeBackup(backup: CloudBackup): Result<UnencryptedBackupData> {
        TODO("Not yet implemented")
    }

    override suspend fun deserializeBackup(backup: UnencryptedBackupData): Result<CloudBackup> {
        TODO("Not yet implemented")
    }
}
