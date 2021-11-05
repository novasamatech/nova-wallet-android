package io.novafoundation.nova.feature_staking_impl.domain.common

import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import java.math.BigInteger

fun Nominations.isWaiting(activeEraIndex: BigInteger): Boolean {
    return submittedInEra >= activeEraIndex
}
