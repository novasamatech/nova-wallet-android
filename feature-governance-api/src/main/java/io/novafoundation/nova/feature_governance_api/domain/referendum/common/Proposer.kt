package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novasama.substrate_sdk_android.runtime.AccountId

data class ReferendumProposer(val accountId: AccountId, val offChainNickname: String?)
