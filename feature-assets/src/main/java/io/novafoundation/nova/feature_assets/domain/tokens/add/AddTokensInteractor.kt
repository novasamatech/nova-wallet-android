package io.novafoundation.nova.feature_assets.domain.tokens.add

import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.AddEvmTokenValidationSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.evmAssetNotExists
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validCoinGeckoLink
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validErc20Contract
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validTokenDecimals
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AddTokensInteractor {

    fun availableChainsToAddTokenFlow(): Flow<List<Chain>>

    suspend fun retrieveContractMetadata(
        chainId: ChainId,
        contractAddress: String
    ): Erc20ContractMetadata?

    suspend fun addCustomTokenAndSync(customErc20Token: CustomErc20Token): Result<*>

    fun getValidationSystem(): AddEvmTokenValidationSystem
}

class RealAddTokensInteractor(
    private val chainRegistry: ChainRegistry,
    private val erc20Standard: Erc20Standard,
    private val chainAssetRepository: ChainAssetRepository,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val ethereumAddressFormat: EthereumAddressFormat,
    private val currencyRepository: CurrencyRepository,
    private val walletRepository: WalletRepository,
    private val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory
) : AddTokensInteractor {

    override fun availableChainsToAddTokenFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains.map { chains ->
            chains.filter { it.isEthereumBased }
                .sortedWith(Chain.defaultComparator())
        }
    }

    override suspend fun retrieveContractMetadata(
        chainId: ChainId,
        contractAddress: String,
    ): Erc20ContractMetadata? {
        return runCatching {
            queryErc20Contract(chainId, contractAddress) {
                Erc20ContractMetadata(
                    decimals = executeOrNull { decimals().toInt() },
                    symbol = executeOrNull { symbol() }
                )
            }
        }.getOrNull()
    }

    override suspend fun addCustomTokenAndSync(customErc20Token: CustomErc20Token): Result<*> = runCatching {
        val priceId = coinGeckoLinkParser.parse(customErc20Token.priceLink).getOrNull()?.priceId

        val asset = Chain.Asset(
            iconUrl = null,
            id = chainAssetIdOfErc20Token(customErc20Token.contract),
            priceId = priceId,
            chainId = customErc20Token.chainId,
            symbol = customErc20Token.symbol.asTokenSymbol(),
            precision = customErc20Token.decimals.asPrecision(),
            buyProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.EvmErc20(customErc20Token.contract),
            source = Chain.Asset.Source.MANUAL,
            name = customErc20Token.symbol,
            enabled = true
        )

        chainAssetRepository.insertCustomAsset(asset)

        syncTokenPrice(asset)
    }

    override fun getValidationSystem(): AddEvmTokenValidationSystem {
        return ValidationSystem {
            evmAssetNotExists(chainRegistry)
            validErc20Contract(ethereumAddressFormat, erc20Standard, chainRegistry)
            validTokenDecimals()
            validCoinGeckoLink(coinGeckoLinkValidationFactory)
        }
    }

    private suspend fun <R> queryErc20Contract(
        chainId: ChainId,
        contractAddress: String,
        query: suspend Erc20Queries.() -> R
    ): R {
        val ethereumApi = chainRegistry.getCallEthereumApiOrThrow(chainId)
        val erc20Queries = erc20Standard.querySingle(contractAddress, ethereumApi)

        return query(erc20Queries)
    }

    private suspend fun <R> executeOrNull(action: suspend () -> R): R? = runCatching { action() }.getOrNull()

    private suspend fun syncTokenPrice(asset: Chain.Asset) {
        val currency = currencyRepository.getSelectedCurrency()
        walletRepository.syncAssetRates(asset, currency)
    }
}
