package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

typealias SplitCalls = List<List<GenericCall.Instance>>

interface ExtrinsicSplitter {

    suspend fun split(signer: NovaSigner, callBuilder: CallBuilder, chain: Chain): SplitCalls

    suspend fun estimateCallWeight(signer: NovaSigner, call: GenericCall.Instance, chain: Chain): WeightV2
}
