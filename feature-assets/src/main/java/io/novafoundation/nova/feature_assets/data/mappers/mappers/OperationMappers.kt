package io.novafoundation.nova.feature_assets.data.mappers.mappers

import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressIcon
import io.novafoundation.nova.feature_assets.presentation.model.OperationModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

private val Operation.Type.operationStatus
    get() = when (this) {
        is Operation.Type.Extrinsic -> status
        is Operation.Type.Reward -> Operation.Status.COMPLETED
        is Operation.Type.Transfer -> status
    }

private fun Chain.Asset.formatPlanks(planks: BigInteger, negative: Boolean): String {
    val amount = amountFromPlanks(planks)

    val withoutSign = amount.formatTokenAmount(this)
    val sign = if (negative) '-' else '+'

    return sign + withoutSign
}

private val Operation.Type.Transfer.isIncome
    get() = myAddress == receiver

private val Operation.Type.Transfer.displayAddress
    get() = if (isIncome) sender else receiver

private fun formatAmount(chainAsset: Chain.Asset, transfer: Operation.Type.Transfer): String {
    return chainAsset.formatPlanks(transfer.amount, negative = !transfer.isIncome)
}

private fun formatAmount(chainAsset: Chain.Asset, reward: Operation.Type.Reward): String {
    return chainAsset.formatPlanks(reward.amount, negative = !reward.isReward)
}

private fun formatFee(chainAsset: Chain.Asset, extrinsic: Operation.Type.Extrinsic): String {
    return chainAsset.formatPlanks(extrinsic.fee, negative = true)
}

private fun mapStatusToStatusAppearance(status: Operation.Status): OperationStatusAppearance {
    return when (status) {
        Operation.Status.COMPLETED -> OperationStatusAppearance.COMPLETED
        Operation.Status.FAILED -> OperationStatusAppearance.FAILED
        Operation.Status.PENDING -> OperationStatusAppearance.PENDING
    }
}

private val CAMEL_CASE_REGEX = "(?<=[a-z])(?=[A-Z])".toRegex()

private fun String.camelCaseToCapitalizedWords() = CAMEL_CASE_REGEX.split(this).joinToString(separator = " ") { it.capitalize() }

private fun Operation.Type.Extrinsic.formattedCall() = call.camelCaseToCapitalizedWords()
private fun Operation.Type.Extrinsic.formattedModule() = module.camelCaseToCapitalizedWords()

suspend fun mapOperationToOperationModel(
    chain: Chain,
    operation: Operation,
    nameIdentifier: AddressDisplayUseCase.Identifier,
    resourceManager: ResourceManager,
    iconGenerator: AddressIconGenerator,
): OperationModel {
    val statusAppearance = mapStatusToStatusAppearance(operation.type.operationStatus)

    return with(operation) {
        when (val operationType = type) {
            is Operation.Type.Reward -> {
                OperationModel(
                    id = id,
                    time = time,
                    amount = formatAmount(chainAsset, operationType),
                    amountColorRes = if (operationType.isReward) R.color.green else R.color.white,
                    header = resourceManager.getString(
                        if (operationType.isReward) R.string.staking_reward else R.string.staking_slash
                    ),
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(R.drawable.ic_staking),
                    subHeader = resourceManager.getString(R.string.tabbar_staking_title),
                )
            }

            is Operation.Type.Transfer -> {
                val amountColor = when {
                    operationType.status == Operation.Status.FAILED -> R.color.gray2
                    operationType.isIncome -> R.color.green
                    else -> R.color.white
                }

                OperationModel(
                    id = id,
                    time = time,
                    amount = formatAmount(chainAsset, operationType),
                    amountColorRes = amountColor,
                    header = nameIdentifier.nameOrAddress(operationType.displayAddress),
                    statusAppearance = statusAppearance,
                    operationIcon = iconGenerator.createAddressIcon(chain, operationType.displayAddress, AddressIconGenerator.SIZE_BIG),
                    subHeader = resourceManager.getString(R.string.transfer_title),
                )
            }

            is Operation.Type.Extrinsic -> {

                val amountColor = if (operationType.status == Operation.Status.FAILED) R.color.gray2 else R.color.white

                OperationModel(
                    id = id,
                    time = time,
                    amount = formatFee(chainAsset, operationType),
                    amountColorRes = amountColor,
                    header = operationType.formattedCall(),
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(R.drawable.ic_code),
                    subHeader = operationType.formattedModule()
                )
            }
        }
    }
}

suspend fun mapOperationToParcel(
    operation: Operation,
    chainRegistry: ChainRegistry,
    resourceManager: ResourceManager,
): OperationParcelizeModel {
    with(operation) {
        return when (val operationType = operation.type) {
            is Operation.Type.Transfer -> {
                val chain = chainRegistry.getChain(chainAsset.chainId)
                val commissionAsset = chain.utilityAsset

                val feeOrZero = operationType.fee ?: BigInteger.ZERO

                val feeFormatted = operationType.fee?.let {
                    commissionAsset.formatPlanks(it, negative = true)
                } ?: resourceManager.getString(R.string.common_unknown)

                val total = if (commissionAsset == chainAsset) {
                    operationType.amount + feeOrZero
                } else {
                    operationType.amount
                }

                OperationParcelizeModel.Transfer(
                    chainId = operation.chainAsset.chainId,
                    assetId = operation.chainAsset.id,
                    time = time,
                    address = address,
                    hash = operationType.hash,
                    amount = formatAmount(operation.chainAsset, operationType),
                    receiver = operationType.receiver,
                    sender = operationType.sender,
                    fee = feeFormatted,
                    isIncome = operationType.isIncome,
                    total = chainAsset.formatPlanks(total, negative = !operationType.isIncome),
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus)
                )
            }

            is Operation.Type.Reward -> {
                OperationParcelizeModel.Reward(
                    chainId = chainAsset.chainId,
                    eventId = id,
                    address = address,
                    time = time,
                    amount = formatAmount(chainAsset, operationType),
                    isReward = operationType.isReward,
                    era = operationType.era,
                    validator = operationType.validator,
                )
            }

            is Operation.Type.Extrinsic -> {
                OperationParcelizeModel.Extrinsic(
                    chainId = chainAsset.chainId,
                    time = time,
                    originAddress = address,
                    hash = operationType.hash,
                    module = operationType.formattedModule(),
                    call = operationType.formattedCall(),
                    fee = formatFee(chainAsset, operationType),
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus)
                )
            }
        }
    }
}
