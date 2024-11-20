package io.novafoundation.nova.feature_staking_impl.domain.validators

import io.novafoundation.nova.common.utils.foldToSet
import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.electedExposuresInActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.CoroutineScope

sealed class ValidatorSource {

    object Elected : ValidatorSource()

    class Custom(val validatorIds: Set<String>) : ValidatorSource()

    object NovaValidators : ValidatorSource()
}

class ValidatorProvider(
    private val stakingRepository: StakingRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val validatorsPreferencesSource: ValidatorsPreferencesSource,
) {

    suspend fun getValidators(
        stakingOption: StakingOption,
        sources: List<ValidatorSource>,
        scope: CoroutineScope,
    ): List<Validator> {
        val chain = stakingOption.assetWithChain.chain
        val chainId = chain.id

        val novaValidatorIds = validatorsPreferencesSource.getRecommendedValidatorIds(chainId)
        val electedValidatorExposures = stakingSharedComputation.electedExposuresInActiveEra(chainId, scope)

        val requestedValidatorIds = sources.allValidatorIds(chainId, electedValidatorExposures, novaValidatorIds)
        // we always need validator prefs for elected validators to construct reward calculator
        val validatorIdsToQueryPrefs = electedValidatorExposures.keys + requestedValidatorIds

        val validatorPrefs = stakingRepository.getValidatorPrefs(chainId, validatorIdsToQueryPrefs)
        val identities = identityRepository.getIdentitiesFromIdsHex(chainId, requestedValidatorIds)
        val slashes = stakingRepository.getSlashes(chainId, requestedValidatorIds)

        val rewardCalculator = rewardCalculatorFactory.create(stakingOption, electedValidatorExposures, validatorPrefs, scope)
        val maxNominators = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return requestedValidatorIds.map { accountIdHex ->
            val electedInfo = electedValidatorExposures[accountIdHex]?.let {
                Validator.ElectedInfo(
                    totalStake = it.total,
                    ownStake = it.own,
                    nominatorStakes = it.others,
                    apy = rewardCalculator.getApyFor(accountIdHex),
                    isOversubscribed = maxNominators != null && it.others.size > maxNominators
                )
            }

            Validator(
                slashed = slashes.getOrDefault(accountIdHex, false),
                accountIdHex = accountIdHex,
                electedInfo = electedInfo,
                prefs = validatorPrefs[accountIdHex],
                identity = identities[accountIdHex],
                address = chain.addressOf(accountIdHex.fromHex()),
                isNovaValidator = accountIdHex in novaValidatorIds
            )
        }
    }

    suspend fun getValidatorWithoutElectedInfo(chainId: ChainId, address: String): Validator {
        val accountId = address.toHexAccountId()

        val accountIdBridged = listOf(accountId)

        val prefs = stakingRepository.getValidatorPrefs(chainId, accountIdBridged)[accountId]
        val identity = identityRepository.getIdentitiesFromIdsHex(chainId, accountIdBridged)[accountId]

        val slashes = stakingRepository.getSlashes(chainId, accountIdBridged)

        val novaValidatorIds = validatorsPreferencesSource.getRecommendedValidatorIds(chainId)

        return Validator(
            slashed = slashes.getOrDefault(accountId, false),
            accountIdHex = accountId,
            address = address,
            prefs = prefs,
            identity = identity,
            electedInfo = null,
            isNovaValidator = accountId in novaValidatorIds
        )
    }

    private fun List<ValidatorSource>.allValidatorIds(
        chainId: ChainId,
        electedExposures: AccountIdMap<Exposure>,
        novaValidatorIds: Set<String>,
    ): Set<String> {
        return foldToSet { it.validatorIds(chainId, electedExposures, novaValidatorIds) }
    }

    private fun ValidatorSource.validatorIds(
        chainId: ChainId,
        electedExposures: AccountIdMap<Exposure>,
        novaValidatorIds: Set<String>,
    ): Set<String> {
        return when (this) {
            is ValidatorSource.Custom -> validatorIds
            ValidatorSource.Elected -> electedExposures.keys
            ValidatorSource.NovaValidators -> novaValidatorIds
        }
    }
}

suspend fun ValidatorProvider.getValidators(
    stakingOption: StakingOption,
    source: ValidatorSource,
    scope: CoroutineScope,
): List<Validator> = getValidators(
    stakingOption = stakingOption,
    sources = listOf(source),
    scope = scope
)
