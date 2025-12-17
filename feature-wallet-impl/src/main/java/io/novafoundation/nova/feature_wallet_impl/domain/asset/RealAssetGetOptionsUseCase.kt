package io.novafoundation.nova.feature_wallet_impl.domain.asset

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_wallet_api.domain.AssetGetOptionsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.incomingCrossChainDirectionsAvailable
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RealAssetGetOptionsUseCase(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val accountRepository: AccountRepository
) : AssetGetOptionsUseCase {

    override fun observeAssetGetOptionsForSelectedAccount(chainAssetFlow: Flow<Chain.Asset?>): Flow<Set<GetAssetOption>> {
        return combine(
            crossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(chainAssetFlow),
            receiveAvailable(chainAssetFlow),
            buyAvailable(chainAssetFlow),
        ) { crossChainTransfersAvailable, receiveAvailable, buyAvailable ->
            setOfNotNull(
                GetAssetOption.CROSS_CHAIN.takeIf { crossChainTransfersAvailable },
                GetAssetOption.RECEIVE.takeIf { receiveAvailable },
                GetAssetOption.BUY.takeIf { buyAvailable }
            )
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
