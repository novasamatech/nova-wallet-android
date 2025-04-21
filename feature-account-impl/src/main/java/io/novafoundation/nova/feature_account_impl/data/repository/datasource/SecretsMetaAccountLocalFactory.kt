package io.novafoundation.nova.feature_account_impl.data.repository.datasource

import io.novafoundation.nova.common.data.secrets.v2.KeyPairSchema
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.utils.substrateAccountId
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface SecretsMetaAccountLocalFactory {

    fun create(
        name: String,
        substrateCryptoType: CryptoType,
        secrets: EncodableStruct<MetaAccountSecrets>,
        accountSortPosition: Int,
    ): MetaAccountLocal
}

class RealSecretsMetaAccountLocalFactory : SecretsMetaAccountLocalFactory {

    override fun create(
        name: String,
        substrateCryptoType: CryptoType,
        secrets: EncodableStruct<MetaAccountSecrets>,
        accountSortPosition: Int,
    ): MetaAccountLocal {
        val substratePublicKey = secrets[MetaAccountSecrets.SubstrateKeypair][KeyPairSchema.PublicKey]
        val ethereumPublicKey = secrets[MetaAccountSecrets.EthereumKeypair]?.get(KeyPairSchema.PublicKey)

        return MetaAccountLocal(
            substratePublicKey = substratePublicKey,
            substrateCryptoType = substrateCryptoType,
            substrateAccountId = substratePublicKey.substrateAccountId(),
            ethereumPublicKey = ethereumPublicKey,
            ethereumAddress = ethereumPublicKey?.asEthereumPublicKey()?.toAccountId()?.value,
            name = name,
            parentMetaId = null,
            isSelected = false,
            position = accountSortPosition,
            type = MetaAccountLocal.Type.SECRETS,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
            typeExtras = null
        )
    }
}
