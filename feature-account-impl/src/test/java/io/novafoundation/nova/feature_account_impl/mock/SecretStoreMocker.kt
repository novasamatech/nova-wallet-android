package io.novafoundation.nova.feature_account_impl.mock

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import org.mockito.ArgumentMatchers.anyLong

@DslMarker
annotation class SecretStoreSdl

object SecretStoreMocker {

    @SecretStoreSdl
    suspend fun setupMocks(
        secretStoreV2: SecretStoreV2,
        mockBuilder: SecretStoreMockerBuilder.() -> Unit
    ) {
        val allSecrets = SecretStoreMockerBuilder().apply(mockBuilder).build()

        whenever(secretStoreV2.getMetaAccountSecrets(anyLong())).then { invocation ->
            val id = invocation.arguments.first() as Long

            allSecrets[id]?.metaAccount
        }

        whenever(secretStoreV2.getChainAccountSecrets(anyLong(), any())).then { invocation ->
            val id = invocation.arguments.first() as Long
            val accountId = invocation.arguments[1] as AccountId

            allSecrets[id]?.chainAccounts?.get(accountId.intoKey())
        }

        whenever(secretStoreV2.getAdditionalMetaAccountSecret(anyLong(), any())).then { invocation ->
            val id = invocation.arguments.first() as Long
            val secretName = invocation.arguments[1] as String

            allSecrets[id]?.additional?.get(secretName)
        }
    }
}

class MockMetaAccountSecrets(
    val metaAccount: EncodableStruct<MetaAccountSecrets>?,
    val chainAccounts: Map<AccountIdKey, EncodableStruct<ChainAccountSecrets>>,
    val additional: Map<String, String>
)

@SecretStoreSdl
class SecretStoreMockerBuilder() {

    private val secrets = mutableMapOf<Long, MockMetaAccountSecrets>()

    fun metaAccount(metaId: Long, builder: MetaAccountSecretsMockBuilder.() -> Unit) {
        val built = MetaAccountSecretsMockBuilder(metaId).apply(builder)

        secrets[metaId] = MockMetaAccountSecrets(
            metaAccount = built.buildMetaAccountSecrets(),
            chainAccounts = built.buildChainAccountSecrets(),
            additional = built.buildAdditional()
        )
    }

    fun build(): Map<Long, MockMetaAccountSecrets> {
        return secrets
    }
}

@SecretStoreSdl
class MetaAccountSecretsMockBuilder(
    private val metaId: Long,
) {

    private val chainAccountSecrets = mutableMapOf<AccountIdKey, EncodableStruct<ChainAccountSecrets>>()

    private var _entropy: ByteArray? = null
    private var _seed: ByteArray? = null
    private var _substrateDerivationPath: String? = null
    private var _substrateKeypair: Keypair? = null
    private var _ethereumKeypair: Keypair? = null
    private var _ethereumDerivationPath: String? = null

    private var additional = mapOf<String, String>()

    fun chainAccount(accountId: AccountId, builder: ChainAccountSecretsMockBuilder.() -> Unit) {
        val chainAccountSecret = ChainAccountSecretsMockBuilder(metaId).apply(builder).build()
        chainAccountSecrets[accountId.intoKey()] = chainAccountSecret
    }

    fun additional(builder: MutableMap<String, String>.() -> Unit) {
        additional = buildMap(builder)
    }

    fun entropy(value: ByteArray) {
        _entropy = value
    }

    fun seed(value: ByteArray) {
        _seed = value
    }

    fun substrateDerivationPath(value: String?) {
        _substrateDerivationPath = value
    }

    fun substrateKeypair(keypair: Keypair) {
        _substrateKeypair = keypair
    }

    fun ethereumKeypair(value: Keypair?) {
        _ethereumKeypair = value
    }

    fun ethereumDerivationPath(ethereumDerivationPath: String?) {
        _ethereumDerivationPath = ethereumDerivationPath
    }

    fun buildMetaAccountSecrets(): EncodableStruct<MetaAccountSecrets>? {
        return _substrateKeypair?.let {
            MetaAccountSecrets(
                substrateKeyPair = it,
                entropy = _entropy,
                seed = _seed,
                substrateDerivationPath = _substrateDerivationPath,
                ethereumKeypair = _ethereumKeypair,
                ethereumDerivationPath = _ethereumDerivationPath
            )
        }
    }

    fun buildChainAccountSecrets(): Map<AccountIdKey, EncodableStruct<ChainAccountSecrets>> {
        return chainAccountSecrets
    }

    fun buildAdditional(): Map<String, String> = additional
}

@SecretStoreSdl
class ChainAccountSecretsMockBuilder(
    private val metaId: Long,
) {

    private var _keypair: Keypair = Sr25519Keypair(
        privateKey = ByteArray(32),
        publicKey = ByteArray(32),
        nonce = ByteArray(32)
    )
    private var _entropy: ByteArray? = null
    private var _seed: ByteArray? = null
    private var _derivationPath: String? = null

    fun entropy(value: ByteArray) {
        _entropy = value
    }

    fun seed(value: ByteArray) {
        _seed = value
    }

    fun derivationPath(value: String?) {
        _derivationPath = value
    }

    fun keypair(keypair: Keypair) {
        _keypair = keypair
    }

    fun build(): EncodableStruct<ChainAccountSecrets> {
        return ChainAccountSecrets(
            keyPair = _keypair,
            entropy = _entropy,
            seed = _seed,
            derivationPath = _derivationPath
        )
    }
}
