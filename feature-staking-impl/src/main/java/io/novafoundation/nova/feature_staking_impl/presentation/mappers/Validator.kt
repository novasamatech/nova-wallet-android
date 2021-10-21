package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.formatAsPercentage
import io.novafoundation.nova.common.utils.fractionToPercentage
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import io.novafoundation.nova.feature_staking_api.domain.model.NominatedValidator
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.APYSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.TotalStakeSorting
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.sortings.ValidatorOwnStakeSorting
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorDetailsModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorStakeModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model.ValidatorStakeModel.ActiveStakeModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.view.Error
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorStakeParcelModel.Active.NominatorInfo
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

private val PERCENT_MULTIPLIER = 100.toBigDecimal()

private const val ICON_SIZE_DP = 24
private const val ICON_DETAILS_SIZE_DP = 32

suspend fun mapValidatorToValidatorModel(
    chain: Chain,
    validator: Validator,
    iconGenerator: AddressIconGenerator,
    token: Token,
    isChecked: Boolean? = null,
    sorting: RecommendationSorting = APYSorting,
) = mapValidatorToValidatorModel(
    chain,
    validator,
    { iconGenerator.createAddressModel(it, ICON_SIZE_DP, validator.identity?.display) },
    token,
    isChecked,
    sorting
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
            APYSorting -> {
                electedInfo?.apy?.let {
                    val apyPercentage = it.fractionToPercentage().formatAsPercentage()

                    ValidatorModel.Scoring.OneField(apyPercentage)
                }
            }

            TotalStakeSorting -> stakeToScoring(electedInfo?.totalStake, token)

            ValidatorOwnStakeSorting -> stakeToScoring(electedInfo?.ownStake, token)

            else -> throw NotImplementedError("Unsupported sorting: $sorting")
        }

        ValidatorModel(
            accountIdHex = accountIdHex,
            slashed = slashed,
            image = addressModel.image,
            address = addressModel.address,
            scoring = scoring,
            title = addressModel.nameOrAddress,
            isChecked = isChecked,
            validator = validator
        )
    }
}

private fun stakeToScoring(stakeInPlanks: BigInteger?, token: Token): ValidatorModel.Scoring.TwoFields? {
    if (stakeInPlanks == null) return null

    val stake = token.amountFromPlanks(stakeInPlanks)

    return ValidatorModel.Scoring.TwoFields(
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
                isSlashed = slashed,
                isOversubscribed = it.isOversubscribed,
                nominatorInfo = nominatorInfo
            )
        } ?: ValidatorStakeParcelModel.Inactive

        ValidatorDetailsParcelModel(accountIdHex, stakeModel, identityModel)
    }
}

// FIXME Wrong logic for isOversubscribed & isSlashed - should not require elected state/nominator info
fun mapValidatorDetailsToErrors(
    validator: ValidatorDetailsParcelModel,
): List<Error>? {
    return when (val stake = validator.stake) {
        ValidatorStakeParcelModel.Inactive -> null
        is ValidatorStakeParcelModel.Active -> {
            val nominatorInfo = stake.nominatorInfo ?: return null

            return mutableListOf<Error>().apply {
                if (stake.isOversubscribed) {
                    if (nominatorInfo.willBeRewarded) {
                        add(Error.OversubscribedPaid)
                    } else {
                        add(Error.OversubscribedUnpaid)
                    }
                }
                if (stake.isSlashed) add(Error.Slashed)
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
        val token = asset.token

        val address = chain.addressOf(validator.accountIdHex.fromHex())

        val addressImage = iconGenerator.createAddressModel(address, ICON_DETAILS_SIZE_DP)

        val identity = identity?.let(::mapIdentityParcelModelToIdentityModel)

        val stake = when (val stake = validator.stake) {

            ValidatorStakeParcelModel.Inactive -> ValidatorStakeModel(
                statusText = resourceManager.getString(R.string.staking_nominator_status_inactive),
                statusColorRes = R.color.gray2,
                activeStakeModel = null
            )

            is ValidatorStakeParcelModel.Active -> {
                val totalStake = token.amountFromPlanks(stake.totalStake)
                val totalStakeFormatted = totalStake.formatTokenAmount(asset.token.configuration)
                val totalStakeFiatFormatted = token.fiatAmount(totalStake).formatAsCurrency()
                val nominatorsCount = stake.nominators.size
                val apyPercentageFormatted = (PERCENT_MULTIPLIER * stake.apy).formatAsPercentage()

                ValidatorStakeModel(
                    statusText = resourceManager.getString(R.string.staking_nominator_status_active),
                    statusColorRes = R.color.green,
                    activeStakeModel = ActiveStakeModel(
                        totalStake = totalStakeFormatted,
                        totalStakeFiat = totalStakeFiatFormatted,
                        nominatorsCount = nominatorsCount.format(),
                        apy = apyPercentageFormatted,
                        maxNominations = maxNominators.format()
                    )
                )
            }
        }

        ValidatorDetailsModel(
            stake,
            address,
            addressImage.image,
            identity
        )
    }
}
