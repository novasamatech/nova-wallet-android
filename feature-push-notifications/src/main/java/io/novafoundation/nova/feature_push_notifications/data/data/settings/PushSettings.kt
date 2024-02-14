package io.novafoundation.nova.feature_push_notifications.data.data.settings

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ethereumAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class PushSettings(
    val announcementsEnabled: Boolean,
    val sentTokensEnabled: Boolean,
    val receivedTokensEnabled: Boolean,
    val governanceState: List<GovernanceFeature>,
    val newReferenda: List<GovernanceFeature>,
    val wallets: List<Wallet>,
    val stakingReward: ChainFeature,
    val govMyDelegatorVoted: ChainFeature,
    val govMyReferendumFinished: ChainFeature
) {

    data class Wallet(
        val baseEthereumAccount: ByteArray?,
        val baseSubstrateAccount: ByteArray?,
        val chainAccounts: Map<ChainId, ByteArray>
    )

    class GovernanceFeature(val chainId: ChainId, val tracks: List<String>)

    sealed class ChainFeature {

        object All : ChainFeature()

        data class Concrete(val chainIds: List<ChainId>) : ChainFeature()
    }

    companion object {

        fun getDefault(wallets: List<Wallet> = emptyList()): PushSettings {
            return PushSettings(
                announcementsEnabled = true,
                sentTokensEnabled = true,
                receivedTokensEnabled = true,
                governanceState = emptyList(),
                newReferenda = emptyList(),
                wallets = wallets,
                stakingReward = ChainFeature.Concrete(emptyList()),
                govMyDelegatorVoted = ChainFeature.Concrete(emptyList()),
                govMyReferendumFinished = ChainFeature.Concrete(emptyList())
            )
        }
    }
}

fun PushSettings.ChainFeature.isNotEmpty(): Boolean {
    return when (this) {
        is PushSettings.ChainFeature.All -> false
        is PushSettings.ChainFeature.Concrete -> chainIds.isNotEmpty()
    }
}

fun PushSettings.isAnyGovEnabled(): Boolean {
    return governanceState.isNotEmpty() ||
        newReferenda.isNotEmpty() ||
        govMyDelegatorVoted.isNotEmpty() ||
        govMyReferendumFinished.isNotEmpty()
}

fun MetaAccount.toWalletSettings(): PushSettings.Wallet {
    return PushSettings.Wallet(
        baseEthereumAccount = ethereumAccountId(),
        baseSubstrateAccount = substrateAccountId,
        chainAccounts = chainAccounts.mapValues { (_, chainAccount) -> chainAccount.accountId }
    )
}
