package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class ChooseAmountModel(
    val input: ChooseAmountInputModel,
    val balanceLabel: String?,
    val balance: String
)

class ChooseAmountInputModel(
    val tokenSymbol: String,
    val tokenIcon: String,
)

internal fun ChooseAmountModel(
    asset: Asset,
    resourceManager: ResourceManager,
    retrieveAmount: (Asset) -> BigDecimal = Asset::transferable,
    @StringRes balanceLabelRes: Int?,
): ChooseAmountModel = ChooseAmountModel(
    input = ChooseAmountInputModel(asset.token.configuration),
    balanceLabel = balanceLabelRes?.let(resourceManager::getString)?.ensureSuffix(":"),
    balance = retrieveAmount(asset).formatTokenAmount(asset.token.configuration.symbol)
)

internal fun ChooseAmountInputModel(chainAsset: Chain.Asset): ChooseAmountInputModel = ChooseAmountInputModel(
    tokenSymbol = chainAsset.symbol,
    tokenIcon = chainAsset.iconUrl,
)
