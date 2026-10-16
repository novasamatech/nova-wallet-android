package io.novafoundation.nova.feature_staking_impl.data.bittensor

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

interface BittensorStakingRepository {

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails>

    fun refreshPosition()
}

interface BittensorRootValidatorSource {

    suspend fun preferredValidator(chain: Chain): BittensorValidator

    suspend fun annualPercentageYield(): BigDecimal?
}

class RealBittensorStakingRepository(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val positionSource: BittensorPositionSource,
    private val validatorSource: BittensorRootValidatorSource,
    private val positionUpdates: BittensorPositionUpdates,
    private val selection: BittensorRootValidatorSelection
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
                    annualPercentageYield = position.apy,
                    position = position.position,
                    dataStatus = position.status
                )
            }
        }
    }

    override fun refreshPosition() {
        positionUpdates.refresh()
    }

    private fun observePosition(chain: Chain, coldkey: ByteArray): Flow<PositionState> {
        var cached: PositionState? = null
        return combine(positionUpdates.updates, selection.address) { _, _ -> Unit }.mapLatest {
            val validator = validatorSource.preferredValidator(chain)
            try {
                val position = positionSource.rootPositions(chain.id, coldkey).firstOrNull { it.validatorHotkey == validator.hotkey }
                PositionState(validator, validatorSource.annualPercentageYield(), position, BittensorDataStatus.Fresh).also { cached = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val previous = cached?.takeIf { it.validator.hotkey == validator.hotkey }
                PositionState(validator, previous?.apy, previous?.position, BittensorDataStatus.Stale(e))
            }
        }
    }

    private data class PositionState(
        val validator: BittensorValidator,
        val apy: BigDecimal?,
        val position: BittensorRootPosition?,
        val status: BittensorDataStatus
    )
}
