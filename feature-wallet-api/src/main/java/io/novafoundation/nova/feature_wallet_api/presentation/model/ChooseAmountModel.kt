package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChooseAmountModel(
    val input: ChooseAmountInputModel,
    val balanceLabel: String?,
)

class ChooseAmountInputModel(
    val tokenSymbol: String,
    val tokenIcon: String?,
)

internal fun ChooseAmountModel(
    asset: Asset,
    resourceManager: ResourceManager,
    @StringRes balanceLabelRes: Int?,
): ChooseAmountModel = ChooseAmountModel(
    input = ChooseAmountInputModel(asset.token.configuration),
    balanceLabel = balanceLabelRes?.let(resourceManager::getString)?.ensureSuffix(":"),
)

internal fun ChooseAmountInputModel(chainAsset: Chain.Asset): ChooseAmountInputModel = ChooseAmountInputModel(
    tokenSymbol = chainAsset.symbol,
    tokenIcon = chainAsset.iconUrl,
)
