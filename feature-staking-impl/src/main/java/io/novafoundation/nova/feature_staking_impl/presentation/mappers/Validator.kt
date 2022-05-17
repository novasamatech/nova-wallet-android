package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.formatAsPercentage
import io.novafoundation.nova.common.utils.fractionToPercentage
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.NominatedValidator
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.APYSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.TotalStakeSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.ValidatorOwnStakeSorting
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorAlert
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorDetailsModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorStakeModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorStakeModel.ActiveStakeModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorStakeParcelModel.Active.NominatorInfo
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
    StakeTargetModel.ColoredText(it, R.color.green)
}

fun stakeToScoring(stakeInPlanks: BigInteger?, token: Token): StakeTargetModel.Scoring.TwoFields? {
    if (stakeInPlanks == null) return null

    val stake = token.amountFromPlanks(stakeInPlanks)

    return StakeTargetModel.Scoring.TwoFields(
        primary = stake.formatTokenAmount(token.configuration),
        secondary = token.fiatAmount(stake).formatAsCurrency()
    )
}

fun mapValidatorToValidatorDetailsParcelModel(
    validator: Validator,
): ValidatorDetailsParcelModel {
    return mapValidatorToValidatorDetailsParcelModel(validator, nominationStatus = null)
}

fun mapValidatorToValidatorDetailsWithStakeFlagParcelModel(
    nominatedValidator: NominatedValidator,
): ValidatorDetailsParcelModel = mapValidatorToValidatorDetailsParcelModel(nominatedValidator.validator, nominatedValidator.status)

private fun mapValidatorToValidatorDetailsParcelModel(
    validator: Validator,
    nominationStatus: NominatedValidator.Status?,
): ValidatorDetailsParcelModel {
    return with(validator) {
        val identityModel = identity?.let(::mapIdentityToIdentityParcelModel)

        val stakeModel = electedInfo?.let {
            val nominators = it.nominatorStakes.map(::mapNominatorToNominatorParcelModel)

            val nominatorInfo = (nominationStatus as? NominatedValidator.Status.Active)?.let { activeStatus ->
                NominatorInfo(willBeRewarded = activeStatus.willUserBeRewarded)
            }

            ValidatorStakeParcelModel.Active(
                totalStake = it.totalStake,
                ownStake = it.ownStake,
                nominators = nominators,
                apy = it.apy,
                isOversubscribed = it.isOversubscribed,
                nominatorInfo = nominatorInfo
            )
        } ?: ValidatorStakeParcelModel.Inactive

        ValidatorDetailsParcelModel(
            accountIdHex = accountIdHex,
            isSlashed = validator.slashed,
            stake = stakeModel,
            identity = identityModel
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun mapValidatorDetailsToErrors(
    validator: ValidatorDetailsParcelModel,
): List<ValidatorAlert> {
    return buildList {
        if (validator.isSlashed) {
            add(ValidatorAlert.Slashed)
        }

        if (validator.stake is ValidatorStakeParcelModel.Active && validator.stake.isOversubscribed) {
            val nominatorInfo = validator.stake.nominatorInfo

            if (nominatorInfo == null || nominatorInfo.willBeRewarded) {
                add(ValidatorAlert.Oversubscribed.UserNotInvolved)
            } else {
                add(ValidatorAlert.Oversubscribed.UserMissedReward)
            }
        }
    }
}

suspend fun mapValidatorDetailsParcelToValidatorDetailsModel(
    chain: Chain,
    validator: ValidatorDetailsParcelModel,
    asset: Asset,
    maxNominators: Int,
    iconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
): ValidatorDetailsModel {
    return with(validator) {
        val address = chain.addressOf(validator.accountIdHex.fromHex())

        val addressModel = iconGenerator.createAccountAddressModel(chain, address, validator.identity?.display)

        val identity = identity?.let(::mapIdentityParcelModelToIdentityModel)

        val stake = when (val stake = validator.stake) {

            ValidatorStakeParcelModel.Inactive -> ValidatorStakeModel(
                status = ValidatorStakeModel.Status(
                    text = resourceManager.getString(R.string.staking_nominator_status_inactive),
                    icon = R.drawable.ic_time_16,
                    iconTint = R.color.white_48
                ),
                activeStakeModel = null
            )

            is ValidatorStakeParcelModel.Active -> {
                val totalStakeModel = mapAmountToAmountModel(stake.totalStake, asset)

                val nominatorsCount = stake.nominators.size
                val apyPercentageFormatted = stake.apy.fractionToPercentage().formatAsPercentage()
                val apyWithLabel = resourceManager.getString(R.string.staking_apy, apyPercentageFormatted)

                ValidatorStakeModel(
                    status = ValidatorStakeModel.Status(
                        text = resourceManager.getString(R.string.staking_nominator_status_active),
                        icon = R.drawable.ic_checkmark_circle_16,
                        iconTint = R.color.green
                    ),
                    activeStakeModel = ActiveStakeModel(
                        totalStake = totalStakeModel,
                        nominatorsCount = nominatorsCount.format(),
                        maxNominations = resourceManager.getString(R.string.staking_nominations_rewarded_format, maxNominators.format()),
                        apy = apyWithLabel
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
