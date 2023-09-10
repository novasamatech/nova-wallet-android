package io.novafoundation.nova.feature_assets.domain.tokens.add

import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.assets
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.AddCustomTokenValidationSystem
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.CoinGeckoLinkValidationFactory
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.evmAssetNotExist
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.substrateAssetNotExist
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validCoinGeckoLink
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validErc20Contract
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validSubstrateTokenId
import io.novafoundation.nova.feature_assets.domain.tokens.add.validations.validTokenDecimals
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.awaitCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

interface AddTokensInteractor {

    fun availableChainsToAddErc20TokenFlow(): Flow<List<Chain>>

    fun availableChainsToAddSubstrateTokenFlow(): Flow<List<Chain>>

    suspend fun retrieveContractMetadata(
        chainId: ChainId,
        tokenId: String
    ): TokenMetadata?

    suspend fun addCustomTokenAndSync(customToken: CustomToken): Result<*>

    fun getValidationSystem(isEthereumBased: Boolean): AddCustomTokenValidationSystem
}

class RealAddTokensInteractor(
    private val chainRegistry: ChainRegistry,
    private val erc20Standard: Erc20Standard,
    private val chainAssetRepository: ChainAssetRepository,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val ethereumAddressFormat: EthereumAddressFormat,
    private val currencyRepository: CurrencyRepository,
    private val walletRepository: WalletRepository,
    private val coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory,
    private val remoteStorage: StorageDataSource
) : AddTokensInteractor {

    override fun availableChainsToAddErc20TokenFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains.map { chains ->
            chains
                .filter { it.isEthereumBased }
                .sortedWith(Chain.defaultComparator())
        }
    }

    override fun availableChainsToAddSubstrateTokenFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains.map { chains ->
            chains.asSequence()
                .filter { it.id == ChainGeneses.STATEMINE }
                .take(1)
                .sortedWith(Chain.defaultComparator())
                .toList()
        }
    }

    override suspend fun retrieveContractMetadata(
        chainId: ChainId,
        tokenId: String,
    ): TokenMetadata? {
        return runCatching {
            if (chainRegistry.getChain(chainId).isEthereumBased) {
                queryErc20Contract(chainId, tokenId) {
                    TokenMetadata(
                        decimals = executeOrNull { decimals().toInt() },
                        symbol = executeOrNull { symbol() }
                    )
                }
            } else {
                val result = querySubstrateTokenMetadata(chainId, tokenId)
                TokenMetadata(
                    decimals = result.getTyped<BigInteger>("decimals").toInt(),
                    symbol = result.getTyped<ByteArray>("symbol").decodeToString()
                )
            }
        }.getOrNull()
    }

    override suspend fun addCustomTokenAndSync(customToken: CustomToken): Result<*> = runCatching {
        val priceId = coinGeckoLinkParser.parse(customToken.priceLink).getOrNull()?.priceId

        val asset = Chain.Asset(
            iconUrl = null,
            id = chainAssetIdOfErc20Token(customToken.tokenId),
            priceId = priceId,
            chainId = customToken.chainId,
            symbol = customToken.symbol,
            precision = customToken.decimals,
            buyProviders = emptyMap(),
            staking = emptyList(),
            type = Chain.Asset.Type.EvmErc20(customToken.tokenId),
            source = Chain.Asset.Source.MANUAL,
            name = customToken.symbol,
            enabled = true
        )

        chainAssetRepository.insertCustomAsset(asset)

        syncTokenPrice(asset)
    }

    override fun getValidationSystem(isEthereumBased: Boolean): AddCustomTokenValidationSystem {
        return ValidationSystem {
            if (isEthereumBased) {
                evmAssetNotExist(chainAssetRepository)
                validErc20Contract(ethereumAddressFormat, erc20Standard, chainRegistry)
            } else {
                validSubstrateTokenId()
                substrateAssetNotExist(chainAssetRepository)
            }
            validTokenDecimals()
            validCoinGeckoLink(coinGeckoLinkValidationFactory)
        }
    }

    private suspend fun <R> queryErc20Contract(
        chainId: ChainId,
        contractAddress: String,
        query: suspend Erc20Queries.() -> R
    ): R {
        val ethereumApi = chainRegistry.awaitCallEthereumApiOrThrow(chainId)
        val erc20Queries = erc20Standard.querySingle(contractAddress, ethereumApi)

        return query(erc20Queries)
    }

    private suspend fun querySubstrateTokenMetadata(
        chainId: ChainId,
        tokenId: String
    ): Struct.Instance {
        return remoteStorage.queryNonNull(
            // Current session index
            keyBuilder = {
                it.metadata.assets().storage("Metadata").storageKey(it, tokenId.toBigInteger())
            },
            binding = ::bindCurrentIndex,
            chainId = chainId
        )
    }

    fun bindCurrentIndex(
        scale: String,
        runtime: RuntimeSnapshot
    ): Struct.Instance {
        val returnType = runtime.metadata.assets().storage("Metadata").returnType()
        return scale.let { returnType.fromHexOrNull(runtime, it) }.cast<Struct.Instance>()
    }

    private suspend fun <R> executeOrNull(action: suspend () -> R): R? = runCatching { action() }.getOrNull()

    private suspend fun syncTokenPrice(asset: Chain.Asset) {
        val currency = currencyRepository.getSelectedCurrency()
        walletRepository.syncAssetRates(asset, currency)
    }
}
