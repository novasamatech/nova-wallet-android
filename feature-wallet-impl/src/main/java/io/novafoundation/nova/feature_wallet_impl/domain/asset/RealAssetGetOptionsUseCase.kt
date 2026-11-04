package io.novafoundation.nova.feature_wallet_impl.domain.asset

import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.AssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.incomingCrossChainDirectionsAvailable
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class RealAssetGetOptionsUseCase(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val accountRepository: AccountRepository,
    private val globalConfigDataSource: GlobalConfigDataSource,
    private val logger: Logger
) : AssetGetOptionsUseCase {

    override fun observeAssetGetOptionsForSelectedAccount(chainAssetFlow: Flow<Chain.Asset?>): Flow<Set<GetAssetOption>> {
        return combine(
            crossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(chainAssetFlow),
            receiveAvailable(chainAssetFlow),
            buyAvailable(chainAssetFlow),
            availableDApps(chainAssetFlow),
        ) { crossChainTransfersAvailable, receiveAvailable, buyAvailable, dApps ->
            setOfNotNull(
                GetAssetOption.CrossChain.takeIf { crossChainTransfersAvailable },
                GetAssetOption.Receive.takeIf { receiveAvailable },
                GetAssetOption.Buy.takeIf { buyAvailable }
            ) + dApps
        }
    }

    private fun availableDApps(chainAssetFlow: Flow<Chain.Asset?>): Flow<List<GetAssetOption.DApp>> {
        return chainAssetFlow.flatMapLatest { asset ->
            if (asset?.chainId == ChainGeneses.BITTENSOR && asset.type == Chain.Asset.Type.Native) {
                flow {
                    emit(emptyList())
                    emit(listOfNotNull(changeNowOption()))
                }
            } else {
                flowOf(emptyList())
            }
        }
    }

    private suspend fun changeNowOption(): GetAssetOption.DApp? {
        return runCatching {
            val configuredUrl = globalConfigDataSource.getGlobalConfig().changeNowTaoUrl ?: return null
            val url = configuredUrl.toHttpUrlOrNull() ?: return null
            if (!url.isHttps) return null

            GetAssetOption.DApp(
                name = "ChangeNOW",
                url = configuredUrl,
                supportedTokens = listOf("DOT", "USDT", "USDC").map { it.asTokenSymbol() }
            )
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            logger.log("[GetAssetOptions] ChangeNOW is unavailable")
            logger.log(error)
            null
        }
    }

    private fun buyAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return chainAssetFlow.map { it != null && it.buyProviders.isNotEmpty() }
    }

    private fun receiveAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return combine(accountRepository.selectedMetaAccountFlow(), chainAssetFlow) { metaAccout, asset ->
            metaAccout.type != LightMetaAccount.Type.WATCH_ONLY && asset != null
        }
    }
}
