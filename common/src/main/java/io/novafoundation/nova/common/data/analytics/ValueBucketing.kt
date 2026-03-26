package io.novafoundation.nova.common.data.analytics

import java.math.BigDecimal

enum class AmountBucket(val value: String) {
    UNDER_1("under_1"),
    FROM_1_TO_10("1_to_10"),
    FROM_10_TO_100("10_to_100"),
    FROM_100_TO_1K("100_to_1k"),
    FROM_1K_TO_10K("1k_to_10k"),
    FROM_10K_TO_100K("10k_to_100k"),
    OVER_100K("over_100k");

    companion object {
        fun from(usdAmount: BigDecimal): AmountBucket = when {
            usdAmount < BigDecimal.ONE -> UNDER_1
            usdAmount < BigDecimal.TEN -> FROM_1_TO_10
            usdAmount < BigDecimal(100) -> FROM_10_TO_100
            usdAmount < BigDecimal(1000) -> FROM_100_TO_1K
            usdAmount < BigDecimal(10000) -> FROM_1K_TO_10K
            usdAmount < BigDecimal(100_000) -> FROM_10K_TO_100K
            else -> OVER_100K
        }

        fun from(usdValue: Double): AmountBucket = from(BigDecimal.valueOf(usdValue))
    }
}

enum class DurationBucket(val value: String) {
    UNDER_5S("under_5s"),
    FROM_5S_TO_15S("5s_to_15s"),
    FROM_15S_TO_30S("15s_to_30s"),
    FROM_30S_TO_60S("30s_to_60s"),
    FROM_1M_TO_5M("1m_to_5m"),
    OVER_5M("over_5m");

    companion object {
        fun from(milliseconds: Long): DurationBucket {
            val seconds = milliseconds / 1000
            return when {
                seconds < 5 -> UNDER_5S
                seconds < 15 -> FROM_5S_TO_15S
                seconds < 30 -> FROM_15S_TO_30S
                seconds < 60 -> FROM_30S_TO_60S
                seconds < 300 -> FROM_1M_TO_5M
                else -> OVER_5M
            }
        }

        fun from(seconds: Double): DurationBucket = from((seconds * 1000).toLong())
    }
}

enum class SlippageBucket(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CUSTOM("custom");

    companion object {
        fun from(percentage: Double): SlippageBucket = when {
            percentage <= 0.5 -> LOW
            percentage <= 1.0 -> MEDIUM
            percentage <= 3.0 -> HIGH
            else -> CUSTOM
        }

        fun from(slippagePercent: BigDecimal): SlippageBucket = from(slippagePercent.toDouble())
    }
}
