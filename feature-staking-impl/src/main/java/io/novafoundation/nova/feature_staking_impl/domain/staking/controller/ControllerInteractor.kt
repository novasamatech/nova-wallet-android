package io.novafoundation.nova.feature_staking_impl.domain.staking.controller

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.stashTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.setController
import io.novafoundation.nova.feature_staking_impl.data.repository.ControllersDeprecationStage
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingVersioningRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.multiAddressOf
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun estimateFee(controllerAccountAddress: String, stakingState: StakingState.Stash): Fee {
        return withContext(Dispatchers.IO) {
            val chain = sharedStakingSate.chain()

            extrinsicService.estimateFee(chain, stakingState.stashTransactionOrigin()) {
                setController(chain.multiAddressOf(controllerAccountAddress))
            }
        }
    }

    suspend fun setController(stashAccountAddress: String, controllerAccountAddress: String): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            val chain = sharedStakingSate.chain()
            val accountId = chain.accountIdOf(stashAccountAddress)

            extrinsicService.submitExtrinsic(chain, accountId.intoOrigin()) {
                setController(chain.multiAddressOf(controllerAccountAddress))
            }
        }
    }
}
