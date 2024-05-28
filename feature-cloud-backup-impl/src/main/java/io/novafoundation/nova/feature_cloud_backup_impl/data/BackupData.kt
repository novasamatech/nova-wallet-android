package io.novafoundation.nova.feature_cloud_backup_impl.data

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

@JvmInline
value class UnencryptedPrivateData(val unencryptedData: String)

@JvmInline
value class EncryptedPrivateData(val encryptedData: ByteArray)

class SerializedBackup<PRIVATE>(
    val publicData: CloudBackup.PublicData,
    val privateData: PRIVATE
)

@JvmInline
value class ReadyForStorageBackup(val value: String)
