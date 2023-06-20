package io.novafoundation.nova.feature_staking_impl.domain.staking.controller

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.setController
import io.novafoundation.nova.feature_staking_impl.data.repository.ControllersDeprecationStage
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingVersioningRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.multiAddressOf
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class ControllerInteractor(
    private val extrinsicService: ExtrinsicService,
    private val sharedStakingSate: StakingSharedState,
    private val stakingVersioningRepository: StakingVersioningRepository,
) {

    suspend fun controllerDeprecationStage(): ControllersDeprecationStage {
        return withContext(Dispatchers.Default) {
            val chain = sharedStakingSate.chain()

            stakingVersioningRepository.controllersDeprecationStage(chain.id)
        }
    }

    suspend fun estimateFee(controllerAccountAddress: String): BigInteger {
        return withContext(Dispatchers.IO) {
            val chain = sharedStakingSate.chain()

            extrinsicService.estimateFee(chain) {
                setController(chain.multiAddressOf(controllerAccountAddress))
            }
        }
    }

    suspend fun setController(stashAccountAddress: String, controllerAccountAddress: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val chain = sharedStakingSate.chain()
            val accountId = chain.accountIdOf(stashAccountAddress)

            extrinsicService.submitExtrinsicWithAnySuitableWallet(chain, accountId) {
                setController(chain.multiAddressOf(controllerAccountAddress))
            }
        }
    }
}
