package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount

import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

interface DelegateAssistant {

    suspend fun estimateLocksAfterDelegating(amount: Balance, conviction: Conviction, asset: Asset): LocksChange

    suspend fun reusableLocks(): List<ReusableLock>
}
