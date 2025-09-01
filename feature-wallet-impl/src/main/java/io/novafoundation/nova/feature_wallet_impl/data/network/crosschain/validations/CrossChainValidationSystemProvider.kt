package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainValidationSystemProvider
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.doNotCrossExistentialDepositInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInCommissionAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notPhishingRecipient
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.recipientIsNotSystemAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import javax.inject.Inject

@FeatureScope
class RealCrossChainValidationSystemProvider @Inject constructor(
    private val phishingValidationFactory: PhishingValidationFactory,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val dryRunSucceedsValidationFactory: DryRunSucceedsValidationFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
) : CrossChainValidationSystemProvider {

    override fun createValidationSystem(): AssetTransfersValidationSystem = ValidationSystem {
        positiveAmount()
        recipientIsNotSystemAccount()

        validAddress()
        notPhishingRecipient(phishingValidationFactory)

        notDeadRecipientInCommissionAsset(assetSourceRegistry)
        notDeadRecipientInUsedAsset(assetSourceRegistry)

        sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

        sufficientTransferableBalanceToPayOriginFee()
        canPayCrossChainFee()

        cannotDropBelowEdBeforePayingDeliveryFee(assetSourceRegistry)

        doNotCrossExistentialDepositInUsedAsset(
            assetSourceRegistry = assetSourceRegistry,
            extraAmount = { it.transfer.amount + it.crossChainFee?.decimalAmount.orZero() }
        )

        dryRunSucceedsValidationFactory.dryRunSucceeds()
    }
}
