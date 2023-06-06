package io.novafoundation.nova.caip

import io.novafoundation.nova.caip.caip19.RealCaip19Parser
import io.novafoundation.nova.caip.caip19.identifiers.AssetIdentifier
import io.novafoundation.nova.caip.caip19.identifiers.Caip19Identifier
import io.novafoundation.nova.caip.caip2.RealCaip2Parser
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.common.utils.requireValue
import org.junit.Assert.assertTrue
import org.junit.Test

class Caip19ParserTest {

    private val caip2Parser = RealCaip2Parser()
    private val caip19Parser = RealCaip19Parser(caip2Parser)

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
        return caip19Parser.parseCaip19(raw).requireValue()
    }
}
