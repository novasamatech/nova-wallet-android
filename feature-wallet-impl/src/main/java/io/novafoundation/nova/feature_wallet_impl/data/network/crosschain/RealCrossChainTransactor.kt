package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import android.util.Log
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.flatMap
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.transformResult
import io.novafoundation.nova.common.utils.wrapInResult
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
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTrackingTransferResult
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
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
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.DynamicCrossChainTransactor
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.legacy.LegacyCrossChainTransactor
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations.canPayCrossChainFee
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations.cannotDropBelowEdBeforePayingDeliveryFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findRelayChainOrThrow
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.BlockEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.hasEvent
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

class RealCrossChainTransactor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val phishingValidationFactory: PhishingValidationFactory,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val eventsRepository: EventsRepository,
    private val chainStateRepository: ChainStateRepository,
    private val chainRegistry: ChainRegistry,
    private val dynamic: DynamicCrossChainTransactor,
    private val legacy: LegacyCrossChainTransactor
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

    context(ExtrinsicService)
    override suspend fun estimateOriginFee(
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

    context(ExtrinsicService)
    override suspend fun performTransfer(
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
        return assetSourceRegistry.sourceFor(sendingAsset).balance.existentialDeposit(sendingAsset)
    }

    context(ExtrinsicService)
    override suspend fun performAndTrackTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
    ): Result<CrossChainTrackingTransferResult> {
        // Start balances updates eagerly to not to miss events in case tx has been included to block right after submission
        val balancesUpdates = observeTransferableBalance(transfer)
            .wrapInResult()
            .shareIn(CoroutineScope(coroutineContext), SharingStarted.Eagerly, replay = 100)

        Log.d("CrossChain", "Starting cross-chain transfer")

        return performTransferOfExactAmount(configuration, transfer)
            .requireOk()
            .flatMap { executionResult ->
                Log.d("CrossChain", "Cross chain transfer for successfully executed on origin, waiting for destination")

                balancesUpdates.awaitCrossChainArrival(transfer)
                    .map { CrossChainTrackingTransferResult(executionResult, it) }
            }
    }

    override suspend fun estimateMaximumExecutionTime(configuration: CrossChainTransferConfiguration): Duration {
        val originChainId = configuration.originChainId
        val remoteReserveChainId = configuration.remoteReserveChainId
        val destinationChainId = configuration.destinationChainId

        val relayId = chainRegistry.findRelayChainOrThrow(originChainId)

        var totalDuration = ZERO

        if (remoteReserveChainId != null) {
            totalDuration += maxTimeToTransmitMessage(originChainId, remoteReserveChainId, relayId)
            totalDuration += maxTimeToTransmitMessage(remoteReserveChainId, destinationChainId, relayId)
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

                    val blockEvents = eventsRepository.getBlockEvents(transfer.destinationChain.id, updatedAt)

                    val xcmArrivedDeposit = searchForXcmArrival(blockEvents.initialization, transfer)
                        ?: searchForXcmArrival(blockEvents.finalization, transfer)
                        ?: searchForXcmArrival(blockEvents.findSetValidationDataEvents(), transfer)

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

    private fun BlockEvents.findSetValidationDataEvents(): List<GenericEvent.Instance> {
        val setValidationDataExtrinsic = applyExtrinsic.find { it.extrinsic.call.instanceOf(Modules.PARACHAIN_SYSTEM, "set_validation_data") }

        return setValidationDataExtrinsic?.events.orEmpty()
    }

    private suspend fun searchForXcmArrival(
        events: List<GenericEvent.Instance>,
        transfer: AssetTransferBase
    ): Balance? {
        if (!events.hasXcmArrivalEvent()) return null

        val eventDetector = assetSourceRegistry.getEventDetector(transfer.destinationChainAsset)

        val depositEvent = events.mapNotNull { event -> eventDetector.tryDetectDeposit(event) }
            .find { it.destination == transfer.recipientAccountId }

        return depositEvent?.amount
    }

    private fun List<GenericEvent.Instance>.hasXcmArrivalEvent(): Boolean {
        return hasEvent("MessageQueue", "Processed") or hasEvent("XcmpQueue", "Success")
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
            // We are transferring the exact amount, so we should add nothing on top of the transfer amount
            crossChainTransfer(configuration, transfer, crossChainFee = Balance.ZERO)
        }
    }

    private suspend fun observeTransferableBalance(transfer: AssetTransferBase): Flow<TransferableBalanceUpdate> {
        val destinationAssetBalances = assetSourceRegistry.sourceFor(transfer.destinationChainAsset)

        return destinationAssetBalances.balance.subscribeTransferableAccountBalance(
            chain = transfer.destinationChain,
            chainAsset = transfer.destinationChainAsset,
            accountId = transfer.recipientAccountId.value,
            sharedSubscriptionBuilder = null
        )
    }

    private suspend fun ExtrinsicBuilder.crossChainTransfer(
        configuration: CrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        crossChainFee: Balance
    ) {
        when (configuration) {
            is CrossChainTransferConfiguration.Dynamic -> dynamic.crossChainTransfer(configuration.config, transfer, crossChainFee)
            is CrossChainTransferConfiguration.Legacy -> legacy.crossChainTransfer(configuration.config, transfer, crossChainFee)
        }
    }
}
