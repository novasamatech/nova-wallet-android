package io.novafoundation.nova.feature_pay_impl.domain.common

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.ext.polkadot
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface ShopAccountSeedAccessPolicy {

    suspend fun isMetaAccountValid(metaAccount: MetaAccount): Boolean

    suspend fun getSeedFor(metaAccount: MetaAccount): ByteArray?
}

class RealShopAccountSeedAccessPolicy(
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry
) : ShopAccountSeedAccessPolicy {
    override suspend fun isMetaAccountValid(metaAccount: MetaAccount): Boolean {
        return metaAccount.type == LightMetaAccount.Type.SECRETS &&
            getSeedFor(metaAccount) != null
    }

    override suspend fun getSeedFor(metaAccount: MetaAccount): ByteArray? {
        return secretStoreV2.getAccountSecrets(metaAccount, chainRegistry.polkadot()).seed()
    }
}
