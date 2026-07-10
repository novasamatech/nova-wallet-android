package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.hasConstant
import io.novafoundation.nova.common.utils.hasStorage
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.activeEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.areNominatorsSlashableOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.lastValidatorEraOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.validators
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.duration.UnstakingDurationInEras
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.duration.UnstakingDurationVariant
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull

private const val NOMINATOR_FAST_UNBOND_DURATION = "NominatorFastUnbondDuration"
private const val ARE_NOMINATORS_SLASHABLE = "AreNominatorsSlashable"

interface UnstakingDurationRepository {

    suspend fun getUnstakingDurationInEras(chainId: ChainId): UnstakingDurationInEras

    suspend fun getStashUnstakingVariant(chainId: ChainId, stashAccountId: AccountId): UnstakingDurationVariant
}

class RealUnstakingDurationRepository(
    private val chainRegistry: ChainRegistry,
    private val remoteStorageSource: StorageDataSource,
    private val stakingConstantsRepository: StakingConstantsRepository,
) : UnstakingDurationRepository {

    override suspend fun getUnstakingDurationInEras(chainId: ChainId): UnstakingDurationInEras {
        val bondingDuration = stakingConstantsRepository.lockupPeriodInEras(chainId)

        if (!hasNominatorFastUnbond(chainId)) {
            return UnstakingDurationInEras(validator = bondingDuration, nominator = bondingDuration)
        }

        val fastUnbondDuration = stakingConstantsRepository.nominatorFastUnbondDurationInErasOrNull(chainId)
            ?: return UnstakingDurationInEras(validator = bondingDuration, nominator = bondingDuration)

        // Unset storage value means the pallet default — nominators are slashable.
        val areNominatorsSlashable = remoteStorageSource.query(chainId) {
            metadata.staking.areNominatorsSlashableOrNull?.query() ?: true
        }

        val nominatorDuration = if (areNominatorsSlashable) bondingDuration else fastUnbondDuration

        return UnstakingDurationInEras(validator = bondingDuration, nominator = nominatorDuration)
    }

    override suspend fun getStashUnstakingVariant(
        chainId: ChainId,
        stashAccountId: AccountId
    ): UnstakingDurationVariant {
        if (!hasNominatorFastUnbond(chainId)) return UnstakingDurationVariant.FULL

        return remoteStorageSource.query(chainId) {
            val validatorPrefs = metadata.staking.validators.query(stashAccountId)
            if (validatorPrefs != null) {
                return@query UnstakingDurationVariant.FULL
            }

            val lastValidatorEra = metadata.staking.lastValidatorEraOrNull?.query(stashAccountId)
                ?: return@query UnstakingDurationVariant.NOMINATOR

            val activeEra = metadata.staking.activeEra.query()
                ?: return@query UnstakingDurationVariant.NOMINATOR

            val bondingDuration = stakingConstantsRepository.lockupPeriodInEras(chainId)

            if (lastValidatorEra + bondingDuration >= activeEra) {
                UnstakingDurationVariant.FULL
            } else {
                UnstakingDurationVariant.NOMINATOR
            }
        }
    }

    private suspend fun hasNominatorFastUnbond(chainId: ChainId): Boolean {
        val metadata = chainRegistry.getRuntime(chainId).metadata
        val stakingModule = metadata.moduleOrNull(Modules.STAKING) ?: return false

        return metadata.hasConstant(Modules.STAKING, NOMINATOR_FAST_UNBOND_DURATION) &&
            stakingModule.hasStorage(ARE_NOMINATORS_SLASHABLE)
    }
}
