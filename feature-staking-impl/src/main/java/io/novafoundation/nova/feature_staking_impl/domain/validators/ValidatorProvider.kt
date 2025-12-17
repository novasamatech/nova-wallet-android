package io.novafoundation.nova.feature_staking_impl.domain.validators

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHex
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
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
        val slashes = stakingRepository.getSlashes(chain.id, requestedValidatorIds)

        val rewardCalculator = rewardCalculatorFactory.create(stakingOption, electedValidatorExposures, validatorPrefs, scope)
        val maxNominators = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return requestedValidatorIds.map { accountIdHex ->
            val accountId = AccountIdKey.fromHex(accountIdHex).getOrThrow()

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
                slashed = accountId in slashes,
                accountIdHex = accountIdHex,
                electedInfo = electedInfo,
                prefs = validatorPrefs[accountIdHex],
                identity = identities[accountIdHex],
                address = chain.addressOf(accountId.value),
                isNovaValidator = accountIdHex in novaValidatorIds
            )
        }
    }

    suspend fun getValidatorWithoutElectedInfo(chain: Chain, address: String): Validator {
        val chainId = chain.id

        val accountIdHex = address.toHexAccountId()
        val accountId = AccountIdKey.fromHex(accountIdHex).getOrThrow()

        val accountIdHexBridged = listOf(accountIdHex)

        val prefs = stakingRepository.getValidatorPrefs(chainId, accountIdHexBridged)[accountIdHex]
        val identity = identityRepository.getIdentitiesFromIdsHex(chainId, accountIdHexBridged)[accountIdHex]

        val slashes = stakingRepository.getSlashes(chain.id, accountIdHexBridged)

        val novaValidatorIds = validatorsPreferencesSource.getRecommendedValidatorIds(chainId)

        return Validator(
            slashed = accountId in slashes,
            accountIdHex = accountIdHex,
            address = address,
            prefs = prefs,
            identity = identity,
            electedInfo = null,
            isNovaValidator = accountIdHex in novaValidatorIds
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
