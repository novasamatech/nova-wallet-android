package io.novafoundation.nova.feature_staking_impl.domain

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapAccountToStakingAccount
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.ActiveEraInfo
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculator
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
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
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
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combineTransform
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
    private val stakingSharedComputation: StakingSharedComputation,
) {

    suspend fun calculatePendingPayouts(scope: CoroutineScope): Result<PendingPayoutsStatistics> = withContext(Dispatchers.Default) {
        runCatching {
            val currentStakingState = selectedAccountStakingStateFlow(scope).first()
            val chainId = currentStakingState.chain.id
            val calculator = getEraTimeCalculator(scope)

            require(currentStakingState is StakingState.Stash)

            val activeEraIndex = stakingRepository.getActiveEraIndex(chainId)
            val historyDepth = stakingRepository.getHistoryDepth(chainId)

            val payouts = payoutRepository.calculateUnpaidPayouts(currentStakingState)

            val allValidatorStashes = payouts.map { it.validatorStash.value }.distinct()
            val identityMapping = identityRepository.getIdentitiesFromIds(allValidatorStashes, chainId)

            val pendingPayouts = payouts.map {
                val erasLeft = remainingEras(createdAtEra = it.era, activeEraIndex, historyDepth)

                val closeToExpire = erasLeft < historyDepth / 2.toBigInteger()

                val leftTime = calculator.calculateTillEraSet(destinationEra = it.era + historyDepth + ERA_OFFSET).toLong()
                val currentTimestamp = System.currentTimeMillis()
                with(it) {
                    val validatorIdentity = identityMapping[validatorStash]

                    val validatorInfo = PendingPayout.ValidatorInfo(
                        address = currentStakingState.chain.addressOf(validatorStash.value),
                        identityName = validatorIdentity?.display
                    )

                    PendingPayout(
                        validatorInfo = validatorInfo,
                        era = era,
                        amountInPlanks = amount,
                        timeLeft = leftTime,
                        timeLeftCalculatedAt = currentTimestamp,
                        closeToExpire = closeToExpire,
                        pagesToClaim = pagesToClaim
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
        stakingOption: StakingOption,
        period: RewardPeriod
    ) = withContext(Dispatchers.IO) {
        runCatching {
            stakingRewardsRepository.sync(stakingState.stashId, stakingOption, period)
        }
    }

    suspend fun observeStashSummary(
        stashState: StakingState.Stash.None,
        scope: CoroutineScope
    ): Flow<StakeSummary<StashNoneStatus>> = observeStakeSummary(stashState, scope) {
        emit(StashNoneStatus.INACTIVE)
    }

    suspend fun observeValidatorSummary(
        validatorState: StakingState.Stash.Validator,
        scope: CoroutineScope
    ): Flow<StakeSummary<ValidatorStatus>> = observeStakeSummary(validatorState, scope) {
        val status = when {
            it.activeStake.isZero -> ValidatorStatus.INACTIVE
            isValidatorActive(validatorState.stashId, it.activeEraInfo.exposures) -> ValidatorStatus.ACTIVE
            else -> ValidatorStatus.INACTIVE
        }

        emit(status)
    }

    suspend fun observeNominatorSummary(
        nominatorState: StakingState.Stash.Nominator,
        scope: CoroutineScope
    ): Flow<StakeSummary<NominatorStatus>> = observeStakeSummary(nominatorState, scope) {
        val eraStakers = it.activeEraInfo.exposures.values

        when {
            it.activeStake.isZero -> emit(NominatorStatus.Inactive(NominatorStatus.Inactive.Reason.MIN_STAKE))

            nominationStatus(nominatorState.stashId, eraStakers, it.rewardedNominatorsPerValidator).isActive -> emit(NominatorStatus.Active)

            nominatorState.nominations.isWaiting(it.activeEraInfo.eraIndex) -> {
                val nextEra = nominatorState.nominations.submittedInEra + ERA_OFFSET

                val timerFlow = eraTimeCalculatorFlow(scope).map { eraTimeCalculator ->
                    val timeLift = eraTimeCalculator.calculate(nextEra).toLong()

                    NominatorStatus.Waiting(timeLift)
                }

                emitAll(timerFlow)
            }

            else -> {
                val inactiveReason = when {
                    it.asset.bondedInPlanks < it.activeEraInfo.minStake -> NominatorStatus.Inactive.Reason.MIN_STAKE
                    else -> NominatorStatus.Inactive.Reason.NO_ACTIVE_VALIDATOR
                }

                emit(NominatorStatus.Inactive(inactiveReason))
            }
        }
    }

    fun observeUserRewards(
        state: StakingState.Stash,
        stakingOption: StakingOption,
    ): Flow<TotalReward> {
        return stakingRewardsRepository.totalRewardFlow(state.stashId, stakingOption.fullId)
    }

    fun observeNetworkInfoState(chainId: ChainId, scope: CoroutineScope): Flow<NetworkInfo> = flow {
        val lockupPeriod = getLockupDuration(chainId, scope)

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

    suspend fun getLockupDuration(sharedComputationScope: CoroutineScope) = withContext(Dispatchers.Default) {
        getLockupDuration(stakingSharedState.chainId(), sharedComputationScope)
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

        accountRepository.getActiveMetaAccounts().mapNotNull {
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

    suspend fun maxValidatorsPerNominator(stake: Balance): Int = withContext(Dispatchers.Default) {
        stakingConstantsRepository.maxValidatorsPerNominator(stakingSharedState.chainId(), stake)
    }

    suspend fun maxRewardedNominators(): Int? = withContext(Dispatchers.Default) {
        stakingConstantsRepository.maxRewardedNominatorPerValidator(stakingSharedState.chainId())
    }

    private suspend fun eraTimeCalculatorFlow(coroutineScope: CoroutineScope): Flow<EraTimeCalculator> {
        return stakingSharedComputation.eraCalculatorFlow(stakingSharedState.selectedOption(), coroutineScope)
    }

    private suspend fun getEraTimeCalculator(coroutineScope: CoroutineScope): EraTimeCalculator {
        return eraTimeCalculatorFlow(coroutineScope).first()
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
        statusResolver: suspend FlowCollector<S>.(StatusResolutionContext) -> Unit,
    ): Flow<StakeSummary<S>> = withContext(Dispatchers.Default) {
        val chainAsset = stakingSharedState.chainAsset()
        val chainId = chainAsset.chainId

        combineTransform(
            stakingSharedComputation.activeEraInfo(chainId, scope),
            walletRepository.assetFlow(state.accountId, chainAsset)
        ) { activeEraInfo, asset ->
            val activeStake = asset.bondedInPlanks

            val rewardedNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

            val statusResolutionContext = StatusResolutionContext(
                activeEraInfo,
                asset,
                rewardedNominatorsPerValidator,
                activeStake = activeStake
            )

            val summary = flow { statusResolver(statusResolutionContext) }.map { status ->
                StakeSummary(
                    status = status,
                    activeStake = activeStake
                )
            }

            emitAll(summary)
        }
    }

    private fun isValidatorActive(stashId: ByteArray, exposures: AccountIdMap<Exposure>): Boolean {
        val stashIdHex = stashId.toHexString()

        return stashIdHex in exposures.keys
    }

    private suspend fun activeNominators(chainId: ChainId, exposures: Collection<Exposure>): Int {
        val activeNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return exposures.fold(0) { acc, exposure ->
            val othersSize = exposure.others.size
            acc + if (activeNominatorsPerValidator != null) {
                othersSize.coerceAtMost(activeNominatorsPerValidator)
            } else {
                othersSize
            }
        }
    }

    private fun totalStake(exposures: Collection<Exposure>): BigInteger {
        return exposures.sumOf(Exposure::total)
    }

    private suspend fun getLockupDuration(chainId: ChainId, coroutineScope: CoroutineScope): Duration {
        val eraCalculator = getEraTimeCalculator(coroutineScope)
        val eraDuration = eraCalculator.eraDuration()

        return eraDuration * stakingConstantsRepository.lockupPeriodInEras(chainId).toInt()
    }

    private class StatusResolutionContext(
        val activeEraInfo: ActiveEraInfo,
        val asset: Asset,
        val rewardedNominatorsPerValidator: Int?,
        val activeStake: Balance,
    )
}
