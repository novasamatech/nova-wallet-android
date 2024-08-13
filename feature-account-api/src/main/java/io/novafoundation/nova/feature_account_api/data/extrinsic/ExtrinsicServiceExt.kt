package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

suspend fun Result<Flow<ExtrinsicStatus>>.awaitInBlock(): Result<ExtrinsicStatus.InBlock> = mapCatching {
    it.filterIsInstance<ExtrinsicStatus.InBlock>().first()
}
