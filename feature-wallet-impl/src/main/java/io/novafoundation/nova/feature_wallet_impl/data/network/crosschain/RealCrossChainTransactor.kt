package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.transformResult
import io.novafoundation.nova.common.utils.wrapInResult
import io.novafoundation.nova.common.utils.xTokensName
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.events.tryDetectDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.domain.implementations.accountIdToMultiLocation
import io.novafoundation.nova.feature_wallet_api.domain.implementations.plus
import io.novafoundation.nova.feature_wallet_api.domain.model.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.XcmTransferType
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
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations.canPayCrossChainFee
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations.cannotDropBelowEdBeforePayingDeliveryFee
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findRelayChainOrThrow
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.getInherentEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.hasEvent
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeout
import java.math.BigInteger
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

class RealCrossChainTransactor(
    private val weigher: CrossChainWeigher,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val phishingValidationFactory: PhishingValidationFactory,
    private val palletXcmRepository: PalletXcmRepository,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val eventsRepository: EventsRepository,
    private val chainStateRepository: ChainStateRepository,
    private val chainRegistry: ChainRegistry,
) : CrossChainTransactor {

    override val validationSystem: AssetTransfersValidationSystem = ValidationSystem {
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
    }

    override suspend fun ExtrinsicService.estimateOriginFee(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase
    ): Fee {
        return estimateFee(
            chain = transfer.originChain,
            origin = TransactionOrigin.SelectedWallet,
            submissionOptions = ExtrinsicService.SubmissionOptions(
                feePaymentCurrency = transfer.feePaymentCurrency
            )
        ) {
            crossChainTransfer(configuration, transfer, crossChainFee = Balance.ZERO)
        }
    }

    override suspend fun ExtrinsicService.performTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): Result<ExtrinsicSubmission> {
        return submitExtrinsic(
            chain = transfer.originChain,
            origin = TransactionOrigin.SelectedWallet,
            submissionOptions = ExtrinsicService.SubmissionOptions(
                feePaymentCurrency = transfer.feePaymentCurrency
            )
        ) {
            crossChainTransfer(configuration, transfer, crossChainFee)
        }
    }

    override suspend fun requiredRemainingAmountAfterTransfer(sendingAsset: Chain.Asset, originChain: Chain): Balance {
        return assetSourceRegistry.sourceFor(sendingAsset).balance.existentialDeposit(originChain, sendingAsset)
    }

    context(ExtrinsicService)
    override suspend fun performAndTrackTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
    ): Result<Balance> {
        // Start balances updates eagerly to not to miss events in case tx has been included to block right after submission
        val balancesUpdates = observeTransferableBalance(transfer)
            .wrapInResult()
            .shareIn(CoroutineScope(coroutineContext), SharingStarted.Eagerly, replay = 100)

        Log.d("CrossChain", "Starting cross-chain transfer")

        return performTransferOfExactAmount(configuration, transfer)
            .requireOk()
            .flatMap {
                Log.d("CrossChain", "Cross chain transfer for successfully executed on origin, waiting for destination")

                balancesUpdates.awaitCrossChainArrival(transfer)
            }
    }

    override suspend fun estimateMaximumExecutionTime(configuration: CrossChainTransferConfiguration): Duration {
        val originChainId = configuration.originChainId
        val reserveChainId = configuration.reserveFee?.to?.chainId
        val destinationChainId = configuration.destinationFee.to.chainId

        val relayId = chainRegistry.findRelayChainOrThrow(originChainId)

        var totalDuration = ZERO

        if (reserveChainId != null) {
            totalDuration += maxTimeToTransmitMessage(originChainId, reserveChainId, relayId)
            totalDuration += maxTimeToTransmitMessage(reserveChainId, destinationChainId, relayId)
        } else {
            totalDuration += maxTimeToTransmitMessage(originChainId, destinationChainId, relayId)
        }

        return totalDuration
    }

    private suspend fun maxTimeToTransmitMessage(from: ChainId, to: ChainId, relay: ChainId): Duration {
        val toProduceBlockOnOrigin = chainStateRepository.expectedBlockTime(from)
        val toProduceBlockOnDestination = chainStateRepository.expectedBlockTime(to)
        val toProduceBlockOnRelay = if (from != relay && to != relay) chainStateRepository.expectedBlockTime(relay) else ZERO

        return toProduceBlockOnOrigin + toProduceBlockOnRelay + toProduceBlockOnDestination
    }

    private suspend fun Flow<Result<TransferableBalanceUpdate>>.awaitCrossChainArrival(transfer: AssetTransferBase): Result<Balance> {
        return runCatching {
            withTimeout(60.seconds) {
                transformResult { balanceUpdate ->
                    Log.d("CrossChain", "Destination balance update detected: $balanceUpdate")

                    val updatedAt = balanceUpdate.updatedAt

                    if (updatedAt == null) {
                        Log.w("CrossChain", "Update block hash was not present, maybe wrong datasource is used?")
                        return@transformResult
                    }

                    val inherentEvents = eventsRepository.getInherentEvents(transfer.destinationChain.id, updatedAt)

                    val xcmArrivedDeposit = searchForXcmArrival(inherentEvents.initialization, transfer)
                        ?: searchForXcmArrival(inherentEvents.finalization, transfer)

                    if (xcmArrivedDeposit != null) {
                        Log.d("CrossChain", "Found destination xcm arrival event, amount is $xcmArrivedDeposit")

                        emit(xcmArrivedDeposit)
                    } else {
                        Log.d("CrossChain", "No destination xcm arrival event found for the received balance update")
                    }
                }
                    .first()
                    .getOrThrow()
            }
        }
    }

    private suspend fun searchForXcmArrival(
        events: List<GenericEvent.Instance>,
        transfer: AssetTransferBase
    ): Balance? {
        if (!events.hasEvent("MessageQueue", "Processed")) return null

        val eventDetector = assetSourceRegistry.getEventDetector(transfer.destinationChainAsset)

        val depositEvent = events.mapNotNull { event -> eventDetector.tryDetectDeposit(event) }
            .find { it.destination.contentEquals(transfer.recipientAccountId) }

        return depositEvent?.amount
    }

    private suspend fun ExtrinsicService.performTransferOfExactAmount(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
    ): Result<ExtrinsicExecutionResult> {
        return submitExtrinsicAndAwaitExecution(
            chain = transfer.originChain,
            origin = TransactionOrigin.SelectedWallet,
            submissionOptions = ExtrinsicService.SubmissionOptions(
                feePaymentCurrency = transfer.feePaymentCurrency
            )
        ) {
            // We are transferring the exact amount, so we should nothing on top of the transfer amount
            crossChainTransfer(configuration, transfer, crossChainFee = Balance.ZERO)
        }
    }

    private suspend fun observeTransferableBalance(transfer: AssetTransferBase): Flow<TransferableBalanceUpdate> {
        val destinationAssetBalances = assetSourceRegistry.sourceFor(transfer.destinationChainAsset)

        return destinationAssetBalances.balance.subscribeTransferableAccountBalance(
            chain = transfer.destinationChain,
            chainAsset = transfer.destinationChainAsset,
            accountId = transfer.recipientAccountId,
            sharedSubscriptionBuilder = null
        )
    }

    private suspend fun ExtrinsicBuilder.crossChainTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        when (configuration.transferType) {
            XcmTransferType.X_TOKENS -> xTokensTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET_RESERVE -> xcmPalletReserveTransfer(configuration, transfer, crossChainFee)
            XcmTransferType.XCM_PALLET_TELEPORT -> xcmPalletTeleport(configuration, transfer, crossChainFee)
            XcmTransferType.UNKNOWN -> throw IllegalArgumentException("Unknown transfer type")
        }
    }

    private suspend fun ExtrinsicBuilder.xTokensTransfer(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)
        val fullDestinationLocation = configuration.destinationChainLocation + assetTransfer.beneficiaryLocation()
        val requiredDestWeight = weigher.estimateRequiredDestWeight(configuration)

        val lowestMultiLocationVersion = palletXcmRepository.lowestPresentMultiLocationVersion(assetTransfer.originChain.id)
        val lowestMultiAssetVersion = palletXcmRepository.lowestPresentMultiAssetVersion(assetTransfer.originChain.id)

        call(
            moduleName = runtime.metadata.xTokensName(),
            callName = "transfer_multiasset",
            arguments = mapOf(
                "asset" to multiAsset.versioned(lowestMultiAssetVersion).toEncodableInstance(),
                "dest" to fullDestinationLocation.versioned(lowestMultiLocationVersion).toEncodableInstance(),

                // depending on the version of the pallet, only one of weights arguments going to be encoded
                "dest_weight" to destWeightEncodable(requiredDestWeight),
                "dest_weight_limit" to WeightLimit.Unlimited.toVersionedEncodableInstance(runtime)
            )
        )
    }

    private fun destWeightEncodable(weight: Weight): Any = weight
    private suspend fun ExtrinsicBuilder.xcmPalletReserveTransfer(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransfer(
            configuration = configuration,
            assetTransfer = assetTransfer,
            crossChainFee = crossChainFee,
            callName = "limited_reserve_transfer_assets"
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTeleport(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        xcmPalletTransfer(
            configuration = configuration,
            assetTransfer = assetTransfer,
            crossChainFee = crossChainFee,
            callName = "limited_teleport_assets"
        )
    }

    private suspend fun ExtrinsicBuilder.xcmPalletTransfer(
        configuration: CrossChainTransferConfiguration,
        assetTransfer: AssetTransferBase,
        crossChainFee: Balance,
        callName: String
    ) {
        val lowestMultiLocationVersion = palletXcmRepository.lowestPresentMultiLocationVersion(assetTransfer.originChain.id)
        val lowestMultiAssetsVersion = palletXcmRepository.lowestPresentMultiAssetsVersion(assetTransfer.originChain.id)

        val multiAsset = configuration.multiAssetFor(assetTransfer, crossChainFee)

        call(
            moduleName = runtime.metadata.xcmPalletName(),
            callName = callName,
            arguments = mapOf(
                "dest" to configuration.destinationChainLocation.versioned(lowestMultiLocationVersion).toEncodableInstance(),
                "beneficiary" to assetTransfer.beneficiaryLocation().versioned(lowestMultiLocationVersion).toEncodableInstance(),
                "assets" to listOf(multiAsset).versioned(lowestMultiAssetsVersion).toEncodableInstance(),
                "fee_asset_item" to BigInteger.ZERO,
                "weight_limit" to WeightLimit.Unlimited.toVersionedEncodableInstance(runtime)
            )
        )
    }

    private fun CrossChainTransferConfiguration.multiAssetFor(
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ): XcmMultiAsset {
        // we add cross chain fee top of entered amount so received amount will be no less than entered one
        val planks = transfer.amountPlanks + crossChainFee

        return XcmMultiAsset.from(assetLocation, planks)
    }

    private fun AssetTransferBase.beneficiaryLocation(): MultiLocation {
        val accountId = destinationChain.accountIdOrDefault(recipient)

        return accountId.accountIdToMultiLocation()
    }
}
