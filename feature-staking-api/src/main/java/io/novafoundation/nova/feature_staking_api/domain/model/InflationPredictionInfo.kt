package io.novafoundation.nova.feature_staking_api.domain.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class InflationPredictionInfo(
    val nextMint: NextMint
) {

    class NextMint(
        val toStakers: Balance,
        val toTreasury: Balance
    )

    companion object {

        fun fromDecoded(decoded: Any?): InflationPredictionInfo {
            val asStruct = decoded.castToStruct()

            return InflationPredictionInfo(
                nextMint = bindNextMint(asStruct["nextMint"])
            )
        }

        private fun bindNextMint(decoded: Any?): NextMint {
            val (toStakersRaw, toTreasuryRaw) = decoded.castToList()

            return NextMint(
                toStakers = bindNumber(toStakersRaw),
                toTreasury = bindNumber(toTreasuryRaw)
            )
        }
    }
}

fun InflationPredictionInfo.calculateStakersInflation(totalIssuance: Balance, eraDuration: Duration): Double {
    val periodsInYear = (365.days / eraDuration).roundToInt()
    val inflationPerMint = nextMint.toStakers.divideToDecimal(totalIssuance)

    return inflationPerMint.toDouble() * periodsInYear
}
