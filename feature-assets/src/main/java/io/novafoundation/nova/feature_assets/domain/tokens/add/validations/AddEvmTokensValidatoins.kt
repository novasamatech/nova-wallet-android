package io.novafoundation.nova.feature_assets.domain.tokens.add.validations

import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_assets.domain.tokens.add.CustomErc20Token
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.evmAssetNotExist
import io.novafoundation.nova.feature_wallet_api.domain.validation.validEvmAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.validTokenDecimals
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias AddEvmTokenValidationSystem = ValidationSystem<AddEvmTokenPayload, AddEvmTokensValidationFailure>
typealias AddEvmTokenValidationSystemBuilder = ValidationSystemBuilder<AddEvmTokenPayload, AddEvmTokensValidationFailure>

sealed interface AddEvmTokensValidationFailure {
    object InvalidTokenContractAddress : AddEvmTokensValidationFailure

    class AssetExist(val alreadyExistingSymbol: String) : AddEvmTokensValidationFailure

    object InvalidDecimals : AddEvmTokensValidationFailure

    object InvalidCoinGeckoLink : AddEvmTokensValidationFailure
}

fun AddEvmTokenValidationSystemBuilder.validEvmAddress(
    ethereumAddressFormat: EthereumAddressFormat,
    erc20Standard: Erc20Standard,
    chainRegistry: ChainRegistry,
) = validEvmAddress(
    ethereumAddressFormat = ethereumAddressFormat,
    erc20Standard = erc20Standard,
    chainRegistry = chainRegistry,
    chain = { it.chain },
    address = { it.customErc20Token.contract },
    error = { AddEvmTokensValidationFailure.InvalidTokenContractAddress }
)

fun AddEvmTokenValidationSystemBuilder.evmAssetNotExist(chainAssetRepository: ChainAssetRepository) = evmAssetNotExist(
    assetRepository = chainAssetRepository,
    chain = { it.chain },
    address = { it.customErc20Token.contract },
    assetNotExistError = AddEvmTokensValidationFailure::AssetExist,
    addressMappingError = { AddEvmTokensValidationFailure.InvalidTokenContractAddress }
)

fun AddEvmTokenValidationSystemBuilder.validTokenDecimals() = validTokenDecimals(
    decimals = { it.customErc20Token.decimals },
    error = { AddEvmTokensValidationFailure.InvalidDecimals }
)

fun AddEvmTokenValidationSystemBuilder.validCoinGeckoLink(
    coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory
) = validCoinGeckoLink(
    coinGeckoLinkValidationFactory = coinGeckoLinkValidationFactory,
    optional = true,
    link = { it.customErc20Token.priceLink },
    error = { AddEvmTokensValidationFailure.InvalidCoinGeckoLink }
)

class AddEvmTokenPayload(
    val customErc20Token: CustomErc20Token,
    val chain: Chain
)
