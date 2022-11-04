package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import jp.co.soramitsu.fearless_utils.runtime.AccountId

data class ReferendumProposer(val accountId: AccountId, val offChainNickname: String?)
