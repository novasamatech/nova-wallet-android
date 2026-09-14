package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * Shows a token one of the user's wallets has just received their own funds into.
 *
 * Writes the decision outright rather than leaving it to balance discovery, and deliberately
 * overrides an earlier hide - funds sitting in a token the wallet refuses to show read as lost
 * money, and the user moving them there is a clearer signal than the hide that came before it.
 */
class ShowReceivedAssetUseCase(
    private val accountRepository: AccountRepository,
    private val assetVisibilityRepository: AssetVisibilityRepository
) {

    /**
     * @param metaId the wallet the funds landed in, which is not always the one sending them -
     * a transfer to another of the user's own wallets has to show the token over there.
     */
    suspend fun onOwnFundsReceived(metaId: Long, assetId: FullChainAssetId) {
        assetVisibilityRepository.setVisibility(metaId, mapOf(assetId to true))
    }

    /**
     * For operations that can only ever deliver into the wallet performing them, such as a swap.
     */
    suspend fun onOwnFundsReceivedBySelectedWallet(assetId: FullChainAssetId) {
        onOwnFundsReceived(accountRepository.getSelectedMetaAccount().id, assetId)
    }
}
