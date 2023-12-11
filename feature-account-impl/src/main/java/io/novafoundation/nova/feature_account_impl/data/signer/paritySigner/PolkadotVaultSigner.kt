package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.MutableSharedState
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

abstract class PolkadotVaultVariantSigner(
    signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    private val signFlowRequester: PolkadotVaultVariantSignCommunicator,
    private val resourceManager: ResourceManager,
    private val variant: PolkadotVaultVariant,
    private val polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    private val messageSigningNotSupported: SigningNotSupportedPresentable
) : SeparateFlowSigner(signingSharedState, signFlowRequester) {

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
    signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    signFlowRequester: PolkadotVaultVariantSignCommunicator,
    resourceManager: ResourceManager,
    polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    messageSigningNotSupported: SigningNotSupportedPresentable
) : PolkadotVaultVariantSigner(
    signingSharedState = signingSharedState,
    signFlowRequester = signFlowRequester,
    resourceManager = resourceManager,
    variant = PolkadotVaultVariant.PARITY_SIGNER,
    polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
    messageSigningNotSupported = messageSigningNotSupported
)

class PolkadotVaultSigner(
    signingSharedState: MutableSharedState<SignerPayloadExtrinsic>,
    signFlowRequester: PolkadotVaultVariantSignCommunicator,
    resourceManager: ResourceManager,
    polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    messageSigningNotSupported: SigningNotSupportedPresentable
) : PolkadotVaultVariantSigner(
    signingSharedState = signingSharedState,
    signFlowRequester = signFlowRequester,
    resourceManager = resourceManager,
    variant = PolkadotVaultVariant.POLKADOT_VAULT,
    polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
    messageSigningNotSupported = messageSigningNotSupported
)
