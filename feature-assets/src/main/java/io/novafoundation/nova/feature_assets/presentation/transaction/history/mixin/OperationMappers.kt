package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin

import android.text.TextUtils
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.splitSnakeOrCamelCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.AmountParcelModel
import io.novafoundation.nova.feature_assets.presentation.model.ExtrinsicContentParcel
import io.novafoundation.nova.feature_assets.presentation.model.OperationModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Extrinsic.Content
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Reward.RewardKind
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

private class EllipsizedString(val value: String, val elipsize: TextUtils.TruncateAt)

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

private fun mapExtrinsicContentToHeaderAndSubHeader(extrinsicContent: Content, resourceManager: ResourceManager): Pair<String, EllipsizedString> {
    return when (extrinsicContent) {
        is Content.ContractCall -> mapContractCallToHeaderAndSubHeader(extrinsicContent, resourceManager)

        is Content.SubstrateCall -> {
            val header = extrinsicContent.call.itemToCapitalizedWords()
            val subHeader = extrinsicContent.module.itemToCapitalizedWords()

            header to EllipsizedString(subHeader, TextUtils.TruncateAt.END)
        }
    }
}

private fun mapContractCallToHeaderAndSubHeader(content: Content.ContractCall, resourceManager: ResourceManager): Pair<String, EllipsizedString> {
    val header = resourceManager.getString(R.string.ethereum_contract_call)
    val functionName = formatContractFunctionName(content)
    val subHeaderEllipsized = if (functionName?.contains("transfer") == true) {
        EllipsizedString(functionName, TextUtils.TruncateAt.END)
    } else {
        val contractAddress = resourceManager.getString(R.string.transfer_history_send_to, content.contractAddress)
        EllipsizedString(contractAddress, TextUtils.TruncateAt.END)
    }

    return header to subHeaderEllipsized
}

private fun formatContractFunctionName(extrinsicContent: Content.ContractCall): String? {
    return extrinsicContent.function?.let { function ->
        val withoutArguments = function.split("(").first()

        withoutArguments.itemToCapitalizedWords()
    }
}

private fun mapExtrinsicContentToParcel(
    extrinsic: Operation.Type.Extrinsic,
    extrinsicHash: String?,
    resourceManager: ResourceManager
): ExtrinsicContentParcel {
    return when (val content = extrinsic.content) {
        is Content.ContractCall -> contractCallUi(content, extrinsicHash, resourceManager)
        is Content.SubstrateCall -> substrateCallUi(content, extrinsicHash, resourceManager)
    }
}

private fun contractCallUi(
    content: Content.ContractCall,
    txHash: String?,
    resourceManager: ResourceManager
) = ExtrinsicContentParcel {
    block {
        address(resourceManager.getString(R.string.ethereum_contract), content.contractAddress)

        formatContractFunctionName(content)?.let { function ->
            value(resourceManager.getString(R.string.ethereum_function), function)
        }
    }

    txHash?.let {
        block {
            transactionId(txHash)
        }
    }
}

private fun substrateCallUi(
    content: Content.SubstrateCall,
    txHash: String?,
    resourceManager: ResourceManager
) = ExtrinsicContentParcel {
    block {
        txHash?.let {
            transactionId(txHash)
        }

        value(resourceManager.getString(R.string.common_module), content.module.itemToCapitalizedWords())

        value(resourceManager.getString(R.string.common_call), content.call.itemToCapitalizedWords())
    }
}

fun mapOperationToOperationModel(
    chain: Chain,
    token: Token,
    operation: Operation,
    nameIdentifier: AddressDisplayUseCase.Identifier,
    resourceManager: ResourceManager,
): OperationModel {
    val statusAppearance = mapStatusToStatusAppearance(operation.type.operationStatus)
    val formattedTime = resourceManager.formatTime(operation.time)

    return with(operation) {
        when (val operationType = type) {
            is Operation.Type.Reward -> {
                val headerResId = if (operationType.isReward) R.string.staking_reward else R.string.staking_slash
                OperationModel(
                    id = id,
                    amount = formatAmount(chainAsset, operationType),
                    amountDetails = mapToFiatWithTime(token, operationType.fiatAmount, formattedTime, resourceManager),
                    amountColorRes = if (operationType.isReward) R.color.text_positive else R.color.text_primary,
                    header = resourceManager.getString(headerResId),
                    subHeader = resourceManager.getString(R.string.tabbar_staking_title),
                    subHeaderEllipsize = TextUtils.TruncateAt.END,
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(R.drawable.ic_staking_filled).asIcon(),
                )
            }

            is Operation.Type.Transfer -> {
                val isIncome = operationType.isIncome(chain)

                val amountColor = when {
                    operationType.status == Operation.Status.FAILED -> R.color.text_tertiary
                    isIncome -> R.color.text_positive
                    else -> R.color.text_primary
                }

                val nameOrAddress = nameIdentifier.nameOrAddress(operationType.displayAddress(isIncome))

                val subHeader = if (isIncome) {
                    resourceManager.getString(R.string.transfer_history_income_from, nameOrAddress)
                } else {
                    resourceManager.getString(R.string.transfer_history_send_to, nameOrAddress)
                }

                OperationModel(
                    id = id,
                    amount = formatAmount(chainAsset, isIncome, operationType),
                    amountDetails = mapToFiatWithTime(token, operationType.fiatAmount, formattedTime, resourceManager),
                    amountColorRes = amountColor,
                    header = resourceManager.getString(R.string.transfer_title),
                    subHeader = subHeader,
                    subHeaderEllipsize = TextUtils.TruncateAt.MIDDLE,
                    statusAppearance = statusAppearance,
                    operationIcon = resourceManager.getDrawable(transferDirectionIcon(isIncome)).asIcon(),
                )
            }

            is Operation.Type.Extrinsic -> {
                val amountColor = if (operationType.status == Operation.Status.FAILED) R.color.text_tertiary else R.color.text_primary
                val (header, subHeader) = mapExtrinsicContentToHeaderAndSubHeader(operationType.content, resourceManager)

                OperationModel(
                    id = id,
                    amount = formatFee(chainAsset, operationType),
                    amountDetails = mapToFiatWithTime(token, operationType.fiatFee, formattedTime, resourceManager),
                    amountColorRes = amountColor,
                    header = header,
                    subHeader = subHeader.value,
                    subHeaderEllipsize = subHeader.elipsize,
                    statusAppearance = statusAppearance,
                    operationIcon = operation.chainAsset.iconUrl?.asIcon() ?: R.drawable.ic_nova.asIcon()
                )
            }
        }
    }
}

fun mapToFiatWithTime(
    token: Token,
    amount: BigDecimal?,
    time: String,
    resourceManager: ResourceManager,
): String {
    val fiatAmount = amount?.formatAsCurrency(token.currency)
    return if (fiatAmount == null) {
        time
    } else {
        resourceManager.getString(R.string.transaction_history_fiat_with_time, fiatAmount, time)
    }
}

suspend fun mapOperationToParcel(
    operation: Operation,
    chainRegistry: ChainRegistry,
    resourceManager: ResourceManager,
    currency: Currency,
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
                    hash =operation.extrinsicHash,
                    amount = AmountParcelModel(
                        token = formatAmount(operation.chainAsset, isIncome, operationType),
                        fiat = operationType.fiatAmount?.formatAsCurrency(currency)
                    ),
                    receiver = operationType.receiver,
                    sender = operationType.sender,
                    fee = operationType.fee,
                    formattedFee = feeFormatted,
                    isIncome = isIncome,
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus),
                    transferDirectionIcon = transferDirectionIcon(isIncome)
                )
            }

            is Operation.Type.Reward -> {
                val typeRes = if (operationType.isReward) R.string.staking_reward else R.string.staking_slash

                val amount = AmountParcelModel(
                    token = formatAmount(chainAsset, operationType),
                    fiat = operationType.fiatAmount?.formatAsCurrency(currency)
                )

                when(val rewardKind = operationType.kind) {
                    is RewardKind.Direct -> OperationParcelizeModel.Reward(
                        chainId = chainAsset.chainId,
                        eventId = id,
                        address = address,
                        time = time,
                        amount = amount,
                        type = resourceManager.getString(typeRes),
                        era = resourceManager.getString(R.string.staking_era_index_no_prefix, rewardKind.era),
                        validator = rewardKind.validator,
                        statusAppearance = OperationStatusAppearance.COMPLETED
                    )

                    is RewardKind.Pool -> OperationParcelizeModel.PoolReward(
                        chainId = chainAsset.chainId,
                        address = address,
                        time = time,
                        amount = amount,
                        type = resourceManager.getString(typeRes),
                        poolId = rewardKind.poolId,
                        extrinsicHash = extrinsicHash
                    )
                }
            }

            is Operation.Type.Extrinsic -> {
                OperationParcelizeModel.Extrinsic(
                    chainId = chainAsset.chainId,
                    chainAssetId = chainAsset.id,
                    time = time,
                    originAddress = address,
                    content = mapExtrinsicContentToParcel(operationType, extrinsicHash, resourceManager),
                    fee = formatFee(chainAsset, operationType),
                    fiatFee = operationType.fiatFee?.formatAsCurrency(currency),
                    statusAppearance = mapStatusToStatusAppearance(operationType.operationStatus)
                )
            }
        }
    }
}
