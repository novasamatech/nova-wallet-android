package io.novafoundation.nova.feature_cloud_backup_impl.data

@JvmInline
value class UnencryptedBackupData(val decryptedData: String)

@JvmInline
value class EncryptedBackupData(val encryptedData: ByteArray)
