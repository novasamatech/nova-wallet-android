package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BittensorValidatorInteractor(
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val chainRegistry: ChainRegistry
) {

    suspend fun chain(): Chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)

    suspend fun validators(subnetId: Int): List<BittensorSubnetValidator> {
        val chain = chain()
        return if (subnetId == 0) validatorRepository.rootValidators(chain) else validatorRepository.validators(chain, BittensorSubnetId(subnetId))
    }
}
