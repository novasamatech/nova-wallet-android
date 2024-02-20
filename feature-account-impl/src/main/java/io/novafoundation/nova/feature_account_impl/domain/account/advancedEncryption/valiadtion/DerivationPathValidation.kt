package io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion

import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.fold
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.SubstrateJunctionDecoder

sealed class DerivationPathValidation(
    private val junctionDecoder: JunctionDecoder,
    private val valueExtractor: (AdvancedEncryptionValidationPayload) -> Input<String>,
    private val failure: AdvancedEncryptionValidationFailure
) : AdvancedEncryptionValidation {

    override suspend fun validate(value: AdvancedEncryptionValidationPayload): ValidationStatus<AdvancedEncryptionValidationFailure> {
        val isValid = valueExtractor(value)
            .fold(
                ifEnabled = { it.isEmpty() || checkCanDecode(it) },
                ifDisabled = true
            )

        return validOrError(isValid) { failure }
    }

    private fun checkCanDecode(derivationPath: String): Boolean = runCatching { junctionDecoder.decode(derivationPath) }.isSuccess
}

class SubstrateDerivationPathValidation : DerivationPathValidation(
    junctionDecoder = SubstrateJunctionDecoder,
    valueExtractor = AdvancedEncryptionValidationPayload::substrateDerivationPathInput,
    failure = AdvancedEncryptionValidationFailure.SUBSTRATE_DERIVATION_PATH
)

class EthereumDerivationPathValidation : DerivationPathValidation(
    junctionDecoder = BIP32JunctionDecoder,
    valueExtractor = AdvancedEncryptionValidationPayload::ethereumDerivationPathInput,
    failure = AdvancedEncryptionValidationFailure.ETHEREUM_DERIVATION_PATH
)
