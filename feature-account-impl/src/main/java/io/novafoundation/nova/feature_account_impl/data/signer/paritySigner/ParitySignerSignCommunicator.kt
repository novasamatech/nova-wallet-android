package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner

import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator

interface PolkadotVaultVariantSignCommunicator: SignInterScreenCommunicator {

    fun setUsedVariant(variant: PolkadotVaultVariant)
}
