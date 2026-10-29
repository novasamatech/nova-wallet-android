package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class RemoteToLocalChainMappersTest {

    private val gson = Gson()

    private val remote = ChainRemote(
        chainId = "chain",
        name = "Chain",
        assets = emptyList(),
        nodes = emptyList(),
        nodeSelectionStrategy = "uniform",
        explorers = emptyList(),
        externalApi = null,
        icon = "icon",
        addressPrefix = 42,
        legacyAddressPrefix = 0,
        types = null,
        options = setOf("fullSyncByDefault", "proxy", "multisig"),
        parentId = "parent",
        additional = null
    )

    @Test
    fun `integral config numbers and numeric strings retain their values`() {
        for (value in listOf(12.toByte(), 12.toShort(), 12, 12L, 12.0, 12f, BigInteger.valueOf(12), BigDecimal("12.00"), "12")) {
            val additional = mapNumericAdditional(value)

            assertEquals(value.toString(), 12, additional.stakingMaxElectingVoters)
            assertEquals(value.toString(), 12, additional.sessionLength)
            assertEquals(value.toString(), 12, additional.sessionsPerEra)
            assertEquals(value.toString(), 12L, additional.defaultBlockTimeMillis)
        }
    }

    @Test
    fun `int limits and immediately adjacent values do not wrap or saturate`() {
        for (boundary in listOf(Int.MIN_VALUE, Int.MAX_VALUE)) {
            for (value in listOf(boundary, boundary.toLong(), boundary.toDouble(), boundary.toString())) {
                val additional = mapNumericAdditional(value)

                assertEquals(boundary, additional.stakingMaxElectingVoters)
                assertEquals(boundary, additional.sessionLength)
                assertEquals(boundary, additional.sessionsPerEra)
                assertEquals(boundary.toLong(), additional.defaultBlockTimeMillis)
            }
        }

        for (outside in listOf(Int.MIN_VALUE.toLong() - 1, Int.MAX_VALUE.toLong() + 1)) {
            for (value in listOf(outside, outside.toDouble(), outside.toString())) {
                val additional = mapNumericAdditional(value)

                assertNull(value.toString(), additional.stakingMaxElectingVoters)
                assertNull(value.toString(), additional.sessionLength)
                assertNull(value.toString(), additional.sessionsPerEra)
                assertEquals(outside, additional.defaultBlockTimeMillis)
            }
        }
    }

    @Test
    fun `long limits are preserved but overflowing numbers are unavailable`() {
        for (boundary in listOf(Long.MIN_VALUE, Long.MAX_VALUE)) {
            for (value in listOf(boundary, boundary.toString(), boundary.toBigInteger(), boundary.toBigDecimal())) {
                assertEquals(boundary, mapNumericAdditional(value).defaultBlockTimeMillis)
            }
        }
        assertEquals(Long.MIN_VALUE, mapNumericAdditional(Long.MIN_VALUE.toDouble()).defaultBlockTimeMillis)
        assertEquals(Long.MIN_VALUE, mapNumericAdditional(Long.MIN_VALUE.toFloat()).defaultBlockTimeMillis)

        val belowMinimum = Long.MIN_VALUE.toBigInteger() - BigInteger.ONE
        val aboveMaximum = Long.MAX_VALUE.toBigInteger() + BigInteger.ONE
        // Long.MAX_VALUE rounds up to 2^63 as Double, and must not saturate back to Long.MAX_VALUE.
        for (value in listOf(belowMinimum, aboveMaximum, aboveMaximum.toString(), Long.MAX_VALUE.toDouble())) {
            assertNull(value.toString(), mapNumericAdditional(value).defaultBlockTimeMillis)
        }
    }

    @Test
    fun `fractional non finite and malformed numbers remain unavailable`() {
        val values = listOf(
            null,
            1.5,
            -1.5,
            1.5f,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            BigDecimal("1.1"),
            "1.0",
            "bad",
            true
        )

        for (value in values) {
            val additional = mapNumericAdditional(value)

            assertNull(value.toString(), additional.defaultBlockTimeMillis)
            assertNull(value.toString(), additional.stakingMaxElectingVoters)
            assertNull(value.toString(), additional.sessionLength)
            assertNull(value.toString(), additional.sessionsPerEra)
        }
        assertEquals(0, mapNumericAdditional(0.0).sessionLength)
        assertEquals(0L, mapNumericAdditional(0.0).defaultBlockTimeMillis)
    }

    @Test
    fun `new features retain additional metadata and chain preferences`() {
        val additional = mapOf(
            "newFeatures" to listOf("staking", "future", "staking"),
            "defaultTip" to "12345678901234567890",
            "themeColor" to "#abcdef",
            "stakingWiki" to "https://example.org/staking",
            "defaultBlockTime" to 6000.0,
            "relaychainAsNative" to true,
            "stakingMaxElectingVoters" to 20000.0,
            "feeViaRuntimeCall" to true,
            "supportsGenericLedgerApp" to true,
            "identityChain" to "identity",
            "disabledCheckMetadataHash" to false,
            "sessionLength" to 100.0,
            "sessionsPerEra" to "6",
            "timelineChain" to "timeline"
        )
        val oldChain = mapRemoteChainToLocal(remote, oldChain = null, ChainLocal.Source.DEFAULT, gson)
            .copy(connectionState = ChainLocal.ConnectionStateLocal.DISABLED)
        val local = mapRemoteChainToLocal(remote.copy(additional = additional), oldChain, ChainLocal.Source.CUSTOM, gson)
        val mapped = gson.fromJson(local.additional, Chain.Additional::class.java)
        val expected = Chain.Additional(
            defaultTip = "12345678901234567890".toBigInteger(),
            themeColor = "#abcdef",
            stakingWiki = "https://example.org/staking",
            defaultBlockTimeMillis = 6000L,
            relaychainAsNative = true,
            stakingMaxElectingVoters = 20000,
            feeViaRuntimeCall = true,
            supportLedgerGenericApp = true,
            identityChain = "identity",
            disabledCheckMetadataHash = false,
            sessionLength = 100,
            sessionsPerEra = 6,
            timelineChain = "timeline",
            newFeatures = setOf(Chain.NewFeature.STAKING)
        )

        assertEquals(expected, mapped)
        assertEquals(oldChain.copy(additional = local.additional, source = ChainLocal.Source.CUSTOM), local)
    }

    @Test
    fun `absent additional and malformed feature flags retain their contracts`() {
        val local = mapRemoteChainToLocal(remote, oldChain = null, ChainLocal.Source.DEFAULT, gson)
        assertNull(gson.fromJson(local.additional, Chain.Additional::class.java))

        val additional = mapAdditional(mapOf("newFeatures" to "staking", "sessionLength" to 10.0))
        assertEquals(emptySet<Chain.NewFeature>(), additional.newFeatures)
        assertEquals(10, additional.sessionLength)
    }

    @Test
    fun `Bittensor aliases map to the canonical staking type and wire name`() {
        for (alias in listOf("subtensor", "bittensor-root")) {
            assertEquals(Chain.Asset.StakingType.BITTENSOR_ROOT, mapStakingStringToStakingType(alias))
        }
        assertEquals("bittensor-root", mapStakingTypeToStakingString(Chain.Asset.StakingType.BITTENSOR_ROOT))
        for (type in Chain.Asset.StakingType.entries) {
            assertEquals(type, mapStakingStringToStakingType(mapStakingTypeToStakingString(type)))
        }
        assertEquals(Chain.Asset.StakingType.UNSUPPORTED, mapStakingStringToStakingType("future"))
    }

    private fun mapNumericAdditional(value: Any?): Chain.Additional {
        return mapAdditional(
            mapOf(
                "defaultBlockTime" to value,
                "stakingMaxElectingVoters" to value,
                "sessionLength" to value,
                "sessionsPerEra" to value
            )
        )
    }

    private fun mapAdditional(additional: Map<String, Any?>): Chain.Additional {
        val local = mapRemoteChainToLocal(remote.copy(additional = additional), oldChain = null, ChainLocal.Source.DEFAULT, gson)
        return gson.fromJson(local.additional, Chain.Additional::class.java)
    }
}
