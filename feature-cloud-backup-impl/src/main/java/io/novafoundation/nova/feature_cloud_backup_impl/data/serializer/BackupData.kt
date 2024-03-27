package io.novafoundation.nova.feature_cloud_backup_impl.data.serializer

@JvmInline
value class UnencryptedBackupData(val data: String)

@JvmInline
value class EncryptedBackupData(val encryptedData: String)
