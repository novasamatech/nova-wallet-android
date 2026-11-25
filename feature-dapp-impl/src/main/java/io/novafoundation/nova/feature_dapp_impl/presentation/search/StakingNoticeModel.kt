package io.novafoundation.nova.feature_dapp_impl.presentation.search

import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingSiteKind

class StakingNoticeModel(
    val kind: DAppStakingSiteKind,
    val host: String
)
