package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.FixedPrecisionFormatter
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class BittensorPortfolioRowMapper(
    private val resources: ResourceManager,
    private val amountFormatter: BittensorAmountFormatter,
    private val assetIconProvider: AssetIconProvider
) {

    fun map(
        portfolio: BittensorPortfolio,
        metadata: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BittensorSubnet>>,
        rootRewards: BigInteger?,
        rootApy: ExtendedLoadingState<BigDecimal?>,
        weeklyChanges: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>
    ): List<BittensorPositionUi> {
        val positions = portfolio.positions ?: return emptyList()
        return buildList {
            if (hasRootPortfolioRow(positions, rootRewards)) add(rootRow(portfolio, rootRewards, rootApy))
            positions.subnets.map { it.subnetId }.distinct().sortedBy { it.value }.forEach { subnetId ->
                add(subnetRow(portfolio, subnetId, metadata, weeklyChanges))
            }
        }
    }

    private fun rootRow(portfolio: BittensorPortfolio, rewards: BigInteger?, apy: ExtendedLoadingState<BigDecimal?>): BittensorPositionUi {
        // Without root stake the row exists only for claimable rewards; an APY of nothing explains nothing.
        val rewardsOnly = !portfolio.positions?.rootStake.isPositiveAmount() && rewards.isPositiveAmount()
        val subtitle = if (rewardsOnly) {
            resources.getString(R.string.bittensor_portfolio_root_claimable, formatAmount(rewards, portfolio).token)
        } else {
            apy.dataOrNull?.let { resources.getString(R.string.bittensor_portfolio_root_apy, it.formatAsPercentage()) }
        }
        return BittensorPositionUi(
            id = "root",
            title = resources.getString(R.string.bittensor_root_staking),
            subtitle = subtitle,
            amount = formatAmount(portfolio.positions?.rootStake, portfolio),
            icon = assetIconProvider.getAssetIconOrFallback(portfolio.asset.token.configuration),
            action = BittensorPositionAction.RootDetails,
            isSubtitleLoading = !rewardsOnly && apy.isLoading()
        )
    }

    private fun subnetRow(
        portfolio: BittensorPortfolio,
        subnetId: BittensorNetuid.Subnet,
        metadata: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BittensorSubnet>>,
        weeklyChanges: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BigDecimal>>
    ): BittensorPositionUi {
        val subnet = metadata.dataOrNull?.get(subnetId)
        val fallback = resources.getString(R.string.bittensor_subnet_number, subnetId.value)
        val alpha = portfolio.positions?.subnets.orEmpty().filter { it.subnetId == subnetId }.sumOf { it.alphaInPlanks }
        val subtitle = if (metadata.isLoading()) {
            null
        } else {
            amountFormatter.amount(
                BigDecimal(alpha, BITTENSOR_ALPHA_DECIMALS),
                TokenSymbol(subnet?.symbol?.takeIf { it.isNotBlank() } ?: "—"),
                null,
                portfolio.asset.token.currency
            ).token.ltrIsolated()
        }
        val change = if (subnet == null) {
            null
        } else {
            weeklyChanges.dataOrNull?.get(subnetId)?.let {
                BittensorPositionChange(
                    resources.getString(
                        R.string.bittensor_portfolio_weekly_change,
                        it.formatAsSingleDecimalChange(),
                        resources.getString(R.string.bittensor_chart_week)
                    ),
                    it.isNonNegative
                )
            }
        }
        return BittensorPositionUi(
            id = "subnet-${subnetId.value}",
            title = subnetPositionTitle(subnet, fallback),
            subtitle = subtitle,
            amount = subnetAmount(portfolio.positions?.subnetStake(subnetId), portfolio),
            icon = subnet?.logoUrl?.asUrlIcon() ?: Icon.FromDrawableRes(R.drawable.ic_fallback_network_icon),
            action = BittensorPositionAction.SubnetDetails(subnetId),
            isSubtitleLoading = metadata.isLoading(),
            change = change,
            isChangeLoading = subnet != null && weeklyChanges.isLoading()
        )
    }

    /** A subnet stake is valued at the current pool price, so it is an estimate: "≈ 0.01993 TAO" without fiat (iOS). */
    private fun subnetAmount(amount: BigInteger?, portfolio: BittensorPortfolio): AmountModel {
        val formatted = formatAmount(amount, portfolio)
        val token = if (amount == null) formatted.token else "≈ ${formatted.token}"
        return AmountModel(token, null)
    }

    private fun formatAmount(amount: BigInteger?, portfolio: BittensorPortfolio): AmountModel {
        if (amount == null) return AmountModel("—", null)
        val token = portfolio.asset.token
        val decimal = token.amountFromPlanks(amount)
        return amountFormatter.amount(decimal, token.configuration.symbol, token.toFiatOrNull(decimal), token.currency)
    }
}

/** [rewards] are claimable root rewards: amounts below the claim minimum do not keep the row, as on iOS. */
internal fun hasRootPortfolioRow(positions: BittensorPortfolioPositions, rewards: BigInteger?): Boolean =
    positions.rootStake.isPositiveAmount() || rewards.isPositiveAmount()

private val singleDecimalFormatter = FixedPrecisionFormatter(1)

/** Signed change with one decimal, as iOS `signedPercentSingle`: "+16.4%". */
internal fun BigDecimal.formatAsSingleDecimalChange(): String {
    val prefix = if (isNonNegative) "+" else ""
    return prefix + singleDecimalFormatter.format(this, RoundingMode.HALF_EVEN) + "%"
}

private fun BigInteger?.isPositiveAmount(): Boolean = this != null && signum() > 0

internal fun isPortfolioEmpty(positions: BittensorPortfolioPositions?, rewards: BigInteger?): Boolean =
    positions != null && rewards != null && !hasRootPortfolioRow(positions, rewards) && positions.subnets.isEmpty()
