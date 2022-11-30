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
        AddEvmTokensValidationFailure.AssetExist -> {
            resourceManager.getString(R.string.asset_add_evm_token_already_exist_title) to
                resourceManager.getString(R.string.asset_add_evm_token_already_exist_message)
        }
        AddEvmTokensValidationFailure.InvalidContractAddress -> {
            resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_contract_address_message)
        }
    }
}
