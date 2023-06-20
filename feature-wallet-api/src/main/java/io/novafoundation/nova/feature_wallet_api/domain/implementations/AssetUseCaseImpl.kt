package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetAndOption
import io.novafoundation.nova.feature_wallet_api.domain.GenericAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AssetUseCaseImpl<A>(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val sharedState: SelectedAssetOptionSharedState<A>,
) : GenericAssetUseCase<A> {

    override fun currentAssetAndOptionFlow(): Flow<AssetAndOption<A>> = combineToPair(
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
}
