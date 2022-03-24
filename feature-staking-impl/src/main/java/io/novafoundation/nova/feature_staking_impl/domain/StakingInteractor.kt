package io.novafoundation.nova.feature_staking_impl.domain

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.api.EraTimeCalculator
import io.novafoundation.nova.feature_staking_api.domain.api.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.erasPerDay
import io.novafoundation.nova.feature_staking_api.domain.api.getActiveElectedValidatorsExposures
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.StakingAccount
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapAccountToStakingAccount
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayout
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayoutsStatistics
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.domain.model.StashNoneStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.ValidatorStatus
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.time.ExperimentalTime

const val HOURS_IN_DAY = 24

class EraRelativeInfo(
    val daysLeft: Int,
    val daysPast: Int,
    val erasLeft: BigInteger,
    val erasPast: BigInteger,
)

val ERA_OFFSET = 1.toBigInteger()

class StakingInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val stakingRepository: StakingRepository,
    private val stakingRewardsRepository: StakingRewardsRepository,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val identityRepository: IdentityRepository,
    private val stakingSharedState: StakingSharedState,
    private val payoutRepository: PayoutRepository,
    private val assetUseCase: AssetUseCase,
    private val factory: EraTimeCalculatorFactory,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun calculatePendingPayouts(): Result<PendingPayoutsStatistics> = withContext(Dispatchers.Default) {
        runCatching {
            val currentStakingState = selectedAccountStakingStateFlow().first()
            val chainId = currentStakingState.chain.id

            require(currentStakingState is StakingState.Stash)

            val erasPerDay = stakingRepository.erasPerDay(chainId)
            val activeEraIndex = stakingRepository.getActiveEraIndex(chainId)
            val historyDepth = stakingRepository.getHistoryDepth(chainId)

            val payouts = payoutRepository.calculateUnpaidPayouts(currentStakingState)

            val allValidatorAddresses = payouts.map(Payout::validatorAddress).distinct()
            val identityMapping = identityRepository.getIdentitiesFromAddresses(currentStakingState.chain, allValidatorAddresses)

            val calculator = getEraTimeCalculator()
            val pendingPayouts = payouts.map {
                val relativeInfo = eraRelativeInfo(it.era, activeEraIndex, historyDepth, erasPerDay)

                val closeToExpire = relativeInfo.erasLeft < historyDepth / 2.toBigInteger()

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

    suspend fun syncStakingRewards(stakingState: StakingState.Stash) = withContext(Dispatchers.IO) {
        runCatching {
            stakingRewardsRepository.sync(stakingState.stashAddress, stakingState.chain)
        }
    }

    suspend fun observeStashSummary(
        stashState: StakingState.Stash.None
    ): Flow<StakeSummary<StashNoneStatus>> = observeStakeSummary(stashState) {
        StashNoneStatus.INACTIVE
    }

    suspend fun observeValidatorSummary(
        validatorState: StakingState.Stash.Validator,
    ): Flow<StakeSummary<ValidatorStatus>> = observeStakeSummary(validatorState) {
        when {
            isValidatorActive(validatorState.stashId, it.eraStakers) -> ValidatorStatus.ACTIVE
            else -> ValidatorStatus.INACTIVE
        }
    }

    suspend fun observeNominatorSummary(
        nominatorState: StakingState.Stash.Nominator,
    ): Flow<StakeSummary<NominatorStatus>> = observeStakeSummary(nominatorState) {
        val eraStakers = it.eraStakers.values
        val chainId = nominatorState.chain.id

        when {
            isNominationActive(nominatorState.stashId, it.eraStakers.values, it.rewardedNominatorsPerValidator) -> NominatorStatus.Active

            nominatorState.nominations.isWaiting(it.activeEraIndex) -> NominatorStatus.Waiting(
                timeLeft = getEraTimeCalculator().calculate(nominatorState.nominations.submittedInEra + ERA_OFFSET).toLong()
            )

            else -> {
                val inactiveReason = when {
                    it.asset.bondedInPlanks < minimumStake(eraStakers, stakingRepository.minimumNominatorBond(chainId)) -> {
                        NominatorStatus.Inactive.Reason.MIN_STAKE
                    }
                    else -> NominatorStatus.Inactive.Reason.NO_ACTIVE_VALIDATOR
                }

                NominatorStatus.Inactive(inactiveReason)
            }
        }
    }

    fun observeUserRewards(state: StakingState.Stash) = stakingRewardsRepository.totalRewardFlow(state.stashAddress)

    suspend fun observeNetworkInfoState(chainId: ChainId): Flow<NetworkInfo> = withContext(Dispatchers.Default) {
        val lockupPeriod = getLockupPeriodInDays(chainId)

        stakingRepository.electedExposuresInActiveEra(chainId).map { exposuresMap ->
            val exposures = exposuresMap.values

            val minimumNominatorBond = stakingRepository.minimumNominatorBond(chainId)

            NetworkInfo(
                lockupPeriodInDays = lockupPeriod,
                minimumStake = minimumStake(exposures, minimumNominatorBond),
                totalStake = totalStake(exposures),
                stakingPeriod = StakingPeriod.Unlimited,
                nominatorsCount = activeNominators(chainId, exposures),
            )
        }
    }

    suspend fun getLockupPeriodInDays() = withContext(Dispatchers.Default) {
        getLockupPeriodInDays(stakingSharedState.chainId())
    }

    suspend fun getEraHoursLength(): Int = withContext(Dispatchers.Default) {
        val chainId = stakingSharedState.chainId()

        HOURS_IN_DAY / stakingRepository.erasPerDay(chainId)
    }

    fun selectionStateFlow() = combineToPair(
        accountRepository.selectedMetaAccountFlow(),
        stakingSharedState.assetWithChain
    )

    fun selectedAccountStakingStateFlow(
        metaAccount: MetaAccount,
        assetWithChain: SingleAssetSharedState.AssetWithChain
    ) = flow {
        val (chain, chainAsset) = assetWithChain
        val accountId = metaAccount.accountIdIn(chain)!! // TODO may be null for ethereum chains

        emitAll(stakingRepository.stakingStateFlow(chain, chainAsset, accountId))
    }

    fun selectedAccountStakingStateFlow() = selectionStateFlow().flatMapLatest { (selectedAccount, assetWithChain) ->
        selectedAccountStakingStateFlow(selectedAccount, assetWithChain)
    }

    suspend fun getAccountProjectionsInSelectedChains() = withContext(Dispatchers.Default) {
        val chain = stakingSharedState.chain()

        accountRepository.allMetaAccounts().map {
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

    fun selectedAccountProjectionFlow(): Flow<StakingAccount> {
        return combine(
            stakingSharedState.assetWithChain,
            accountRepository.selectedMetaAccountFlow()
        ) { (chain, _), account ->
            mapAccountToStakingAccount(chain, account)
        }
    }

    suspend fun getProjectedAccount(address: String): StakingAccount = withContext(Dispatchers.Default) {
        val chain = stakingSharedState.chain()
        val accountId = chain.accountIdOf(address)

        val metaAccount = accountRepository.findMetaAccount(accountId)!!

        mapAccountToStakingAccount(chain, metaAccount)
    }

    suspend fun getSelectedAccountProjection(): StakingAccount = withContext(Dispatchers.Default) {
        val account = accountRepository.getSelectedAccount(stakingSharedState.chainId())

        mapAccountToStakingAccount(account)
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
        return factory.create(stakingSharedState.chainId())
    }

    private fun eraRelativeInfo(
        createdAtEra: BigInteger,
        activeEra: BigInteger,
        lifespanInEras: BigInteger,
        erasPerDay: Int,
    ): EraRelativeInfo {
        val erasPast = activeEra - createdAtEra
        val erasLeft = lifespanInEras - erasPast

        val daysPast = erasPast.toInt() / erasPerDay
        val daysLeft = erasLeft.toInt() / erasPerDay

        return EraRelativeInfo(daysLeft, daysPast, erasLeft, erasPast)
    }

    private suspend fun <S> observeStakeSummary(
        state: StakingState.Stash,
        statusResolver: suspend (StatusResolutionContext) -> S,
    ): Flow<StakeSummary<S>> = withContext(Dispatchers.Default) {
        val chainAsset = stakingSharedState.chainAsset()
        val chainId = chainAsset.chainId

        combine(
            stakingRepository.observeActiveEraIndex(chainId),
            walletRepository.assetFlow(state.accountId, chainAsset)
        ) { activeEraIndex, asset ->
            val totalStaked = asset.bonded

            val eraStakers = stakingRepository.getActiveElectedValidatorsExposures(chainId)
            val rewardedNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

            val statusResolutionContext = StatusResolutionContext(eraStakers, activeEraIndex, asset, rewardedNominatorsPerValidator)

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

    @OptIn(ExperimentalTime::class)
    private suspend fun activeNominators(chainId: ChainId, exposures: Collection<Exposure>): Int {
        val activeNominatorsPerValidator = stakingConstantsRepository.maxRewardedNominatorPerValidator(chainId)

        return exposures.fold(mutableSetOf<String>()) { acc, exposure ->
            acc += exposure.others.sortedByDescending(IndividualExposure::value)
                .take(activeNominatorsPerValidator)
                .map { it.who.toHexString() }

            acc
        }.size
    }

    private fun totalStake(exposures: Collection<Exposure>): BigInteger {
        return exposures.sumOf(Exposure::total)
    }

    private suspend fun getLockupPeriodInDays(chainId: ChainId): Int {
        return stakingConstantsRepository.lockupPeriodInEras(chainId).toInt() / stakingRepository.erasPerDay(chainId)
    }

    private class StatusResolutionContext(
        val eraStakers: AccountIdMap<Exposure>,
        val activeEraIndex: BigInteger,
        val asset: Asset,
        val rewardedNominatorsPerValidator: Int
    )
}
