package io.novafoundation.nova.feature_account_impl.mock

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.JoinedMetaAccountInfo
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.RelationJoinedMetaAccountInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import kotlinx.coroutines.runBlocking

@DslMarker
annotation class LocalAccountsMockerDsl

object LocalAccountsMocker {

    @LocalAccountsMockerDsl
    suspend fun setupMocks(
        dao: MetaAccountDao,
        mockBuilder: LocalAccountsMockerBuilder.() -> Unit
    ) {
        val allJoinedMetaAccountInfo = LocalAccountsMockerBuilder().apply(mockBuilder).build()

        var metaAccountCounter = allJoinedMetaAccountInfo.size

        whenever(dao.runInTransaction(any())).then { invocation ->
            val txAction = invocation.arguments.first() as suspend () -> Unit

            runBlocking { txAction() }
        }

        whenever(dao.nextAccountPosition()).thenReturn(0)

        whenever(dao.insertMetaAccount(any())).thenAnswer {
            metaAccountCounter++
        }

        whenever(dao.getMetaAccountsByStatus(any())).thenAnswer { invocation ->
            val status = invocation.arguments.first() as MetaAccountLocal.Status

            allJoinedMetaAccountInfo.filter { it.metaAccount.status == status }
        }
    }
}

@LocalAccountsMockerDsl
class LocalAccountsMockerBuilder {

    private val metaAccounts = mutableListOf<JoinedMetaAccountInfo>()

    fun metaAccount(metaId: Long, builder: LocalMetaAccountMockBuilder.() -> Unit) {
        metaAccounts.add(LocalMetaAccountMockBuilder(metaId).apply(builder).build())
    }

    fun build(): List<JoinedMetaAccountInfo> {
        return metaAccounts
    }
}

@LocalAccountsMockerDsl
class LocalMetaAccountMockBuilder(
    private val metaId: Long,
) {

    private val chainAccounts = mutableListOf<ChainAccountLocal>()

    private var _substratePublicKey: ByteArray? = null
    private var _substrateCryptoType: CryptoType? = null
    private var _substrateAccountId: ByteArray? = null
    private var _ethereumPublicKey: ByteArray? = null
    private var _ethereumAddress: ByteArray? = null
    private var _name: String = ""
    private val _parentMetaId: Long? = null
    private var _isSelected: Boolean = false
    private var _position: Int = 0
    private var _type: MetaAccountLocal.Type = MetaAccountLocal.Type.SECRETS
    private var _status: MetaAccountLocal.Status = MetaAccountLocal.Status.ACTIVE
    private var _globallyUniqueId: String = MetaAccountLocal.generateGloballyUniqueId()


    fun chainAccount(chainId: ChainId, builder: LocalChainAccountMockBuilder.() -> Unit) {
        val chainAccountLocal = LocalChainAccountMockBuilder(metaId, chainId).apply(builder).build()
        chainAccounts.add(chainAccountLocal)
    }

    fun substratePublicKey(value: ByteArray?) {
        _substratePublicKey = value
    }

    fun substrateCryptoType(value: CryptoType?) {
        _substrateCryptoType = value
    }

    fun substrateAccountId(value: ByteArray?) {
        _substrateAccountId = value
    }

    fun ethereumPublicKey(value: ByteArray?) {
        _ethereumPublicKey = value
    }

    fun ethereumAddress(value: ByteArray?) {
        _ethereumAddress = value
    }

    fun name(value: String) {
        _name = value
    }

    fun isSelected(value: Boolean) {
        _isSelected = value
    }

    fun position(value: Int) {
        _position = value
    }

    fun type(value: MetaAccountLocal.Type) {
        _type = value
    }

    fun status(value: MetaAccountLocal.Status) {
        _status = value
    }

    fun globallyUniqueId(value: String) {
        _globallyUniqueId = value
    }

    fun build(): JoinedMetaAccountInfo {
        return RelationJoinedMetaAccountInfo(
            metaAccount = MetaAccountLocal(
                _substratePublicKey,
                _substrateCryptoType,
                _substrateAccountId,
                _ethereumPublicKey,
                _ethereumAddress,
                _name,
                _parentMetaId,
                _isSelected,
                _position,
                _type,
                _status,
                _globallyUniqueId,
            ).also {
                it.id = metaId
            },
            chainAccounts = chainAccounts,
            proxyAccountLocal = null
        )
    }
}

@LocalAccountsMockerDsl
class LocalChainAccountMockBuilder(
    private val metaId: Long,
    private val chainId: ChainId,
) {

    private var _publicKey: ByteArray? = null
    private var _accountId = ByteArray(32)
    private var _cryptoType: CryptoType? = null

    fun publicKey(value: ByteArray) {
        _publicKey = value
    }

    fun accountId(value: ByteArray) {
        _accountId = value
    }

    fun cryptoType(cryptoType: CryptoType) {
        _cryptoType = cryptoType
    }

    fun build(): ChainAccountLocal {
        return ChainAccountLocal(metaId, chainId, _publicKey, _accountId, _cryptoType)
    }
}
