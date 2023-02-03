package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface NewDelegationChooseAmountInteractor {

    fun delegateAssistantFlow(
        coroutineScope: CoroutineScope
    ): Flow<DelegateAssistant>

    suspend fun estimateFee(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>,
    ): Balance

    suspend fun delegate(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>,
    ): Result<ExtrinsicStatus.InBlock>
}
