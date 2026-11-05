package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novasama.substrate_sdk_android.extensions.fromHex
import org.junit.Assert.assertEquals
import org.junit.Test

class CrossChainJunctionsMapperTest {

    private val ethereum = Junction.GlobalConsensus(MultiLocation.NetworkId.Ethereum(chainId = 1))
    private val wethContract = Junction.AccountKey20(AccountIdKey("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2".fromHex()))

    @Test
    fun `maps ethereum global consensus`() {
        val remote = mapOf("globalConsensus" to mapOf("ethereum" to mapOf("chainId" to 1.0)))

        assertLocationEquals(AbsoluteMultiLocation(ethereum), remote.toAbsoluteLocation())
    }

    @Test
    fun `maps erc20 location regardless of key order`() {
        val remote = mapOf(
            "accountKey20" to "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2",
            "globalConsensus" to mapOf("ethereum" to mapOf("chainId" to 1.0)),
        )

        assertLocationEquals(AbsoluteMultiLocation(ethereum, wethContract), remote.toAbsoluteLocation())
    }

    @Test
    fun `keeps existing local shape`() {
        val remote = mapOf("parachainId" to 1000.0, "palletInstance" to 50.0, "generalIndex" to "1984")
        val expected = AbsoluteMultiLocation(
            Junction.ParachainId(1000),
            Junction.PalletInstance(50.toBigInteger()),
            Junction.GeneralIndex(1984.toBigInteger())
        )

        assertLocationEquals(expected, remote.toAbsoluteLocation())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects unsupported consensus`() {
        val remote = mapOf("globalConsensus" to mapOf("polkadot" to emptyMap<String, Any>()))

        remote.toAbsoluteLocation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects unknown junction key`() {
        val remote = mapOf("foo" to 2.0)

        remote.toAbsoluteLocation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects parents in absolute location`() {
        val remote = mapOf("parents" to 2.0, "globalConsensus" to mapOf("ethereum" to mapOf("chainId" to 1.0)))

        remote.toAbsoluteLocation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects accountKey20 of wrong length`() {
        val remote = mapOf("accountKey20" to "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2ff")

        remote.toAbsoluteLocation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects accountKey20 that is not hex`() {
        val remote = mapOf("accountKey20" to "0xnot-a-contract")

        remote.toAbsoluteLocation()
    }

    private fun assertLocationEquals(expected: AbsoluteMultiLocation, actual: AbsoluteMultiLocation) {
        assertEquals(expected.interior, actual.interior)
    }
}
