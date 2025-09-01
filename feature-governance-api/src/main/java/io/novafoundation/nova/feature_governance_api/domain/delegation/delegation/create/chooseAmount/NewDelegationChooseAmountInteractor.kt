package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount

import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface NewDelegationChooseAmountInteractor {

    suspend fun maxAvailableBalanceToDelegate(asset: Asset): Balance

    fun delegateAssistantFlow(
        coroutineScope: CoroutineScope
    ): Flow<DelegateAssistant>

    suspend fun estimateFee(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>,
        shouldRemoveOtherTracks: Boolean,
    ): Fee

    suspend fun delegate(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>,
        shouldRemoveOtherTracks: Boolean,
    ): RetriableMultiResult<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>>
}
