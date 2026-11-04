package io.novafoundation.nova.feature_wallet_impl.presentation.getAsset

import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.common.utils.asTokenSymbol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAssetButtonVisibilityTest {

    @Test
    fun `Bittensor funding action remains visible without any available options`() {
        assertTrue(shouldShowGetAssetButton(true, emptySet(), showWhenNoOptions = true))
    }

    @Test
    fun `default behavior still hides an action without available options`() {
        assertFalse(shouldShowGetAssetButton(true, emptySet(), showWhenNoOptions = false))
    }

    @Test
    fun `additional visibility filter still applies to Bittensor`() {
        assertFalse(shouldShowGetAssetButton(false, emptySet(), showWhenNoOptions = true))
    }

    @Test
    fun `available receive action remains visible with the default policy`() {
        assertTrue(shouldShowGetAssetButton(true, setOf(GetAssetOption.Receive), showWhenNoOptions = false))
    }

    @Test
    fun `a dApp option alone keeps the funding action visible`() {
        val dApp = GetAssetOption.DApp("ExampleSwap", "https://swap.example/buy", listOf("DOT".asTokenSymbol()))

        assertTrue(shouldShowGetAssetButton(true, setOf(dApp), showWhenNoOptions = false))
    }
}
