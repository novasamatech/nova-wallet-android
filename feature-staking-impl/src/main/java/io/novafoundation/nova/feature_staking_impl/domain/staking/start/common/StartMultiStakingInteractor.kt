package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

interface StartMultiStakingInteractor {

    suspend fun calculateFee(selection: StartMultiStakingSelection): Fee

    suspend fun startStaking(selection: StartMultiStakingSelection): Result<String>
}

class RealStartMultiStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
): StartMultiStakingInteractor {

    override suspend fun calculateFee(selection: StartMultiStakingSelection): Fee {
        return extrinsicService.estimateFeeV2(selection.stakingOption.chain) {
            startStaking(selection)
        }
    }

    override suspend fun startStaking(selection: StartMultiStakingSelection): Result<String> {
        return extrinsicService.submitExtrinsicWithSelectedWallet(selection.stakingOption.chain) {
            startStaking(selection)
        }
    }

    private suspend fun ExtrinsicBuilder.startStaking(selection: StartMultiStakingSelection) {
        val account = accountRepository.getSelectedMetaAccount()

        with(selection) {
            startStaking(account)
        }
    }
}
