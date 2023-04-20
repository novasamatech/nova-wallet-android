package io.novafoundation.nova.web3names

import io.novafoundation.nova.common.utils.requireValue
import io.novafoundation.nova.web3names.data.caip19.Caip19Parser
import io.novafoundation.nova.web3names.data.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Identifier
import org.junit.Test
import org.junit.Assert.assertTrue

class Caip19ParserTest {

    private val parser = Caip19Parser()

    @Test
    fun `test substrate chain namespace`() {
        val identifier = getIdentifier("polkadot:0x0/slip44:10")
        assertInstance<Caip2Identifier.Polkadot>(identifier.caip2Identifier)
    }

    @Test
    fun `test ethereum chain namespace`() {
        val identifier = getIdentifier("eip155:1/slip44:10")
        assertInstance<Caip2Identifier.Eip155>(identifier.caip2Identifier)
    }

    @Test
    fun `test slip44 asset namespace`() {
        val identifier = getIdentifier("polkadot:0x0/slip44:10")
        assertInstance<AssetIdentifier.Slip44>(identifier.assetIdentifier)
    }

    @Test
    fun `test erc20 asset namespace`() {
        val identifier = getIdentifier("polkadot:0x0/erc20:10")
        assertInstance<AssetIdentifier.Erc20>(identifier.assetIdentifier)
    }

    private inline fun <reified T> assertInstance(value: Any?) {
        assertTrue(value is T)
    }

    private fun getIdentifier(raw: String): Caip19Identifier {
        return parser.parseCaip19(raw).requireValue()
    }
}
