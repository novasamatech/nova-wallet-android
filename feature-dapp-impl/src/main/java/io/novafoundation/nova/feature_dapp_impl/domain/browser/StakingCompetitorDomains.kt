package io.novafoundation.nova.feature_dapp_impl.domain.browser

import io.novafoundation.nova.common.utils.Urls

object StakingCompetitorDomains {

    private val BLOCKED_DOMAINS = listOf(
        "staking.polkadot.cloud",
        "polkadot.cloud",
        "app.bifrost.io",
        "omni.ls",
        "portal.invarch.network",
        "capitaldex.exchange",
        "unique.network",
        "apps.karura.network",
        "apps.acala.network",
        "farm.acala.network",
        "hub.ternoa.network",
        "mentatminds.com",
        "dash.taostats.io",
        "tensorwallet.ca",
        "staking.polkadot.network"
    )

    fun isStakingCompetitor(url: String): Boolean {
        return runCatching {
            val host = Urls.hostOf(url)
            BLOCKED_DOMAINS.any { domain -> host == domain || host.endsWith(".$domain") }
        }.getOrDefault(false)
    }
}
