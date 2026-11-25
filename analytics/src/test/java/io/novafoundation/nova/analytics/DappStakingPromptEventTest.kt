package io.novafoundation.nova.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DappStakingPromptEventTest {

    @Test
    fun `search banner shown carries placement and kind without a host`() {
        val event = AnalyticsEvent.DappStakingPromptShown(DappStakingPromptPlacement.SEARCH_BANNER, DappStakingPromptKind.BITTENSOR)

        assertEquals("dapp_staking_prompt_shown", event.name)
        assertEquals(mapOf("placement" to "search_banner", "kind" to "bittensor"), event.properties)
        assertFalse(event.properties.containsKey("dapp_host"))
    }

    @Test
    fun `notice shown carries the host`() {
        val event = AnalyticsEvent.DappStakingPromptShown(DappStakingPromptPlacement.NOTICE, DappStakingPromptKind.STAKING, "staking.polkadot.cloud")

        assertEquals("dapp_staking_prompt_shown", event.name)
        assertEquals(
            mapOf("placement" to "notice", "kind" to "staking", "dapp_host" to "staking.polkadot.cloud"),
            event.properties
        )
    }

    @Test
    fun `search banner action carries the action without a host`() {
        val event = AnalyticsEvent.DappStakingPromptAction(
            DappStakingPromptPlacement.SEARCH_BANNER,
            DappStakingPromptKind.STAKING,
            DappStakingPromptAction.GO_TO_STAKING
        )

        assertEquals("dapp_staking_prompt_action", event.name)
        assertEquals(mapOf("placement" to "search_banner", "kind" to "staking", "action" to "go_to_staking"), event.properties)
        assertFalse(event.properties.containsKey("dapp_host"))
    }

    @Test
    fun `notice action carries the action and the host`() {
        val event = AnalyticsEvent.DappStakingPromptAction(
            DappStakingPromptPlacement.NOTICE,
            DappStakingPromptKind.BITTENSOR,
            DappStakingPromptAction.CONTINUE_TO_SITE,
            "taostats.io"
        )

        assertEquals("dapp_staking_prompt_action", event.name)
        assertEquals(
            mapOf("placement" to "notice", "kind" to "bittensor", "action" to "continue_to_site", "dapp_host" to "taostats.io"),
            event.properties
        )
    }

    @Test
    fun `prompt enums keep their wire values`() {
        assertEquals(listOf("search_banner", "notice"), DappStakingPromptPlacement.values().map { it.value })
        assertEquals(listOf("bittensor", "staking"), DappStakingPromptKind.values().map { it.value })
        assertEquals(listOf("go_to_staking", "continue_to_site"), DappStakingPromptAction.values().map { it.value })
    }

    @Test
    fun `staking flow sources of the prompts keep their wire values`() {
        assertEquals("dapp_search_banner", StakingFlowSource.DAPP_SEARCH_BANNER.value)
        assertEquals("dapp_notice", StakingFlowSource.DAPP_NOTICE.value)
        assertEquals("deep_link", StakingFlowSource.DEEP_LINK.value)
    }
}
