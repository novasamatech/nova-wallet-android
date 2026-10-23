package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.AvailableStakingOptionsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

enum class BittensorPortfolioEntry { PORTFOLIO, DEEP_LINK }

sealed interface BittensorEntryResolution {
    object Portfolio : BittensorEntryResolution

    class Start(val payload: StartStakingLandingPayload, val network: String) : BittensorEntryResolution
}

internal fun resolveBittensorEntry(portfolio: BittensorPortfolio?, claims: BittensorRootClaimState?): BittensorEntryResolution? {
    if (portfolio?.dataStatus !is BittensorDataStatus.Fresh) return null
    val positions = portfolio.positions ?: return null
    if (positions.root.isNotEmpty() || positions.subnets.isNotEmpty()) return BittensorEntryResolution.Portfolio
    if (claims?.dataStatus !is BittensorDataStatus.Fresh || claims.metaId != portfolio.metaId) return null
    if (!isPortfolioEmpty(positions, claims.claims.totalClaimable)) return BittensorEntryResolution.Portfolio

    val options = AvailableStakingOptionsPayload(portfolio.chain.id, portfolio.asset.token.configuration.id, listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))
    return BittensorEntryResolution.Start(StartStakingLandingPayload(options), portfolio.chain.name)
}
