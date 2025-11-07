package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface CreateGiftMetaAccountUseCase {

    fun createTemporaryGiftMetaAccount(chain: Chain, chainSecrets: EncodableStruct<ChainAccountSecrets>): MetaAccount
}
