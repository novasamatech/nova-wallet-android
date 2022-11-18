package io.novafoundation.nova.feature_assets.data.mappers.mappers

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.OperationModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

private val Operation.Type.operationStatus
    get() = when (this) {
        is Operation.Type.Extrinsic -> status
        is Operation.Type.Reward -> Operation.Status.COMPLETED
        is Operation.Type.Transfer -> status
    }

private fun Chain.Asset.formatPlanksSigned(planks: BigInteger, negative: Boolean): String {
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
    return chainAsset.formatPlanksSigned(transfer.amount, negative = !transfer.isIncome)
}

private fun formatAmount(chainAsset: Chain.Asset, reward: Operation.Type.Reward): String {
    return chainAsset.formatPlanksSigned(reward.amount, negative = !reward.isReward)
}

private fun formatFee(chainAsset: Chain.Asset, extrinsic: Operation.Type.Extrinsic): String {
    return chainAsset.formatPlanksSigned(extrinsic.fee, negative = true)
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

@DrawableRes
private fun Operation.Type.Transfer.transferDirectionIcon(): Int {
    return if (isIncome) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
}

suspend fun mapOperationToOperationModel(
    chain: Chain,
    operation: Operation,
    nameIdentifier: AddressDisplayUseCase.Identifier,
    resourceManager: ResourceManager,
): OperationModel {
    val statusAppearance = mapStatusToStatusAppearance(operation.type.operationStatus)
    val formattedTime = resourceManager.formatTime(operation.time)

    return with(operation) {
        when (val operationType = type) {
            is Operation.Type.Reward -> {
                OperationModel(
                    id = id,
                    formattedTime = formattedTime,
                    amount = formatAmount(chainAsset, operationType),
                    amountColorRes = if (operationType.isReward) R.color.text_positive else R.color.text_primary,
                    header = resourceManager.getString(
                        if (operationType.isReward) R.string.staking_reward else R.string.staking_slash
                    ),
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(R.drawable.ic_staking_filled).asIcon(),
                    subHeader = resourceManager.getString(R.string.tabbar_staking_title),
                )
            }

            is Operation.Type.Transfer -> {
                val amountColor = when {
                    operationType.status == Operation.Status.FAILED -> R.color.failed_transaction_color
                    operationType.isIncome -> R.color.text_positive
                    else -> R.color.text_primary
                }

                OperationModel(
                    id = id,
                    formattedTime = formattedTime,
                    amount = formatAmount(chainAsset, operationType),
                    amountColorRes = amountColor,
                    header = nameIdentifier.nameOrAddress(operationType.displayAddress),
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(operationType.transferDirectionIcon()).asIcon(),
                    subHeader = resourceManager.getString(R.string.transfer_title),
                )
            }

            is Operation.Type.Extrinsic -> {
                val amountColor = if (operationType.status == Operation.Status.FAILED) R.color.failed_transaction_color else R.color.text_primary

                OperationModel(
                    id = id,
                    formattedTime = formattedTime,
                    amount = formatFee(chainAsset, operationType),
                    amountColorRes = amountColor,
                    header = operationType.formattedCall(),
                    statusAppearance = statusAppearance,
                    operationIcon = operation.chainAsset.iconUrl?.asIcon() ?: R.drawable.ic_nova.asIcon(),
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

                val feeFormatted = operationType.fee?.formatPlanks(chain.commissionAsset)
                    ?: resourceManager.getString(R.string.common_unknown)

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
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus),
                    transferDirectionIcon = operationType.transferDirectionIcon()
                )
            }

            is Operation.Type.Reward -> {
                val typeRes = if (operationType.isReward) R.string.staking_reward else R.string.staking_slash

                OperationParcelizeModel.Reward(
                    chainId = chainAsset.chainId,
                    eventId = id,
                    address = address,
                    time = time,
                    amount = formatAmount(chainAsset, operationType),
                    type = resourceManager.getString(typeRes),
                    era = resourceManager.getString(R.string.staking_era_index_no_prefix, operationType.era),
                    validator = operationType.validator,
                    statusAppearance = OperationStatusAppearance.COMPLETED
                )
            }

            is Operation.Type.Extrinsic -> {
                OperationParcelizeModel.Extrinsic(
                    chainId = chainAsset.chainId,
                    chainAssetId = chainAsset.id,
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
