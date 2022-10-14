package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.PreImagePreviewPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumCallPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumFullDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.ReferendumProposerPayload
import java.math.BigInteger

class ReferendumDetailsViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumDetailsPayload
) : BaseViewModel() {

    fun backClicked() {
        router.back()
    }

    fun openFullDetails() {
        router.openReferendumDetails(
            ReferendumFullDetailsPayload(
                ReferendumProposerPayload(ByteArray(32), "null"),
                "null",
                "null",
                "null",
                ByteArray(32),
                BigInteger.valueOf(11113290000100),
                BigInteger.valueOf(3244100020000),
                BigInteger.valueOf(41391303001000),
                ReferendumCallPayload.TreasuryRequest(BigInteger.ZERO, ByteArray(32)),
                PreImagePreviewPayload.TooLong
            )
        )
    }
}
