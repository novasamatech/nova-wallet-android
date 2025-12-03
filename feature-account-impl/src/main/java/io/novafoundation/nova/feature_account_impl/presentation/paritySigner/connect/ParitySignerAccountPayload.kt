package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_impl.domain.utils.ScanSecret
import kotlinx.parcelize.Parcelize

sealed interface ParitySignerAccountPayload : Parcelable {

    val accountId: ByteArray

    val variant: PolkadotVaultVariant

    @Parcelize
    class AsPublic(
        override val accountId: ByteArray,
        override val variant: PolkadotVaultVariant
    ) : ParitySignerAccountPayload

    @Parcelize
    class AsSecret(
        override val accountId: ByteArray,
        override val variant: PolkadotVaultVariant,
        val secret: ScanSecretPayload
    ) : ParitySignerAccountPayload
}

sealed interface ScanSecretPayload : Parcelable {

    val data: ByteArray

    @Parcelize
    class Seed(override val data: ByteArray) : ScanSecretPayload

    @Parcelize
    class EncryptedKey(override val data: ByteArray) : ScanSecretPayload
}

fun ScanSecretPayload.toDomain(): ScanSecret = when (this) {
    is ScanSecretPayload.EncryptedKey -> ScanSecret.EncryptedKeypair(data)
    is ScanSecretPayload.Seed -> ScanSecret.Seed(data)
}

fun ScanSecret.fromDomain(): ScanSecretPayload = when (this) {
    is ScanSecret.EncryptedKeypair -> ScanSecretPayload.EncryptedKey(data)
    is ScanSecret.Seed -> ScanSecretPayload.Seed(data)
}
