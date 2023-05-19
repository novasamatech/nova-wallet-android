package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetAndOption
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.alphabeticalOrder
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.relaychainsFirstAscendingOrder
import io.novafoundation.nova.runtime.ext.testnetsLastAscendingOrder
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AssetUseCaseImpl(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val sharedState: SingleAssetSharedState,
) : AssetUseCase {

    override fun currentAssetAndOptionFlow(): Flow<AssetAndOption> = combineToPair(
        accountRepository.selectedMetaAccountFlow(),
        sharedState.selectedOption,
    ).flatMapLatest { (selectedMetaAccount, selectedOption) ->
        val (_, chainAsset) = selectedOption.assetWithChain

        walletRepository.assetFlow(
            metaId = selectedMetaAccount.id,
            chainAsset = chainAsset
        ).map {
            AssetAndOption(it, selectedOption)
        }
    }

    override suspend fun availableAssetsToSelect(): List<AssetAndOption> = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val balancesByChainAssets = walletRepository.getSupportedAssets(metaAccount.id).associateBy { it.token.configuration.fullId }

        sharedState.availableToSelect().mapNotNull { supportedOption ->
            val asset = balancesByChainAssets[supportedOption.assetWithChain.asset.fullId]

            asset?.let { AssetAndOption(asset, supportedOption) }
        }
            .sortedWith(assetsComparator())
    }

    private fun assetsComparator(): Comparator<AssetAndOption> {
        return compareBy<AssetAndOption> { it.option.assetWithChain.chain.relaychainsFirstAscendingOrder }
            .thenBy { it.option.assetWithChain.chain.testnetsLastAscendingOrder }
            .thenByDescending { it.asset.token.amountToFiat(it.asset.transferable) }
            .thenByDescending { it.asset.transferable }
            .thenBy { it.option.assetWithChain.chain.alphabeticalOrder }
    }
}
