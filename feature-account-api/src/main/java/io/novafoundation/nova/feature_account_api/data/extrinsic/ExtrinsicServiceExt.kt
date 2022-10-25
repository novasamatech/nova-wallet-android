package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

suspend fun ExtrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(
    chain: Chain,
    formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
): Result<ExtrinsicStatus.InBlock> = runCatching {
    submitAndWatchExtrinsicWithSelectedWallet(chain, formExtrinsic)
        .filterIsInstance<ExtrinsicStatus.InBlock>()
        .first()
}
