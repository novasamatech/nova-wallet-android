package io.novafoundation.nova.feature_staking_api.domain.model.relaychain

import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class StakingState(
    val chain: Chain,
    val chainAsset: Chain.Asset,
) {

    class NonStash(chain: Chain, chainAsset: Chain.Asset) : StakingState(chain, chainAsset)

    sealed class Stash(
        chain: Chain,
        chainAsset: Chain.Asset,
        val accountId: AccountId,
        val controllerId: AccountId,
        val stashId: AccountId,
    ) : StakingState(chain, chainAsset) {

        val accountAddress: String = chain.addressOf(accountId)

        val stashAddress = chain.addressOf(stashId)
        val controllerAddress = chain.addressOf(controllerId)

        class None(
            chain: Chain,
            chainAsset: Chain.Asset,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
        ) : Stash(chain, chainAsset, accountId, controllerId, stashId)

        class Validator(
            chain: Chain,
            chainAsset: Chain.Asset,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
            val prefs: ValidatorPrefs,
        ) : Stash(chain, chainAsset, accountId, controllerId, stashId)

        class Nominator(
            chain: Chain,
            chainAsset: Chain.Asset,
            accountId: AccountId,
            controllerId: AccountId,
            stashId: AccountId,
            val nominations: Nominations,
        ) : Stash(chain, chainAsset, accountId, controllerId, stashId)
    }
}
