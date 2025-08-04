package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common

import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.flattenDispatchFailure
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StartMultiStakingInteractor {

    suspend fun calculateFee(selection: StartMultiStakingSelection): Fee

    suspend fun startStaking(selection: StartMultiStakingSelection): Result<Unit>
}

class RealStartMultiStakingInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
) : StartMultiStakingInteractor {

    override suspend fun calculateFee(selection: StartMultiStakingSelection): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(selection.stakingOption.chain, TransactionOrigin.SelectedWallet) {
                startStaking(selection)
            }
        }
    }

    override suspend fun startStaking(selection: StartMultiStakingSelection): Result<Unit> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsicAndAwaitExecution(selection.stakingOption.chain, TransactionOrigin.SelectedWallet) {
                startStaking(selection)
            }
                .flattenDispatchFailure()
                .coerceToUnit()
        }
    }

    private suspend fun ExtrinsicBuilder.startStaking(selection: StartMultiStakingSelection) {
        val account = accountRepository.getSelectedMetaAccount()

        with(selection) {
            startStaking(account)
        }
    }
}
