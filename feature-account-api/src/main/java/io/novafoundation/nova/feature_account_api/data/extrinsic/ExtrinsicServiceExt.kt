package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

suspend fun ExtrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(
    chain: Chain,
    formExtrinsic: FormExtrinsicWithOrigin,
): Result<ExtrinsicStatus.InBlock> = runCatching {
    submitAndWatchExtrinsicWithSelectedWallet(chain, formExtrinsic)
        .filterIsInstance<ExtrinsicStatus.InBlock>()
        .first()
}
