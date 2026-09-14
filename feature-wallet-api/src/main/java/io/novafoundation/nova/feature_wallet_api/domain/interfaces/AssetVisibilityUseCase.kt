package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetVisibility
import io.novafoundation.nova.runtime.multiNetwork.chain.DefaultAssetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AssetVisibilityUseCase(
    private val accountRepository: AccountRepository,
    private val assetVisibilityRepository: AssetVisibilityRepository,
    private val defaultAssetsRepository: DefaultAssetsRepository
) {

    /**
     * Follows the selected wallet, so switching wallets re-evaluates the list against that
     * wallet's own rows rather than the previous one's.
     */
    fun visibilityFlow(): Flow<AssetVisibility> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            assetVisibilityRepository.visibilityFlow(metaAccount.id).map { userChoices ->
                AssetVisibility(defaults = defaultAssetsRepository.defaultAssetIds().toSet(), userChoices = userChoices)
            }
        }
    }
}
