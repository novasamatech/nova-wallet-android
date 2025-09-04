package io.novafoundation.nova.feature_staking_impl.domain.setup

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.controllerTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.nominate
import io.novafoundation.nova.runtime.ext.multiAddressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChangeValidatorsInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
) {

    suspend fun estimateFee(validatorAccountIds: List<String>, stakingState: StakingState.Stash): Fee {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain, stakingState.controllerTransactionOrigin()) {
            formExtrinsic(chain, validatorAccountIds)
        }
    }

    suspend fun changeValidators(
        stakingState: StakingState.Stash,
        validatorAccountIds: List<String>
    ): Result<ExtrinsicSubmission> = withContext(Dispatchers.Default) {
        val chain = stakingSharedState.chain()

        extrinsicService.submitExtrinsic(chain, stakingState.controllerTransactionOrigin()) {
            formExtrinsic(chain, validatorAccountIds)
        }
    }

    private fun ExtrinsicBuilder.formExtrinsic(
        chain: Chain,
        validatorAccountIdsHex: List<String>,
    ) {
        val validatorsIds = validatorAccountIdsHex.map(String::fromHex)
        val targets = validatorsIds.map(chain::multiAddressOf)

        nominate(targets)
    }
}
