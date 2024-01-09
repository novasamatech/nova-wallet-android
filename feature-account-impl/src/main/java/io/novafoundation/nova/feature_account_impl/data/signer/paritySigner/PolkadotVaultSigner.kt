package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.signer.SeparateFlowSigner
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class PolkadotVaultVariantSignerFactory(
    private val signingSharedState: SigningSharedState,
    private val signFlowRequester: PolkadotVaultVariantSignCommunicator,
    private val resourceManager: ResourceManager,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) {

    fun createPolkadotVault(metaAccount: MetaAccount): PolkadotVaultSigner {
        return PolkadotVaultSigner(
            signingSharedState = signingSharedState,
            metaAccount = metaAccount,
            signFlowRequester = signFlowRequester,
            resourceManager = resourceManager,
            polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
            messageSigningNotSupported = messageSigningNotSupported
        )
    }

    fun createParitySigner(metaAccount: MetaAccount): ParitySignerSigner {
        return ParitySignerSigner(
            signingSharedState = signingSharedState,
            metaAccount = metaAccount,
            signFlowRequester = signFlowRequester,
            resourceManager = resourceManager,
            polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
            messageSigningNotSupported = messageSigningNotSupported
        )
    }
}

abstract class PolkadotVaultVariantSigner(
    signingSharedState: SigningSharedState,
    metaAccount: MetaAccount,
    private val signFlowRequester: PolkadotVaultVariantSignCommunicator,
    private val resourceManager: ResourceManager,
    private val variant: PolkadotVaultVariant,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) : SeparateFlowSigner(signingSharedState, signFlowRequester, metaAccount) {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        signFlowRequester.setUsedVariant(variant)

        return super.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        val config = polkadotVaultVariantConfigProvider.variantConfigFor(variant)

        messageSigningNotSupported.presentSigningNotSupported(
            SigningNotSupportedPresentable.Payload(
                iconRes = config.common.iconRes,
                message = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_not_supported_subtitle, variant)
            )
        )

        throw SigningCancelledException()
    }
}

class ParitySignerSigner(
    signingSharedState: SigningSharedState,
    metaAccount: MetaAccount,
    signFlowRequester: PolkadotVaultVariantSignCommunicator,
    resourceManager: ResourceManager,
    polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    messageSigningNotSupported: SigningNotSupportedPresentable,
) : PolkadotVaultVariantSigner(
    signingSharedState = signingSharedState,
    metaAccount = metaAccount,
    signFlowRequester = signFlowRequester,
    resourceManager = resourceManager,
    variant = PolkadotVaultVariant.PARITY_SIGNER,
    polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
    messageSigningNotSupported = messageSigningNotSupported
)

class PolkadotVaultSigner(
    signingSharedState: SigningSharedState,
    metaAccount: MetaAccount,
    signFlowRequester: PolkadotVaultVariantSignCommunicator,
    resourceManager: ResourceManager,
    polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    messageSigningNotSupported: SigningNotSupportedPresentable
) : PolkadotVaultVariantSigner(
    signingSharedState = signingSharedState,
    metaAccount = metaAccount,
    signFlowRequester = signFlowRequester,
    resourceManager = resourceManager,
    variant = PolkadotVaultVariant.POLKADOT_VAULT,
    polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
    messageSigningNotSupported = messageSigningNotSupported
)
