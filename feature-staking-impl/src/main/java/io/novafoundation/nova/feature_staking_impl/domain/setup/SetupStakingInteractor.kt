package io.novafoundation.nova.feature_staking_impl.domain.setup

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.nominate
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.multiAddressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class SetupStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
) {

    suspend fun estimateFee(validatorAccountIds: List<String>): BigInteger {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain) {
            formExtrinsic(chain, validatorAccountIds)
        }
    }

    suspend fun changeValidators(
        controllerAddress: String,
        validatorAccountIds: List<String>
    ): Result<String> = withContext(Dispatchers.Default) {
        val chain = stakingSharedState.chain()
        val accountId = chain.accountIdOf(controllerAddress)

        extrinsicService.submitExtrinsicWithAnySuitableWallet(chain, accountId) {
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
