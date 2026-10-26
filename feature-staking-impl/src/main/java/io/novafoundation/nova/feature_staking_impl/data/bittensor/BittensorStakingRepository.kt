package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

interface BittensorStakingRepository {

    suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal? = null

    fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails>

    fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails>
}

interface BittensorRootValidatorSource {

    suspend fun validatorDetails(chain: Chain): Map<AccountIdKey, BittensorRootValidatorDetails>

    suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal?
}

class RealBittensorStakingRepository(
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: BittensorPositionRepository,
    private val validatorSource: BittensorRootValidatorSource
) : BittensorStakingRepository {

    override suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator) =
        validatorSource.annualPercentageYield(chain, validator)

    override fun observeRootStakingDetails(): Flow<BittensorRootStakingDetails> =
        observeRootStakingDetails(accountRepository.selectedMetaAccountFlow())

    override fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails> =
        observeRootStakingDetails(accountRepository.metaAccountFlow(metaId))

    private fun observeRootStakingDetails(accounts: Flow<MetaAccount>): Flow<BittensorRootStakingDetails> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)

        accounts
            .flatMapLatest { metaAccount ->
                combine(
                    walletRepository.assetFlow(metaAccount.id, chain.utilityAsset),
                    positionRepository.observePositions(chain.id, metaAccount.requireAccountIdIn(chain))
                ) { asset, positionsState ->
                    BittensorRootStakingDetails(
                        metaId = metaAccount.id,
                        chain = chain,
                        asset = asset,
                        validator = null,
                        annualPercentageYield = null,
                        positions = positionsState.positions?.root.orEmpty(),
                        dataStatus = positionsState.status
                    )
                }
            }
    }
}
