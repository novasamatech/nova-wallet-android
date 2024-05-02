package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_account_api.data.model.OnChainIdentity
import java.math.BigDecimal
import java.math.BigInteger

typealias Commission = BigDecimal

class ValidatorPrefs(val commission: Commission, val blocked: Boolean)

class Validator(
    val address: String,
    val slashed: Boolean,
    val accountIdHex: String,
    val prefs: ValidatorPrefs?,
    val electedInfo: ElectedInfo?,
    val identity: OnChainIdentity?,
    val isNovaValidator: Boolean
) : Identifiable {

    class ElectedInfo(
        val totalStake: BigInteger,
        val ownStake: BigInteger,
        val nominatorStakes: List<IndividualExposure>,
        val apy: BigDecimal,
        val isOversubscribed: Boolean
    )

    override val identifier: String = address
}
