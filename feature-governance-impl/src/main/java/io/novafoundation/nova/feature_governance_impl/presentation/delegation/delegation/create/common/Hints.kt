package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common

import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.coroutines.CoroutineScope

fun ResourcesHintsMixinFactory.newDelegationHints(coroutineScope: CoroutineScope) = create(
    coroutineScope = coroutineScope,
    hintsRes = listOf(R.string.delegation_delegate_hint_vote, R.string.delegation_delegate_hint_unlock)
)

fun ResourcesHintsMixinFactory.revokeDelegationHints(coroutineScope: CoroutineScope) = create(
    coroutineScope = coroutineScope,
    hintsRes = listOf(R.string.delegation_revoke_hint_unlock)
)
