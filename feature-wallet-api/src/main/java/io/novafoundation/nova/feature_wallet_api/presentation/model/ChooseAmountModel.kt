package io.novafoundation.nova.feature_wallet_api.presentation.model

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.ensureSuffix
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class ChooseAmountModel(
    val input: ChooseAmountInputModel,
    val balanceLabel: String?,
    val balance: String?
)

class ChooseAmountInputModel(
    val tokenSymbol: String,
    val tokenIcon: String?,
)

internal fun ChooseAmountModel(
    asset: Asset,
    resourceManager: ResourceManager,
    availableBalance: BigInteger?,
    @StringRes balanceLabelRes: Int?,
): ChooseAmountModel = ChooseAmountModel(
    input = ChooseAmountInputModel(asset.token.configuration),
    balanceLabel = balanceLabelRes?.let(resourceManager::getString)?.ensureSuffix(":"),
    balance = availableBalance?.let { mapAmountToAmountModel(it, asset).token }
)

internal fun ChooseAmountInputModel(chainAsset: Chain.Asset): ChooseAmountInputModel = ChooseAmountInputModel(
    tokenSymbol = chainAsset.symbol,
    tokenIcon = chainAsset.iconUrl,
)
