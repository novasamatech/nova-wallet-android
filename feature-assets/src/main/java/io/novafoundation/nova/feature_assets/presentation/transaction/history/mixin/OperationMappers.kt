package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.splitSnakeOrCamelCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.ExtrinsicContentParcel
import io.novafoundation.nova.feature_assets.presentation.model.OperationModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Extrinsic.Content
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.accountIdOf
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

private fun Operation.Type.Transfer.isIncome(chain: Chain): Boolean = kotlin.runCatching {
    val myAccountId = chain.accountIdOf(myAddress)
    val receiverAccountId = chain.accountIdOf(receiver)

    myAccountId.contentEquals(receiverAccountId)
}.getOrElse {
    // conversion of an address to account id failed. Try less robust direct comparison
    myAddress.lowercase() == receiver.lowercase()
}

private fun Operation.Type.Transfer.displayAddress(isIncome: Boolean) = if (isIncome) sender else receiver

private fun formatAmount(chainAsset: Chain.Asset, isIncome: Boolean, transfer: Operation.Type.Transfer): String {
    return chainAsset.formatPlanksSigned(transfer.amount, negative = !isIncome)
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

@DrawableRes
private fun transferDirectionIcon(isIncome: Boolean): Int {
    return if (isIncome) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up
}

private fun String.itemToCapitalizedWords(): String {
    val split = splitSnakeOrCamelCase()

    return split.joinToString(separator = " ") { it.capitalize() }
}

private fun mapExtrinsicContentToHeaderAndSubHeader(extrinsicContent: Content, resourceManager: ResourceManager): Pair<String, String> {
    return when (extrinsicContent) {
        is Content.ContractCall -> {
            val header = formatContractFunctionName(extrinsicContent) ?: extrinsicContent.contractAddress
            val subHeader = resourceManager.getString(R.string.ethereum_contract_call)

            header to subHeader
        }

        is Content.SubstrateCall -> {
            val header = extrinsicContent.call.itemToCapitalizedWords()
            val subHeader = extrinsicContent.module.itemToCapitalizedWords()

            header to subHeader
        }
    }
}

private fun formatContractFunctionName(extrinsicContent: Content.ContractCall): String? {
    return extrinsicContent.function?.let { function ->
        val withoutArguments = function.split("(").first()

        withoutArguments.itemToCapitalizedWords()
    }
}

private fun mapExtrinsicContentToParcel(extrinsic: Operation.Type.Extrinsic, resourceManager: ResourceManager): ExtrinsicContentParcel {
    return when (val content = extrinsic.content) {
        is Content.ContractCall -> contractCallUi(content, extrinsic.hash, resourceManager)
        is Content.SubstrateCall -> substrateCallUi(content, extrinsic.hash, resourceManager)
    }
}

private fun contractCallUi(
    content: Content.ContractCall,
    txHash: String,
    resourceManager: ResourceManager
) = ExtrinsicContentParcel {
    block {
        address(resourceManager.getString(R.string.ethereum_contract), content.contractAddress)

        formatContractFunctionName(content)?.let { function ->
            value(resourceManager.getString(R.string.ethereum_function), function)
        }
    }

    block {
        transactionId(txHash)
    }
}

private fun substrateCallUi(
    content: Content.SubstrateCall,
    txHash: String,
    resourceManager: ResourceManager
) = ExtrinsicContentParcel {
    block {
        transactionId(txHash)

        value(resourceManager.getString(R.string.common_module), content.module)

        value(resourceManager.getString(R.string.common_call), content.call)
    }
}

fun mapOperationToOperationModel(
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
                val isIncome = operationType.isIncome(chain)

                val amountColor = when {
                    operationType.status == Operation.Status.FAILED -> R.color.text_secondary
                    isIncome -> R.color.text_positive
                    else -> R.color.text_primary
                }

                OperationModel(
                    id = id,
                    formattedTime = formattedTime,
                    amount = formatAmount(chainAsset, isIncome, operationType),
                    amountColorRes = amountColor,
                    header = nameIdentifier.nameOrAddress(operationType.displayAddress(isIncome)),
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(transferDirectionIcon(isIncome)).asIcon(),
                    subHeader = resourceManager.getString(R.string.transfer_title),
                )
            }

            is Operation.Type.Extrinsic -> {
                val amountColor = if (operationType.status == Operation.Status.FAILED) R.color.text_secondary else R.color.text_primary
                val (header, subHeader) = mapExtrinsicContentToHeaderAndSubHeader(operationType.content, resourceManager)

                OperationModel(
                    id = id,
                    formattedTime = formattedTime,
                    amount = formatFee(chainAsset, operationType),
                    amountColorRes = amountColor,
                    header = header,
                    subHeader = subHeader,
                    statusAppearance = statusAppearance,
                    operationIcon = operation.chainAsset.iconUrl?.asIcon() ?: R.drawable.ic_nova.asIcon()
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

                val isIncome = operationType.isIncome(chain)

                OperationParcelizeModel.Transfer(
                    chainId = operation.chainAsset.chainId,
                    assetId = operation.chainAsset.id,
                    time = time,
                    address = address,
                    hash = operationType.hash,
                    amount = formatAmount(operation.chainAsset, isIncome, operationType),
                    receiver = operationType.receiver,
                    sender = operationType.sender,
                    fee = feeFormatted,
                    isIncome = isIncome,
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus),
                    transferDirectionIcon = transferDirectionIcon(isIncome)
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
                    content = mapExtrinsicContentToParcel(operationType, resourceManager),
                    fee = formatFee(chainAsset, operationType),
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus)
                )
            }
        }
    }
}
