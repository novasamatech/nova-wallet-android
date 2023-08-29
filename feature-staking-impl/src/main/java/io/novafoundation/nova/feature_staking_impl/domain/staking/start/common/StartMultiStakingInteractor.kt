package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StartMultiStakingInteractor {

    suspend fun calculateFee(selection: StartMultiStakingSelection): Fee

    suspend fun startStaking(selection: StartMultiStakingSelection): Result<ExtrinsicStatus.InBlock>
}

class RealStartMultiStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
) : StartMultiStakingInteractor {

    override suspend fun calculateFee(selection: StartMultiStakingSelection): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFeeV2(selection.stakingOption.chain) {
                startStaking(selection)
            }
        }
    }

    override suspend fun startStaking(selection: StartMultiStakingSelection): Result<ExtrinsicStatus.InBlock> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(selection.stakingOption.chain) {
                startStaking(selection)
            }
        }
    }

    private suspend fun ExtrinsicBuilder.startStaking(selection: StartMultiStakingSelection) {
        val account = accountRepository.getSelectedMetaAccount()

        with(selection) {
            startStaking(account)
        }
    }
}
