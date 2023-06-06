package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_staking_api.domain.model.NominatedValidator
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.APYSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.TotalStakeSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.ValidatorOwnStakeSorting
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorAlert
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorDetailsModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorStakeModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel.Active.UserStakeInfo
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import java.math.BigDecimal
import java.math.BigInteger

private const val ICON_SIZE_DP = 24

suspend fun mapValidatorToValidatorModel(
    chain: Chain,
    validator: Validator,
    iconGenerator: AddressIconGenerator,
    token: Token,
    isChecked: Boolean? = null,
    sorting: RecommendationSorting = APYSorting,
) = mapValidatorToValidatorModel(
    chain = chain,
    validator = validator,
    createIcon = { iconGenerator.createAddressModel(it, ICON_SIZE_DP, validator.identity?.display, AddressIconGenerator.BACKGROUND_TRANSPARENT) },
    token = token,
    isChecked = isChecked,
    sorting = sorting
)

suspend fun mapValidatorToValidatorModel(
    chain: Chain,
    validator: Validator,
    createIcon: suspend (address: String) -> AddressModel,
    token: Token,
    isChecked: Boolean? = null,
    sorting: RecommendationSorting = APYSorting,
): ValidatorModel {
    val address = chain.addressOf(validator.accountIdHex.fromHex())
    val addressModel = createIcon(address)

    return with(validator) {
        val scoring = when (sorting) {
            APYSorting -> rewardsToScoring(electedInfo?.apy)

            TotalStakeSorting -> stakeToScoring(electedInfo?.totalStake, token)

            ValidatorOwnStakeSorting -> stakeToScoring(electedInfo?.ownStake, token)

            else -> throw NotImplementedError("Unsupported sorting: $sorting")
        }

        ValidatorModel(
            accountIdHex = accountIdHex,
            slashed = slashed,
            addressModel = addressModel,
            scoring = scoring,
            isChecked = isChecked,
            stakeTarget = validator,
            subtitle = null // TODO relaychain subtitles
        )
    }
}

fun rewardsToScoring(rewardsGain: BigDecimal?) = rewardsToColoredText(rewardsGain)?.let(StakeTargetModel.Scoring::OneField)

fun rewardsToColoredText(rewardsGain: BigDecimal?) = formatStakeTargetRewardsOrNull(rewardsGain)?.let {
    StakeTargetModel.ColoredText(it, R.color.text_positive)
}

fun stakeToScoring(stakeInPlanks: BigInteger?, token: Token): StakeTargetModel.Scoring.TwoFields? {
    if (stakeInPlanks == null) return null

    val stake = token.amountFromPlanks(stakeInPlanks)

    return StakeTargetModel.Scoring.TwoFields(
        primary = stake.formatTokenAmount(token.configuration),
        secondary = token.amountToFiat(stake).formatAsCurrency(token.currency)
    )
}

fun mapValidatorToValidatorDetailsParcelModel(
    validator: Validator,
): StakeTargetDetailsParcelModel {
    return mapValidatorToValidatorDetailsParcelModel(validator, nominationStatus = null)
}

fun mapValidatorToValidatorDetailsWithStakeFlagParcelModel(
    nominatedValidator: NominatedValidator,
): StakeTargetDetailsParcelModel = mapValidatorToValidatorDetailsParcelModel(nominatedValidator.validator, nominatedValidator.status)

private fun mapValidatorToValidatorDetailsParcelModel(
    validator: Validator,
    nominationStatus: NominatedValidator.Status?,
): StakeTargetDetailsParcelModel {
    return with(validator) {
        val identityModel = identity?.let(::mapIdentityToIdentityParcelModel)

        val stakeModel = electedInfo?.let {
            val nominators = it.nominatorStakes.map(::mapNominatorToNominatorParcelModel)

            val nominatorInfo = (nominationStatus as? NominatedValidator.Status.Active)?.let { activeStatus ->
                UserStakeInfo(willBeRewarded = activeStatus.willUserBeRewarded)
            }

            StakeTargetStakeParcelModel.Active(
                totalStake = it.totalStake,
                ownStake = it.ownStake,
                stakers = nominators,
                minimumStake = null,
                rewards = it.apy,
                isOversubscribed = it.isOversubscribed,
                userStakeInfo = nominatorInfo
            )
        } ?: StakeTargetStakeParcelModel.Inactive

        StakeTargetDetailsParcelModel(
            accountIdHex = accountIdHex,
            isSlashed = validator.slashed,
            stake = stakeModel,
            identity = identityModel
        )
    }
}

fun mapStakeTargetDetailsToErrors(
    stakeTarget: StakeTargetDetailsParcelModel,
    displayConfig: StakeTargetDetailsPayload.DisplayConfig,
): List<ValidatorAlert> {
    return buildList {
        if (stakeTarget.isSlashed) {
            add(ValidatorAlert.Slashed)
        }

        if (stakeTarget.stake is StakeTargetStakeParcelModel.Active && stakeTarget.stake.isOversubscribed) {
            val nominatorInfo = stakeTarget.stake.userStakeInfo

            if (nominatorInfo == null || nominatorInfo.willBeRewarded) {
                add(ValidatorAlert.Oversubscribed.UserNotInvolved)
            } else {
                add(ValidatorAlert.Oversubscribed.UserMissedReward(displayConfig.oversubscribedWarningText))
            }
        }
    }
}

suspend fun mapValidatorDetailsParcelToValidatorDetailsModel(
    chain: Chain,
    validator: StakeTargetDetailsParcelModel,
    asset: Asset,
    displayConfig: StakeTargetDetailsPayload.DisplayConfig,
    iconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
): ValidatorDetailsModel {
    return with(validator) {
        val address = chain.addressOf(validator.accountIdHex.fromHex())

        val addressModel = iconGenerator.createAccountAddressModel(chain, address, validator.identity?.display)

        val identity = identity?.let(::mapIdentityParcelModelToIdentityModel)

        val stake = when (val stake = validator.stake) {
            StakeTargetStakeParcelModel.Inactive -> ValidatorStakeModel(
                status = ValidatorStakeModel.Status(
                    text = resourceManager.getString(R.string.staking_nominator_status_inactive),
                    icon = R.drawable.ic_time_16,
                    iconTint = R.color.icon_secondary
                ),
                activeStakeModel = null
            )

            is StakeTargetStakeParcelModel.Active -> {
                val totalStakeModel = mapAmountToAmountModel(stake.totalStake, asset)

                val nominatorsCount = stake.stakers.size
                val rewardsWithLabel = displayConfig.rewardSuffix.format(resourceManager, stake.rewards)

                val formattedMaxStakers = displayConfig.rewardedStakersPerStakeTarget.format()

                ValidatorStakeModel(
                    status = ValidatorStakeModel.Status(
                        text = resourceManager.getString(R.string.common_active),
                        icon = R.drawable.ic_checkmark_circle_16,
                        iconTint = R.color.icon_positive
                    ),
                    activeStakeModel = ValidatorStakeModel.ActiveStakeModel(
                        totalStake = totalStakeModel,
                        minimumStake = stake.minimumStake?.let { mapAmountToAmountModel(it, asset) },
                        nominatorsCount = nominatorsCount.format(),
                        maxNominations = resourceManager.getString(R.string.staking_nominations_rewarded_format, formattedMaxStakers),
                        apy = rewardsWithLabel
                    )
                )
            }
        }

        ValidatorDetailsModel(
            stake = stake,
            addressModel = addressModel,
            identity = identity
        )
    }
}

fun formatStakeTargetRewards(rewardsRate: BigDecimal) = rewardsRate.fractionToPercentage().formatAsPercentage()
fun formatStakeTargetRewardsOrNull(rewardsRate: BigDecimal?) = rewardsRate?.let(::formatStakeTargetRewards)

fun formatValidatorApy(validator: Validator) = formatStakeTargetRewardsOrNull(validator.electedInfo?.apy)
