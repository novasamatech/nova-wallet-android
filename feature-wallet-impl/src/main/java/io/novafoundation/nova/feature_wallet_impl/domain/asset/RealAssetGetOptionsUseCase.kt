package io.novafoundation.nova.feature_wallet_impl.domain.asset

import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.AssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.incomingCrossChainDirectionsAvailable
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RealAssetGetOptionsUseCase(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val accountRepository: AccountRepository
) : AssetGetOptionsUseCase {

    private val changeNow = GetAssetOption.DApp(
        name = "ChangeNOW",
        url = "https://changenow.io/embeds/exchange-widget/v2/widget.html?from=assethub&to=tao&link_id=59409968ba3831",
        supportedTokens = listOf("DOT", "USDT", "USDC").map { it.asTokenSymbol() }
    )

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
        return chainAssetFlow.map { asset ->
            if (asset?.chainId == ChainGeneses.BITTENSOR && asset.type == Chain.Asset.Type.Native) {
                listOf(changeNow)
            } else {
                emptyList()
            }
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
