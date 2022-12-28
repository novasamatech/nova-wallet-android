package io.novafoundation.nova.feature_governance_api.domain.delegation.delegate

import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface Delegate {

    val accountId: AccountId

    val stats: DelegateStats

    val metadata: Metadata?

    interface Metadata {

        val accountType: DelegateAccountType
    }
}
