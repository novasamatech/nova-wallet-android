package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

class AssetUseCaseImpl(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val sharedState: SingleAssetSharedState
) : AssetUseCase {

    override fun currentAssetFlow() = combine(
        accountRepository.selectedMetaAccountFlow(),
        sharedState.assetWithChain,
        ::Pair
    ).flatMapLatest { (selectedMetaAccount, chainAndAsset) ->
        val (_, chainAsset) = chainAndAsset

        walletRepository.assetFlow(
            metaId = selectedMetaAccount.id,
            chainAsset = chainAsset
        )
    }

    override suspend fun availableAssetsToSelect(): List<Asset> = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val availableChainAssets = sharedState.availableToSelect().toSet()

        walletRepository.getAssets(metaAccount.id).filter {
            it.token.configuration in availableChainAssets
        }.sortedByDescending { it.token.fiatAmount(it.transferable) }
    }
}
