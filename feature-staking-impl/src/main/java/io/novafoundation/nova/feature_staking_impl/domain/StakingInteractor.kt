package io.novafoundation.nova.feature_staking_impl.domain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapAccountToStakingAccount
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.ActiveEraInfo
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculator
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayout
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayoutsStatistics
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.domain.model.StashNoneStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.model.ValidatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.time.Duration

val ERA_OFFSET = 1.toBigInteger()

class StakingInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingRepository: StakingRepository,
    private val stakingRewardsRepository: StakingRewardsRepository,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val identityRepository: OnChainIdentityRepository,
    private val stakingSharedState: StakingSharedState,
    private val payoutRepository: PayoutRepository,
    private val assetUseCase: AssetUseCase,
    private val factory: EraTimeCalculatorFactory,
    private val stakingSharedComputation: StakingSharedComputation,
) {
    suspend fun calculatePendingPayouts(scope: CoroutineScope): Result<PendingPayoutsStatistics> = withContext(Dispatchers.Default) {
        runCatching {
            val currentStakingState = selectedAccountStakingStateFlow(scope).first()
            val chainId = currentStakingState.chain.id
            val calculator = getEraTimeCalculator()

            require(currentStakingState is StakingState.Stash)

            val activeEraIndex = stakingRepository.getActiveEraIndex(chainId)
            val historyDepth = stakingRepository.getHistoryDepth(chainId)

            val payouts = payoutRepository.calculateUnpaidPayouts(currentStakingState)

            val allValidatorAddresses = payouts.map(Payout::validatorAddress).distinct()
            val identityMapping = identityRepository.getIdentitiesFromAddresses(currentStakingState.chain, allValidatorAddresses)

            val pendingPayouts = payouts.map {
                val erasLeft = remainingEras(createdAtEra = it.era, activeEraIndex, historyDepth)

                val closeToExpire = erasLeft < historyDepth / 2.toBigInteger()

                val leftTime = calculator.calculateTillEraSet(destinationEra = it.era + historyDepth + ERA_OFFSET).toLong()
                val currentTimestamp = System.currentTimeMillis()
                with(it) {
                    val validatorIdentity = identityMapping[validatorAddress]

                    val validatorInfo = PendingPayout.ValidatorInfo(validatorAddress, validatorIdentity?.display)

                    PendingPayout(
                        validatorInfo = validatorInfo,
                        era = era,
                        amountInPlanks = amount,
                        timeLeft = leftTime,
                        timeLeftCalculatedAt = currentTimestamp,
                        closeToExpire = closeToExpire
                    )
                }
            }.sortedBy { it.era }

            PendingPayoutsStatistics(
                payouts = pendingPayouts,
                totalAmountInPlanks = pendingPayouts.sumByBigInteger(PendingPayout::amountInPlanks)
            )
        }
    }

    suspend fun syncStakingRewards(
        stakingState: StakingState.Stash,
        chain: Chain,
        chainAsset: Chain.Asset,
        period: RewardPeriod
    ) = withContext(Dispatchers.IO) {
        runCatching {
            stakingRewardsRepository.sync(stakingState.stashAddress, chain, chainAsset, period)
        }
    }

    suspend fun observeStashSummary(
        stashState: StakingState.Stash.None,
        scope: CoroutineScope
    ): Flow<StakeSummary<StashNoneStatus>> = observeStakeSummary(stashState, scope) {
        StashNoneStatus.INACTIVE
    }

    suspend fun observeValidatorSummary(
        validatorState: StakingState.Stash.Validator,
        scope: CoroutineScope
    ): Flow<StakeSummary<ValidatorStatus>> = observeStakeSummary(validatorState, scope) {
        when {
            isValidatorActive(validatorState.stashId, it.activeEraInfo.exposures) -> ValidatorStatus.ACTIVE
            else -> ValidatorStatus.INACTIVE
        }
    }

    suspend fun observeNominatorSummary(
        nominatorState: StakingState.Stash.Nominator,
        scope: CoroutineScope
    ): Flow<StakeSummary<NominatorStatus>> = observeStakeSummary(nominatorState, scope) {
        val eraStakers = it.activeEraInfo.exposures.values

        when {
            nominationStatus(nominatorState.stashId, eraStakers, it.rewardedNominatorsPerValidator).isActive -> NominatorStatus.Active

            nominatorState.nominations.isWaiting(it.activeEraInfo.eraIndex) -> NominatorStatus.Waiting(
                timeLeft = getEraTimeCalculator().calculate(nominatorState.nominations.submittedInEra + ERA_OFFSET).toLong()
            )

            else -> {
                val inactiveReason = when {
                    it.asset.bondedInPlanks < it.activeEraInfo.minStake -> NominatorStatus.Inactive.Reason.MIN_STAKE
                    else -> NominatorStatus.Inactive.Reason.NO_ACTIVE_VALIDATOR
                }

                NominatorStatus.Inactive(inactiveReason)
            }
        }
    }

    fun observeUserRewards(
        state: StakingState.Stash,
        chain: Chain,
        chainAsset: Chain.Asset,
    ): Flow<TotalReward> {
        return stakingRewardsRepository.totalRewardFlow(state.stashAddress, chain.id, chainAsset.id)
    }

    fun observeNetworkInfoState(chainId: ChainId, scope: CoroutineScope): Flow<NetworkInfo> = flow {
        val lockupPeriod = getLockupDuration(chainId)

        val innerFlow = stakingSharedComputation.activeEraInfo(chainId, scope).map { activeEraInfo ->
            val exposures = activeEraInfo.exposures.values

            NetworkInfo(
                lockupPeriod = lockupPeriod,
                minimumStake = activeEraInfo.minStake,
                totalStake = totalStake(exposures),
                stakingPeriod = StakingPeriod.Unlimited,
                nominatorsCount = activeNominators(chainId, exposures),
            )
        }

        emitAll(innerFlow)
    }

    suspend fun getLockupDuration() = withContext(Dispatchers.Default) {
        getLockupDuration(stakingSharedState.chainId())
    }

    suspend fun getEraDuration() = withContext(Dispatchers.Default) {
        getEraTimeCalculator().eraDuration()
    }

    fun selectedAccountStakingStateFlow(scope: CoroutineScope) = flowOfAll {
        val assetWithChain = stakingSharedState.chainAndAsset()

        stakingSharedComputation.selectedAccountStakingStateFlow(
            assetWithChain = assetWithChain,
            scope = scope
        )
    }

    suspend fun getAccountProjectionsInSelectedChains() = withContext(Dispatchers.Default) {
        val chain = stakingSharedState.chain()

        accountRepository.allMetaAccounts().mapNotNull {
            mapAccountToStakingAccount(chain, it)
        }
    }

    fun currentAssetFlow() = assetUseCase.currentAssetFlow()

    fun assetFlow(accountAddress: String): Flow<Asset> {
        return flow {
            val (chain, chainAsset) = stakingSharedState.assetWithChain.first()

            emitAll(
                walletRepository.assetFlow(
                    accountId = chain.accountIdOf(accountAddress),
                    chainAsset = chainAsset
                )
            )
        }
    }

    suspend fun getSelectedAccountProjection(): StakingAccount = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val selectedChain = stakingSharedState.chain()

        mapAccountToStakingAccount(selectedChain, metaAccount)!!
    }

    suspend fun getRewardDestination(accountStakingState: StakingState.Stash): RewardDestination = withContext(Dispatchers.Default) {
        stakingRepository.getRewardDestination(accountStakingState)
    }

    suspend fun maxValidatorsPerNominator(): Int = withContext(Dispatchers.Default) {
        stakingConstantsRepository.maxValidatorsPerNominator(stakingSharedState.chainId())
    }

    suspend fun maxRewardedNominators(): Int = withContext(Dispatchers.Default) {
        stakingConstantsRepository.maxRewardedNominatorPerValidator(stakingSharedState.chainId())
    }

    private suspend fun getEraTimeCalculator(): EraTimeCalculator {
        return factory.create(stakingSharedState.selectedOption())
    }

    private fun remainingEras(
        createdAtEra: BigInteger,
        activeEra: BigInteger,
        lifespanInEras: BigInteger,
    ): BigInteger {
        val erasPast = activeEra - createdAtEra

        return lifespanInEras - erasPast
    }

    private suspend fun <S> observeStakeSummary(
        state: StakingState.Stash,
        scope: CoroutineScope,
        statusResolver: suspend (StatusResolutionContext) -> S,
    ): Flow<StakeSummary<S>> = withContext(Dispatchers.Default) {
        val chainAsset = stakingSharedState.chainAsset()
        val chainId = chainAsset.chainId

        combine(
            stakingSharedComputation.activeEraInfo(chainId, scope),
            walletRepository.assetFlow(state.accountId, chainAsset)
        ) { activeEraInfo, asset ->
            val totalStaked = asset.bonded

            val rewardedNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

            val statusResolutionContext = StatusResolutionContext(
                activeEraInfo,
                asset,
                rewardedNominatorsPerValidator,
            )

            val status = statusResolver(statusResolutionContext)

            StakeSummary(
                status = status,
                totalStaked = totalStaked
            )
        }
    }

    private fun isValidatorActive(stashId: ByteArray, exposures: AccountIdMap<Exposure>): Boolean {
        val stashIdHex = stashId.toHexString()

        return stashIdHex in exposures.keys
    }

    private suspend fun activeNominators(chainId: ChainId, exposures: Collection<Exposure>): Int {
        val activeNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return exposures.fold(mutableSetOf<AccountIdKey>()) { acc, exposure ->
            acc += exposure.others.sortedByDescending(IndividualExposure::value)
                .take(activeNominatorsPerValidator)
                .map { it.who.intoKey() }

            acc
        }.size
    }

    private fun totalStake(exposures: Collection<Exposure>): BigInteger {
        return exposures.sumOf(Exposure::total)
    }

    private suspend fun getLockupDuration(chainId: ChainId): Duration {
        val eraCalculator = getEraTimeCalculator()
        val eraDuration = eraCalculator.eraDuration()

        return eraDuration * stakingConstantsRepository.lockupPeriodInEras(chainId).toInt()
    }

    private class StatusResolutionContext(
        val activeEraInfo: ActiveEraInfo,
        val asset: Asset,
        val rewardedNominatorsPerValidator: Int,
    )
}
