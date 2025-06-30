package io.novafoundation.nova.feature_xcm_impl.builder

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.withAmount
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.builder.fees.PayFeesMode
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.XcmInstruction
import io.novafoundation.nova.feature_xcm_api.message.XcmMessage
import io.novafoundation.nova.feature_xcm_api.message.asVersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.asXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.multiAssetIdOn
import io.novafoundation.nova.feature_xcm_api.multiLocation.toMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit

internal class RealXcmBuilder(
    initialLocation: ChainLocation,
    override val xcmVersion: XcmVersion,
    private val measureXcmFees: MeasureXcmFees,
) : XcmBuilder {

    override var currentLocation: ChainLocation = initialLocation

    private val previousContexts: MutableList<PendingContextInstructions> = mutableListOf()
    private val currentLocationInstructions: MutableList<PendingInstruction> = mutableListOf()

    override fun payFees(payFeesMode: PayFeesMode) {
        currentLocationInstructions.add(PendingInstruction.PayFees(payFeesMode))
    }

    override fun withdrawAsset(assets: MultiAssets) {
        addRegularInstruction(XcmInstruction.WithdrawAsset(assets))
    }

    override fun buyExecution(fees: MultiAsset, weightLimit: WeightLimit) {
        addRegularInstruction(XcmInstruction.BuyExecution(fees, weightLimit))
    }

    override fun depositAsset(assets: MultiAssetFilter, beneficiary: AccountIdKey) {
        addRegularInstruction(XcmInstruction.DepositAsset(assets, beneficiary.toMultiLocation()))
    }

    override fun transferReserveAsset(assets: MultiAssets, dest: ChainLocation) {
        performContextSwitch(dest) { forwardedMessage, forwardingFrom ->
            XcmInstruction.TransferReserveAsset(assets, dest.location.fromPointOfViewOf(forwardingFrom), forwardedMessage)
        }
    }

    override fun initiateReserveWithdraw(assets: MultiAssetFilter, reserve: ChainLocation) {
        performContextSwitch(reserve) { forwardedMessage, forwardingFrom ->
            XcmInstruction.InitiateReserveWithdraw(assets, reserve.location.fromPointOfViewOf(forwardingFrom), forwardedMessage)
        }
    }

    override fun depositReserveAsset(assets: MultiAssetFilter, dest: ChainLocation) {
        performContextSwitch(dest) { forwardedMessage, forwardingFrom ->
            XcmInstruction.DepositReserveAsset(assets, dest.location.fromPointOfViewOf(forwardingFrom), forwardedMessage)
        }
    }

    override fun initiateTeleport(assets: MultiAssetFilter, dest: ChainLocation) {
        performContextSwitch(dest) { forwardedMessage, forwardingFrom ->
            XcmInstruction.InitiateTeleport(assets, dest.location.fromPointOfViewOf(forwardingFrom), forwardedMessage)
        }
    }

    override suspend fun build(): VersionedXcmMessage {
        val lastMessage = createXcmMessage(currentLocationInstructions, currentLocation)

        return previousContexts.foldRight(lastMessage) { context, forwardedMessage ->
            createXcmMessage(context, forwardedMessage)
        }.versionedXcm(xcmVersion)
    }

    private fun addRegularInstruction(instruction: XcmInstruction) {
        currentLocationInstructions.add(PendingInstruction.Regular(instruction))
    }

    private suspend fun createXcmMessage(
        pendingContextInstructions: PendingContextInstructions,
        forwardedMessage: XcmMessage
    ): XcmMessage {
        val switchInstruction = pendingContextInstructions.contextSwitch(forwardedMessage, pendingContextInstructions.chainLocation.location)
        val allInstructions = pendingContextInstructions.instructions + PendingInstruction.Regular(switchInstruction)

        return createXcmMessage(allInstructions, pendingContextInstructions.chainLocation)
    }

    private suspend fun createXcmMessage(
        pendingInstructions: List<PendingInstruction>,
        chainLocation: ChainLocation,
    ): XcmMessage {
        return pendingInstructions.map { pendingInstruction ->
            pendingInstruction.constructSubmissionInstruction(pendingInstructions, chainLocation)
        }.asXcmMessage()
    }

    private suspend fun PendingInstruction.constructSubmissionInstruction(
        allInstructions: List<PendingInstruction>,
        chainLocation: ChainLocation,
    ): XcmInstruction {
        return when (this) {
            is PendingInstruction.Regular -> instruction
            is PendingInstruction.PayFees -> constructSubmissionInstruction(allInstructions, chainLocation)
        }
    }

    private suspend fun PendingInstruction.PayFees.constructSubmissionInstruction(
        allInstructions: List<PendingInstruction>,
        chainLocation: ChainLocation,
    ): XcmInstruction.PayFees {
        val fees = when (val mode = mode) {
            is PayFeesMode.Exact -> mode.fee
            is PayFeesMode.Measured -> measureFees(allInstructions, mode.feeAssetId, chainLocation)
        }

        return XcmInstruction.PayFees(fees)
    }

    private suspend fun measureFees(
        allInstructions: List<PendingInstruction>,
        feeAssetIdLocation: AssetLocation,
        chainLocation: ChainLocation,
    ): MultiAsset {
        val feeAssetId = feeAssetIdLocation.multiAssetIdOn(chainLocation)

        val messageForEstimation = allInstructions.map { pendingInstruction ->
            pendingInstruction.constructEstimationInstruction(feeAssetId)
        }.asVersionedXcmMessage(xcmVersion)

        require(chainLocation.chainId == feeAssetIdLocation.assetId.chainId) {
            """
                 Supplied fee asset does not belong to the current chain.
                 Expected: ${chainLocation.chainId}
                 Got: ${feeAssetIdLocation.assetId.chainId} (Asset id: ${feeAssetIdLocation.assetId.assetId})
            """.trimIndent()
        }

        val measuredFees = measureXcmFees.measureFees(messageForEstimation, feeAssetIdLocation, chainLocation)

        return feeAssetId.withAmount(measuredFees)
    }

    private fun PendingInstruction.constructEstimationInstruction(feeAssetId: MultiAssetId): XcmInstruction {
        return when (this) {
            is PendingInstruction.Regular -> instruction
            is PendingInstruction.PayFees -> constructEstimationInstruction(feeAssetId)
        }
    }

    private fun PendingInstruction.PayFees.constructEstimationInstruction(feeAssetId: MultiAssetId): XcmInstruction.PayFees {
        val fees = when (val mode = mode) {
            is PayFeesMode.Exact -> mode.fee
            is PayFeesMode.Measured -> feeAssetId.withAmount(BalanceOf.ONE) // Use fake amount in pay fees instruction for fee estimation
        }

        return XcmInstruction.PayFees(fees)
    }

    private fun performContextSwitch(newLocation: ChainLocation, switch: PendingContextSwitch) {
        val instructionsInCurrentContext = currentLocationInstructions.toList()
        val pendingContextInstructions = PendingContextInstructions(instructionsInCurrentContext, currentLocation, switch)

        previousContexts.add(pendingContextInstructions)
        currentLocationInstructions.clear()
        currentLocation = newLocation
    }

    private class PendingContextInstructions(
        val instructions: List<PendingInstruction>,
        val chainLocation: ChainLocation,
        val contextSwitch: PendingContextSwitch
    )

    private sealed class PendingInstruction {

        class Regular(val instruction: XcmInstruction) : PendingInstruction()

        class PayFees(val mode: PayFeesMode) : PendingInstruction()
    }
}

private typealias PendingContextSwitch = (forwardedXcm: XcmMessage, forwardingFrom: AbsoluteMultiLocation) -> XcmInstruction
