package io.novafoundation.nova.feature_account_impl.domain.utils

sealed class ScanSecret(val data: ByteArray) {

    class Seed(data: ByteArray) : ScanSecret(data)

    class EncryptedKeypair(data: ByteArray) : ScanSecret(data)
}
