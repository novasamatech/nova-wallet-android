package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

interface BittensorStakingRepository {

    suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal? = null

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails>

    fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails>

    fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails>
}

interface BittensorRootValidatorSource {

    suspend fun validatorDetails(chain: Chain): Map<AccountIdKey, BittensorRootValidatorDetails>

    suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator?

    suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal?
}

class RealBittensorStakingRepository(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: BittensorPositionRepository,
    private val validatorSource: BittensorRootValidatorSource,
    private val logger: Logger
) : BittensorStakingRepository {

    override suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator) =
        validatorSource.annualPercentageYield(chain, validator)

    override fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> =
        observeRootStakingDetails(accountRepository.selectedMetaAccountFlow())

    override fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails> =
        observeRootStakingDetails(accountRepository.metaAccountFlow(metaId))

    override fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails> =
        observeRootStakingDetails(accountRepository.metaAccountFlow(metaId), selectDefault = false)

    private fun observeRootStakingDetails(
        accounts: Flow<MetaAccount>,
        selectDefault: Boolean = true
    ): Flow<BittensorRootStakingDetails> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)

        accounts
            .flatMapLatest { metaAccount ->
                combine(
                    walletRepository.assetFlow(metaAccount.id, chain.utilityAsset),
                    observePosition(chain, metaAccount.requireAccountIdIn(chain), selectDefault)
                ) { asset, position ->
                    BittensorRootStakingDetails(
                        metaId = metaAccount.id,
                        chain = chain,
                        asset = asset,
                        validator = position.validator,
                        annualPercentageYield = position.annualPercentageYield,
                        position = position.positions.firstOrNull { it.validatorHotkey == position.validator?.hotkey },
                        positions = position.positions,
                        dataStatus = position.status
                    )
                }
            }
    }

    private fun observePosition(chain: Chain, coldKey: AccountId, selectDefault: Boolean): Flow<PositionState> = flowOfAll {
        var defaultValidatorSelection: DefaultValidatorSelection? = null
        positionRepository.observePositions(chain.id, coldKey).filter { !selectDefault || it.status !is BittensorDataStatus.Loading }
            .map { positionsState ->
                if (!selectDefault) {
                    return@map PositionState(positionsState.status, null, positionsState.positions?.root.orEmpty(), null)
                }
                val validator = try {
                    val selection = selectDefaultValidator(chain, positionsState.positions?.root.orEmpty(), defaultValidatorSelection)
                    defaultValidatorSelection = selection
                    selection.validator
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.log("[BittensorRoot] Failed to select a default validator")
                    logger.log(e)
                    null
                }
                val annualPercentageYield = validator?.let { validatorSource.annualPercentageYield(chain, it) }
                PositionState(
                    positionsState.status,
                    validator,
                    positionsState.positions?.root.orEmpty(),
                    annualPercentageYield
                )
            }
    }

    private suspend fun selectDefaultValidator(
        chain: Chain,
        positions: List<BittensorRootPosition>,
        cachedSelection: DefaultValidatorSelection?
    ): DefaultValidatorSelection {
        val largestPositionHotkey = positions.maxByOrNull { it.stakeInPlanks }
            ?.validatorHotkey
        if (cachedSelection != null && cachedSelection.positionHotkey == largestPositionHotkey) return cachedSelection

        val validator = validatorSource.preferredValidator(chain, positions)
        return DefaultValidatorSelection(largestPositionHotkey, validator)
    }

    private data class DefaultValidatorSelection(val positionHotkey: AccountIdKey?, val validator: BittensorValidator?)

    private data class PositionState(
        val status: BittensorDataStatus,
        val validator: BittensorValidator?,
        val positions: List<BittensorRootPosition>,
        val annualPercentageYield: BigDecimal?
    )
}
