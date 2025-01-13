package io.novafoundation.nova.feature_account_api.presenatation.account.common.model

import android.os.Parcelable
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import kotlinx.parcelize.Parcelize

@Parcelize
class AdvancedEncryptionModel(
    val substrateCryptoType: CryptoType?,
    val substrateDerivationPath: String?,
    val ethereumCryptoType: CryptoType?,
    val ethereumDerivationPath: String?
) : Parcelable

fun AdvancedEncryptionModel.toAdvancedEncryption(): AdvancedEncryption {
    return AdvancedEncryption(
        substrateCryptoType,
        ethereumCryptoType,
        derivationPaths = AdvancedEncryption.DerivationPaths(
            substrateDerivationPath,
            ethereumDerivationPath
        )
    )
}

fun AdvancedEncryption.toAdvancedEncryptionModel(): AdvancedEncryptionModel {
    return AdvancedEncryptionModel(
        substrateCryptoType,
        derivationPaths.substrate,
        ethereumCryptoType,
        derivationPaths.ethereum
    )
}
