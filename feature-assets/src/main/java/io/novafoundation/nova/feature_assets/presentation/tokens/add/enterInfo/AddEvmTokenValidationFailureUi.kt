package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.AddCustomTokensValidationFailure

fun mapAddEvmTokensValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: AddCustomTokensValidationFailure
): TitleAndMessage {
    return when (failure) {
        is AddCustomTokensValidationFailure.EvmAssetExist -> {
            resourceManager.getString(R.string.asset_add_token_already_exist_title) to
                resourceManager.getString(R.string.asset_add_evm_token_already_exist_message, failure.alreadyExistingSymbol)
        }
        is AddCustomTokensValidationFailure.SubstrateAssetExist -> {
            resourceManager.getString(R.string.asset_add_token_already_exist_title) to
                    resourceManager.getString(R.string.asset_add_substrate_token_already_exist_message, failure.alreadyExistingSymbol)
        }
        is AddCustomTokensValidationFailure.InvalidTokenContractAddress -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_message, failure.chainName)
        }
        is AddCustomTokensValidationFailure.InvalidTokenId -> {
            resourceManager.getString(R.string.asset_add_substrate_token_invalid_token_id_title) to
                    resourceManager.getString(R.string.asset_add_substrate_token_invalid_token_id_message)
        }
        AddCustomTokensValidationFailure.InvalidDecimals -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_decimals_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_decimals_message)
        }
        AddCustomTokensValidationFailure.InvalidCoinGeckoLink -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_message)
        }
    }
}
