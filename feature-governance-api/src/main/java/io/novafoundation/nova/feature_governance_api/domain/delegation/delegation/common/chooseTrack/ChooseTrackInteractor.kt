package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ChooseTrackInteractor {

    suspend fun isAllowedToShowRemoveVotesSuggestion(): Boolean

    suspend fun disallowShowRemoveVotesSuggestion()

    fun observeNewDelegationTrackData(): Flow<ChooseTrackData>

    fun observeEditDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData>

    fun observeRevokeDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData>
}
