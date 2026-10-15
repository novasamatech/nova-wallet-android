package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

interface BittensorStakingRepository {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails>
}

interface BittensorRootValidatorSource {

    suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator?

    suspend fun annualPercentageYield(): BigDecimal?
}

class RealBittensorStakingRepository(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val positionSource: BittensorPositionSource,
    private val validatorSource: BittensorRootValidatorSource,
    private val chainStateRepository: ChainStateRepository,
    private val logger: Logger
) : BittensorStakingRepository {

    override fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)

        accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            combine(
                walletRepository.assetFlow(metaAccount.id, chain.utilityAsset),
                observePosition(chain, metaAccount.requireAccountIdIn(chain)),
            ) { asset, position ->
                BittensorRootStakingDetails(
                    metaId = metaAccount.id,
                    chain = chain,
                    asset = asset,
                    validator = position.validator,
                    annualPercentageYield = validatorSource.annualPercentageYield(),
                    dataStatus = position.status
                )
            }
        }
    }

    private fun observePosition(chain: Chain, coldKey: AccountId): Flow<PositionState> = flowOfAll {
        var cachedPositions: List<BittensorRootPosition>? = null
        var defaultValidatorSelection: DefaultValidatorSelection? = null
        chainStateRepository.currentRemoteBlockNumberFlow(chain.id).conflate().map {
            val status = try {
                cachedPositions = positionSource.positions(chain.id, coldKey).root
                BittensorDataStatus.Fresh
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorRoot] Failed to refresh root positions")
                logger.log(e)
                BittensorDataStatus.Stale(e)
            }
            val validator = try {
                val positions = cachedPositions.orEmpty()
                val largestPositionHotkey = positions.maxByOrNull { it.stakeInPlanks }?.validatorHotkey
                if (defaultValidatorSelection == null || defaultValidatorSelection?.positionHotkey != largestPositionHotkey) {
                    val validator = validatorSource.preferredValidator(chain, positions)
                    defaultValidatorSelection = DefaultValidatorSelection(largestPositionHotkey, validator)
                }
                defaultValidatorSelection?.validator
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorRoot] Failed to select a default validator")
                logger.log(e)
                null
            }
            PositionState(cachedPositions, status, validator)
        }
    }

    private data class DefaultValidatorSelection(val positionHotkey: AccountIdKey?, val validator: BittensorValidator?)

    private data class PositionState(val positions: List<BittensorRootPosition>?, val status: BittensorDataStatus, val validator: BittensorValidator?)
}
