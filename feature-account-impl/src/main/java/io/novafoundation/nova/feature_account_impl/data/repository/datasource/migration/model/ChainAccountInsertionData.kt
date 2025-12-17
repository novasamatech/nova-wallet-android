package io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.model

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class ChainAccountInsertionData(
    val chain: Chain,
    val cryptoType: CryptoType,
    val secrets: EncodableStruct<ChainAccountSecrets>
)
