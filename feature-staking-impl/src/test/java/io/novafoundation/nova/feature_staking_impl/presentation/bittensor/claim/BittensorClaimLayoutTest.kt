package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Layout contract only; device rendering is verified separately. */
class BittensorClaimLayoutTest {

    @Test
    fun `claim uses a primary amount and one notice without duplicate captions`() {
        val layout = sequenceOf(File("."), File("feature-staking-impl"))
            .map { File(it, "src/main/res/layout/fragment_bittensor_root_claim.xml") }
            .first { it.isFile }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val nodes = builder.parse(layout).getElementsByTagName("*")
        val elements = (0 until nodes.length).map { nodes.item(it) as Element }
        val ids = elements.map { it.getAttributeNS("http://schemas.android.com/apk/res/android", "id") }

        assertTrue(ids.indexOf("@+id/amount") < ids.indexOf("@+id/information"))
        assertTrue(ids.indexOf("@+id/validator") < ids.indexOf("@+id/stakeAfterClaim"))
        assertFalse(ids.any { it in listOf("@+id/forfeited", "@+id/remaining", "@+id/feeCaption") })
        assertEquals(
            "io.novafoundation.nova.feature_wallet_api.presentation.view.amount.PrimaryAmountView",
            elements[ids.indexOf("@+id/amount")].tagName
        )
        assertEquals(
            "io.novafoundation.nova.common.view.AlertView",
            elements[ids.indexOf("@+id/disclosure")].tagName
        )
        assertTrue(ids.contains("@+id/error"))
    }
}
