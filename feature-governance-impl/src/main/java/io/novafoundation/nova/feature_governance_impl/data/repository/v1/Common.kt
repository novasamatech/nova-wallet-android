package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId
import java.math.BigInteger

val DemocracyTrackId = TrackId(BigInteger.ZERO)
val DEMOCRACY_ID = BalanceLockId.fromFullId("democrac")
