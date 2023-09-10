package io.novafoundation.nova.feature_assets.domain.tokens.add.validations

import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_assets.domain.tokens.add.CustomToken
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.evmAssetNotExist
import io.novafoundation.nova.feature_wallet_api.domain.validation.substrateAssetNotExist
import io.novafoundation.nova.feature_wallet_api.domain.validation.validErc20Contract
import io.novafoundation.nova.feature_wallet_api.domain.validation.validSubstrateTokenId
import io.novafoundation.nova.feature_wallet_api.domain.validation.validTokenDecimals
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias AddCustomTokenValidationSystem = ValidationSystem<AddTokenPayload, AddCustomTokensValidationFailure>
typealias AddCustomTokenValidationSystemBuilder = ValidationSystemBuilder<AddTokenPayload, AddCustomTokensValidationFailure>

sealed interface AddCustomTokensValidationFailure {
    class InvalidTokenContractAddress(val chainName: String) : AddCustomTokensValidationFailure

    object InvalidTokenId : AddCustomTokensValidationFailure

    class EvmAssetExist(val alreadyExistingSymbol: String) : AddCustomTokensValidationFailure

    class SubstrateAssetExist(val alreadyExistingSymbol: String) : AddCustomTokensValidationFailure

    object InvalidDecimals : AddCustomTokensValidationFailure

    object InvalidCoinGeckoLink : AddCustomTokensValidationFailure
}

fun AddCustomTokenValidationSystemBuilder.validErc20Contract(
    ethereumAddressFormat: EthereumAddressFormat,
    erc20Standard: Erc20Standard,
    chainRegistry: ChainRegistry,
) = validErc20Contract(
    ethereumAddressFormat = ethereumAddressFormat,
    erc20Standard = erc20Standard,
    chainRegistry = chainRegistry,
    chain = { it.chain },
    address = { it.customToken.tokenId },
    error = { AddCustomTokensValidationFailure.InvalidTokenContractAddress(it.chain.name) }
)

fun AddCustomTokenValidationSystemBuilder.validSubstrateTokenId() = validSubstrateTokenId(
    tokenId = { it.customToken.tokenId },
    error = { AddCustomTokensValidationFailure.InvalidTokenId }
)

fun AddCustomTokenValidationSystemBuilder.evmAssetNotExist(chainAssetRepository: ChainAssetRepository) = evmAssetNotExist(
    assetRepository = chainAssetRepository,
    chain = { it.chain },
    address = { it.customToken.tokenId },
    assetNotExistError = AddCustomTokensValidationFailure::EvmAssetExist,
    addressMappingError = { AddCustomTokensValidationFailure.InvalidTokenContractAddress(it.chain.name) }
)

fun AddCustomTokenValidationSystemBuilder.substrateAssetNotExist(chainAssetRepository: ChainAssetRepository) = substrateAssetNotExist(
    assetRepository = chainAssetRepository,
    chain = { it.chain },
    tokenId = { it.customToken.tokenId },
    assetNotExistError = AddCustomTokensValidationFailure::SubstrateAssetExist,
    addressMappingError = { AddCustomTokensValidationFailure.InvalidTokenId }
)

fun AddCustomTokenValidationSystemBuilder.validTokenDecimals() = validTokenDecimals(
    decimals = { it.customToken.decimals },
    error = { AddCustomTokensValidationFailure.InvalidDecimals }
)

fun AddCustomTokenValidationSystemBuilder.validCoinGeckoLink(
    coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory
) = validCoinGeckoLink(
    coinGeckoLinkValidationFactory = coinGeckoLinkValidationFactory,
    optional = true,
    link = { it.customToken.priceLink },
    error = { AddCustomTokensValidationFailure.InvalidCoinGeckoLink }
)

class AddTokenPayload(
    val customToken: CustomToken,
    val chain: Chain
)
