package io.novafoundation.nova.feature_staking_api.domain.model.relaychain

import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class StakingState(
    val chain: Chain,
    val accountId: AccountId
) {

    val accountAddress: String = chain.addressOf(accountId)

    class NonStash(chain: Chain, accountId: AccountId) : StakingState(chain, accountId)

    sealed class Stash(
        chain: Chain,
        accountId: AccountId,
        val controllerId: AccountId,
        val stashId: AccountId,
    ) : StakingState(chain, accountId) {

        val stashAddress = chain.addressOf(stashId)
        val controllerAddress = chain.addressOf(controllerId)

        class None(
            chain: Chain,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
        ) : Stash(chain, accountId, controllerId, stashId)

        class Validator(
            chain: Chain,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
            val prefs: ValidatorPrefs,
        ) : Stash(chain, accountId, controllerId, stashId)

        class Nominator(
            chain: Chain,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
            val nominations: Nominations,
        ) : Stash(chain, accountId, controllerId, stashId)
    }
}
