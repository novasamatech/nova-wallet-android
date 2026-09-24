package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId

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
     * Shows the token in every wallet that holds [recipientAccountId] on [chain]. The receiver is not
     * always the sending wallet - a transfer to another of the user's own wallets has to show the token
     * over there - and the same account can be imported into several wallets at once.
     */
    suspend fun onOwnFundsReceived(recipientAccountId: AccountId, chain: Chain, assetId: FullChainAssetId) {
        accountRepository.getActiveMetaAccounts()
            .filter { it.accountIdIn(chain)?.contentEquals(recipientAccountId) == true }
            .forEach { show(it.id, assetId) }
    }

    /**
     * For operations that can only ever deliver into the wallet performing them, such as a swap.
     */
    suspend fun onOwnFundsReceivedBySelectedWallet(assetId: FullChainAssetId) {
        show(accountRepository.getSelectedMetaAccount().id, assetId)
    }

    private suspend fun show(metaId: Long, assetId: FullChainAssetId) {
        assetVisibilityRepository.setVisibility(metaId, mapOf(assetId to true))
    }
}
