package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetAndOption
import io.novafoundation.nova.feature_wallet_api.domain.GenericAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.SelectableAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.alphabeticalOrder
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.relaychainsFirstAscendingOrder
import io.novafoundation.nova.runtime.ext.testnetsLastAscendingOrder
import io.novafoundation.nova.runtime.state.SelectableAssetAdditionalData
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelectableAssetUseCaseImpl<A : SelectableAssetAdditionalData>(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val sharedState: SelectableSingleAssetSharedState<A>,
) : GenericAssetUseCase<A> by AssetUseCaseImpl(walletRepository, accountRepository, sharedState),
    SelectableAssetUseCase<A> {

    override suspend fun availableAssetsToSelect(): List<AssetAndOption<A>> = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val balancesByChainAssets = walletRepository.getSupportedAssets(metaAccount.id).associateBy { it.token.configuration.fullId }

        sharedState.availableToSelect()
            .filter { it.assetWithChain.chain.enabled }
            .mapNotNull { supportedOption ->
                val asset = balancesByChainAssets[supportedOption.assetWithChain.asset.fullId]

                asset?.let { AssetAndOption(asset, supportedOption) }
            }
            .sortedWith(assetsComparator())
    }

    private fun assetsComparator(): Comparator<AssetAndOption<A>> {
        return compareBy<AssetAndOption<A>> { it.option.assetWithChain.chain.relaychainsFirstAscendingOrder }
            .thenBy { it.option.assetWithChain.chain.testnetsLastAscendingOrder }
            .thenByDescending { it.asset.token.amountToFiat(it.asset.transferable) }
            .thenByDescending { it.asset.transferable }
            .thenBy { it.option.assetWithChain.chain.alphabeticalOrder }
    }
}
