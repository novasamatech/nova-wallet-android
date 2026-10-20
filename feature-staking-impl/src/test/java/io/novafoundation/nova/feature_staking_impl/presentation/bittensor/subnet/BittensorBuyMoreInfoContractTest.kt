package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Source-wiring regression checks: a formatted value alone must not replace the info action.
 * These checks intentionally do not claim to exercise Android view clicks or sheet rendering.
 */
class BittensorBuyMoreInfoContractTest {

    private val moduleDirectory = sequenceOf(File("."), File("feature-staking-impl"))
        .first { File(it, "src/main/res/layout/fragment_bittensor_subnet_review.xml").isFile }

    private val presentationDirectory = File(
        moduleDirectory,
        "src/main/java/io/novafoundation/nova/feature_staking_impl/presentation/bittensor/subnet"
    )

    @Test
    fun `buy more average price advertises the existing info action`() {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val layout = File(moduleDirectory, "src/main/res/layout/fragment_bittensor_subnet_review.xml")
        val document = builder.parse(layout)
        val elements = document.getElementsByTagName("io.novafoundation.nova.common.view.TableCellView")
        val cell = (0 until elements.length)
            .map { elements.item(it) as Element }
            .single { it.getAttributeNS(ANDROID_NAMESPACE, "id") == "@+id/projectedAveragePrice" }

        assertEquals("@string/bittensor_exit_avg_buy_price", cell.getAttributeNS(APP_NAMESPACE, "title"))
        assertEquals("@drawable/ic_info", cell.getAttributeNS(APP_NAMESPACE, "titleIcon"))
    }

    @Test
    fun `buy more confirmation connects the description to the existing sheet`() {
        val fragment = source("BittensorSubnetReviewFragment.kt")

        assertTrue(fragment.contains("binder.projectedAveragePrice.isVisible = viewModel.existingPosition"))
        assertTrue(
            Regex(
                """if \(viewModel\.existingPosition\) \{\s*""" +
                    """viewModel\.averagePriceDescription\.observe \{ description ->\s*""" +
                    """projectedAveragePrice\.setOnClickListener \{\s*""" +
                    """description\?\.let \{ showDescription\(R\.string\.bittensor_exit_avg_buy_price, it\) \}"""
            ).containsMatchIn(fragment)
        )
        assertTrue(fragment.contains("DescriptionBottomSheet(requireContext(), getString(titleRes), description)"))
    }

    @Test
    fun `confirmation uses the amount sheet copy with current subnet symbol and name`() {
        val amount = source("BittensorSubnetAmountViewModel.kt")
        val review = source("BittensorSubnetReviewViewModel.kt")
        val description = "resources.getString(R.string.bittensor_average_price_description, subnet.symbol, subnet.name)"

        assertTrue(
            "Amount description must use the current subnet symbol and name",
            Regex(
                """val averagePriceDescription = subnet\.map \{ subnet ->\s*""" +
                    """subnet\?\.let \{\s*""" +
                    Regex.escape("resources.getString(R.string.bittensor_average_price_description, it.symbol, it.name)")
            ).containsMatchIn(amount)
        )
        assertTrue(
            "Confirmation description must use its prepared subnet symbol and name",
            Regex(
                """val averagePriceDescription = preparationState\.map \{ preparation ->\s*""" +
                    """preparation\.dataOrNull\?\.subnet\?\.let \{ subnet ->\s*""" +
                    Regex.escape(description)
            ).containsMatchIn(review)
        )
    }

    private fun source(name: String): String = File(presentationDirectory, name).readText()

    private companion object {

        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"
    }
}
