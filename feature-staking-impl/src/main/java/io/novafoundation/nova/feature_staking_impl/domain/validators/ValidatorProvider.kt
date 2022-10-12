package io.novafoundation.nova.feature_staking_impl.domain.validators

import io.novafoundation.nova.common.utils.toHexAccountId
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.getActiveElectedValidatorsExposures
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.fromHex

sealed class ValidatorSource {

    object Elected : ValidatorSource()

    class Custom(val validatorIds: List<String>) : ValidatorSource()
}

class ValidatorProvider(
    private val stakingRepository: StakingRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val stakingConstantsRepository: StakingConstantsRepository,
) {

    suspend fun getValidators(
        chain: Chain,
        chainAsset: Chain.Asset,
        source: ValidatorSource,
        cachedExposures: AccountIdMap<Exposure>? = null,
    ): List<Validator> {
        val chainId = chain.id

        val electedValidatorExposures = cachedExposures ?: stakingRepository.getActiveElectedValidatorsExposures(chainId)

        val requestedValidatorIds = when (source) {
            ValidatorSource.Elected -> electedValidatorExposures.keys.toList()
            is ValidatorSource.Custom -> source.validatorIds
        }

        val validatorIdsToQueryPrefs = electedValidatorExposures.keys + requestedValidatorIds

        val validatorPrefs = stakingRepository.getValidatorPrefs(chainId, validatorIdsToQueryPrefs.toList())

        val identities = identityRepository.getIdentitiesFromIds(chainId, requestedValidatorIds)
        val slashes = stakingRepository.getSlashes(chainId, requestedValidatorIds)

        val rewardCalculator = rewardCalculatorFactory.create(chainAsset, electedValidatorExposures, validatorPrefs)
        val maxNominators = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return requestedValidatorIds.map { accountIdHex ->
            val prefs = validatorPrefs[accountIdHex]

            val electedInfo = electedValidatorExposures[accountIdHex]?.let {
                Validator.ElectedInfo(
                    totalStake = it.total,
                    ownStake = it.own,
                    nominatorStakes = it.others,
                    apy = rewardCalculator.getApyFor(accountIdHex),
                    isOversubscribed = it.others.size > maxNominators
                )
            }

            Validator(
                slashed = slashes.getOrDefault(accountIdHex, false),
                accountIdHex = accountIdHex,
                electedInfo = electedInfo,
                prefs = prefs,
                identity = identities[accountIdHex],
                address = chain.addressOf(accountIdHex.fromHex())
            )
        }
    }

    suspend fun getValidatorWithoutElectedInfo(chainId: ChainId, address: String): Validator {
        val accountId = address.toHexAccountId()

        val accountIdBridged = listOf(accountId)

        val prefs = stakingRepository.getValidatorPrefs(chainId, accountIdBridged)[accountId]
        val identity = identityRepository.getIdentitiesFromIds(chainId, accountIdBridged)[accountId]

        val slashes = stakingRepository.getSlashes(chainId, accountIdBridged)

        return Validator(
            slashed = slashes.getOrDefault(accountId, false),
            accountIdHex = accountId,
            address = address,
            prefs = prefs,
            identity = identity,
            electedInfo = null
        )
    }
}
