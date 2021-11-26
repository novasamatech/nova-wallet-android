package io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion

import io.novafoundation.nova.common.utils.input.Input

class AdvancedEncryptionValidationPayload(
    val substrateDerivationPathInput: Input<String>,
    val ethereumDerivationPathInput: Input<String>,
)
