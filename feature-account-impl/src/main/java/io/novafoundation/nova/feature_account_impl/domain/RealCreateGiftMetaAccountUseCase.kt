package io.novafoundation.nova.feature_account_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.publicKey
import io.novafoundation.nova.feature_account_api.domain.interfaces.CreateGiftMetaAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.RealSecretsMetaAccount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import java.util.UUID
import kotlin.Long

class RealCreateGiftMetaAccountUseCase : CreateGiftMetaAccountUseCase {

    override fun createTemporaryGiftMetaAccount(chain: Chain, chainSecrets: EncodableStruct<ChainAccountSecrets>): MetaAccount {
        val publicKey = chainSecrets.keypair.publicKey
        val chainAccount = MetaAccount.ChainAccount(
            metaId = Long.MAX_VALUE,
            chainId = chain.id,
            publicKey = publicKey,
            accountId = chain.accountIdOf(publicKey),
            cryptoType = null,
        )

        return RealSecretsMetaAccount(
            id = Long.MAX_VALUE,
            globallyUniqueId = UUID.randomUUID().toString(),
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumAddress = null,
            ethereumPublicKey = null,
            isSelected = false,
            name = "Temporary Meta Account",
            status = LightMetaAccount.Status.ACTIVE,
            chainAccounts = mapOf(chain.id to chainAccount),
            parentMetaId = null,
        )
    }
}
