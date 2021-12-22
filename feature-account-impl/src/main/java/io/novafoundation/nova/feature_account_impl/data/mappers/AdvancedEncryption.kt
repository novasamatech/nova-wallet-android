package io.novafoundation.nova.feature_account_impl.data.mappers

import io.novafoundation.nova.common.utils.input.valueOrNull
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInput
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator

fun mapAdvancedEncryptionStateToResponse(
    input: AdvancedEncryptionInput
): AdvancedEncryptionCommunicator.Response {
    return with(input) {
        AdvancedEncryptionCommunicator.Response(
            substrateCryptoType = substrateCryptoType.valueOrNull,
            substrateDerivationPath = substrateDerivationPath.valueOrNull,
            ethereumCryptoType = ethereumCryptoType.valueOrNull,
            ethereumDerivationPath = ethereumDerivationPath.valueOrNull
        )
    }
}

fun mapAdvancedEncryptionResponseToAdvancedEncryption(
    advancedEncryptionResponse: AdvancedEncryptionCommunicator.Response
): AdvancedEncryption {
    return with(advancedEncryptionResponse) {
        AdvancedEncryption(
            substrateCryptoType = substrateCryptoType,
            ethereumCryptoType = ethereumCryptoType,
            derivationPaths = AdvancedEncryption.DerivationPaths(
                substrate = substrateDerivationPath,
                ethereum = ethereumDerivationPath
            )
        )
    }
}
