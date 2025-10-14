package io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.model

import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class MetaAccountInsertionData(
    val name: String,
    val substrateCryptoType: CryptoType,
    val secrets: EncodableStruct<MetaAccountSecrets>
)
