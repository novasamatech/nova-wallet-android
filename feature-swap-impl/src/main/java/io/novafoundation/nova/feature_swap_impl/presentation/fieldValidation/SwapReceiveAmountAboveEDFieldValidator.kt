package io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SwapReceiveAmountAboveEDFieldValidatorFactory(
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry
) {

    fun create(assetFlow: Flow<Asset?>): SwapReceiveAmountAboveEDFieldValidator {
        return SwapReceiveAmountAboveEDFieldValidator(resourceManager, chainRegistry, assetSourceRegistry, assetFlow)
    }
}

class SwapReceiveAmountAboveEDFieldValidator(
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val assetFlow: Flow<Asset?>
) : FieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, assetWithExistentialDeposit()) { input, assetWithExistentialDeposit ->
            val amount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok
            val asset = assetWithExistentialDeposit?.first ?: return@combine FieldValidationResult.Ok
            val existentialDeposit = assetWithExistentialDeposit.second

            when {
                amount >= BigDecimal.ZERO && asset.balanceCountedTowardsED() + amount < existentialDeposit -> {
                    val formattedExistentialDeposit = existentialDeposit.formatTokenAmount(asset.token.configuration)
                    FieldValidationResult.Error(
                        resourceManager.getString(R.string.swap_field_validation_to_low_amount_out, formattedExistentialDeposit)
                    )
                }

                else -> FieldValidationResult.Ok
            }
        }
    }

    private fun assetWithExistentialDeposit(): Flow<Pair<Asset, BigDecimal>?> {
        return assetFlow
            .map { asset ->
                asset?.let {
                    val chain = chainRegistry.getChain(asset.token.configuration.chainId)
                    val existentialDeposit = assetSourceRegistry.existentialDeposit(chain, asset.token.configuration)
                    asset to existentialDeposit
                }
            }
            .distinctUntilChangedBy { it?.first?.token?.configuration?.fullId }
    }
}
