package io.novafoundation.nova.feature_assets.domain.tokens.add.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_assets.domain.tokens.add.CustomErc20Token
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.validEvmAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.evmAssetNotExist
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias AddEvmTokenValidationSystem = ValidationSystem<AddEvmTokenPayload, AddEvmTokensValidationFailure>
typealias AddEvmTokenValidationSystemBuilder = ValidationSystemBuilder<AddEvmTokenPayload, AddEvmTokensValidationFailure>

sealed interface AddEvmTokensValidationFailure {
    object InvalidContractAddress : AddEvmTokensValidationFailure

    object AssetExist : AddEvmTokensValidationFailure
}

fun AddEvmTokenValidationSystemBuilder.validEvmAddress() = validEvmAddress(
    address = { it.customErc20Token.contract },
    chain = { it.chain },
    error = { AddEvmTokensValidationFailure.InvalidContractAddress }
)

fun AddEvmTokenValidationSystemBuilder.evmAssetNotExist(chainAssetRepository: ChainAssetRepository) = evmAssetNotExist(
    assetRepository = chainAssetRepository,
    chain = { it.chain },
    address = { it.customErc20Token.contract },
    assetNotExistError = { AddEvmTokensValidationFailure.AssetExist },
    addressMappingError = { AddEvmTokensValidationFailure.InvalidContractAddress }
)

class AddEvmTokenPayload(
    val customErc20Token: CustomErc20Token,
    val chain: Chain
)
