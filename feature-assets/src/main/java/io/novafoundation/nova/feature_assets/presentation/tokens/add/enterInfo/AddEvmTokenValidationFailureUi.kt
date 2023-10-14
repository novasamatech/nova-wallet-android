package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.AddEvmTokensValidationFailure

fun mapAddEvmTokensValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: AddEvmTokensValidationFailure
): TitleAndMessage {
    return when (failure) {
        is AddEvmTokensValidationFailure.AssetExist -> {
            val title = resourceManager.getString(R.string.asset_add_evm_token_already_exist_title)
            val message = if (failure.canModify) {
                resourceManager.getString(R.string.asset_add_evm_token_already_exist_modifiable_message, failure.alreadyExistingSymbol)
            } else {
                resourceManager.getString(R.string.asset_add_evm_token_already_exist_message, failure.alreadyExistingSymbol)
            }

            title to message
        }
        is AddEvmTokensValidationFailure.InvalidTokenContractAddress -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_message, failure.chainName)
        }
        AddEvmTokensValidationFailure.InvalidDecimals -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_decimals_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_decimals_message)
        }
        AddEvmTokensValidationFailure.InvalidCoinGeckoLink -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_message)
        }
    }
}
