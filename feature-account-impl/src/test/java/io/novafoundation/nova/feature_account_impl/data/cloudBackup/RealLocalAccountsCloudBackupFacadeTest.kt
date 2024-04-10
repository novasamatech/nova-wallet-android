package io.novafoundation.nova.feature_account_impl.data.cloudBackup

import io.novafoundation.feature_cloud_backup_test.TEST_MODIFIED_AT
import io.novafoundation.feature_cloud_backup_test.buildTestCloudBackup
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_impl.mock.LocalAccountsMocker
import io.novafoundation.nova.feature_account_impl.mock.SecretStoreMocker
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.BackupPriorityResolutionStrategy
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup.WalletPrivateInfo.KeyPairSecrets
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.localVsCloudDiff
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.argThat
import io.novafoundation.nova.test_shared.eq
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.encrypt.keypair.BaseKeypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class RealLocalAccountsCloudBackupFacadeTest {

    @Mock
    lateinit var metaAccountDao: MetaAccountDao

    @Mock
    lateinit var cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker

    @Mock
    lateinit var secretStore: SecretStoreV2

    lateinit var facade: RealLocalAccountsCloudBackupFacade

    val ethereumDerivationPath = "//44//60//0/0/0"


    @Before
    fun setup() {
        facade = RealLocalAccountsCloudBackupFacade(
            secretsStoreV2 = secretStore,
            accountDao = metaAccountDao,
            cloudBackupAccountsModificationsTracker = cloudBackupAccountsModificationsTracker
        )

        whenever(cloudBackupAccountsModificationsTracker.getAccountsLastModifiedAt()).thenReturn(TEST_MODIFIED_AT)
    }

    @Test
    fun fullBackupInfoForDefaultBuilderConfiguration() = runBlocking {
        val uuid = "id"

        LocalAccountsMocker.setupMocks(metaAccountDao) {
            metaAccount(0) {
                globallyUniqueId(uuid)
            }
        }

        SecretStoreMocker.setupMocks(secretStore) {
            metaAccount(0) {}
        }

        val expectedCloudBackup = buildTestCloudBackup {
            publicData {
                wallet(uuid) {}
            }

            privateData {
                wallet(uuid) {}
            }
        }

        val actualCloudBackup = facade.fullBackupInfoFromLocalSnapshot()

        assertEquals(expectedCloudBackup, actualCloudBackup)
    }

    @Test
    fun shouldConstructFullBackupOfBaseSubstrate() = runBlocking {
        val uuid = "id"

        val zero32Bytes = ByteArray(32)

        LocalAccountsMocker.setupMocks(metaAccountDao) {
            metaAccount(0) {
                globallyUniqueId(uuid)

                substrateAccountId(zero32Bytes)
                substrateCryptoType(CryptoType.SR25519)
                substratePublicKey(zero32Bytes)
            }
        }

        SecretStoreMocker.setupMocks(secretStore) {
            metaAccount(0) {
                entropy(zero32Bytes)
                seed(zero32Bytes)
                substrateKeypair(Sr25519Keypair(zero32Bytes, zero32Bytes, zero32Bytes))
            }
        }

        val expectedCloudBackup = buildTestCloudBackup {
            publicData {
                wallet(uuid) {
                    substrateAccountId(zero32Bytes)
                    substrateCryptoType(CryptoType.SR25519)
                    substratePublicKey(zero32Bytes)
                }
            }

            privateData {
                wallet(uuid) {
                    entropy(zero32Bytes)

                    substrate {
                        seed(zero32Bytes)
                        keypair(KeyPairSecrets(zero32Bytes, zero32Bytes, zero32Bytes))
                    }
                }
            }
        }

        val actualCloudBackup = facade.fullBackupInfoFromLocalSnapshot()

        assertEquals(expectedCloudBackup, actualCloudBackup)
    }

    @Test
    fun shouldConstructFullBackupOfMultipleWallets() = runBlocking {
        val walletsCount = 3

        LocalAccountsMocker.setupMocks(metaAccountDao) {
            generateWallets(walletsCount) { walletIndex, uuid, bytes32, bytes20 ->
                metaAccount(walletIndex) {
                    globallyUniqueId(uuid)

                    substrateAccountId(bytes32)
                    substrateCryptoType(CryptoType.SR25519)
                    substratePublicKey(bytes32)

                    ethereumPublicKey(bytes32)
                    ethereumAddress(bytes20)

                    chainAccount(chainId(walletIndex)) {
                        publicKey(bytes32)
                        accountId(bytes32)
                        cryptoType(CryptoType.ED25519)
                    }
                }
            }
        }

        SecretStoreMocker.setupMocks(secretStore) {
            generateWallets(walletsCount) { walletIndex, _, bytes32, _ ->
                metaAccount(walletIndex) {
                    entropy(bytes32)
                    seed(bytes32)
                    substrateKeypair(Sr25519Keypair(bytes32, bytes32, bytes32))

                    ethereumKeypair(BaseKeypair(bytes32, bytes32))
                    ethereumDerivationPath(ethereumDerivationPath)

                    chainAccount(accountId = bytes32) {
                        entropy(bytes32)
                        seed(bytes32)
                        derivationPath("//${walletIndex}")
                        keypair(BaseKeypair(bytes32, bytes32))
                    }
                }
            }
        }

        val expectedCloudBackup = buildTestCloudBackup {
            publicData {
                generateWallets(walletsCount) { index, uuid, bytes32, bytes20 ->
                    wallet(uuid) {
                        substrateAccountId(bytes32)
                        substrateCryptoType(CryptoType.SR25519)
                        substratePublicKey(bytes32)

                        ethereumPublicKey(bytes32)
                        ethereumAddress(bytes20)

                        chainAccount(chainId(index)) {
                            publicKey(bytes32)
                            accountId(bytes32)
                            cryptoType(CryptoType.ED25519)
                        }
                    }
                }
            }

            privateData {
                generateWallets(walletsCount) { index, uuid, bytes32, _ ->
                    wallet(uuid) {
                        entropy(bytes32)

                        substrate {
                            seed(bytes32)
                            keypair(KeyPairSecrets(bytes32, bytes32, bytes32))
                        }

                        ethereum {
                            entropy(bytes32)
                            derivationPath(ethereumDerivationPath)
                            keypair(KeyPairSecrets(bytes32, bytes32, nonce = null))
                        }

                        chainAccount(accountId = bytes32) {
                            entropy(bytes32)
                            seed(bytes32)
                            derivationPath("//${index}")
                            keypair(KeyPairSecrets(bytes32, bytes32, nonce = null))
                        }
                    }
                }
            }
        }

        val actualCloudBackup = facade.fullBackupInfoFromLocalSnapshot()

        assertEquals(expectedCloudBackup, actualCloudBackup)
    }

    @Test
    fun shouldApplyAddAccountDiff() = runBlocking {
        LocalAccountsMocker.setupMocks(metaAccountDao) {}
        SecretStoreMocker.setupMocks(secretStore) {}

        val localBackup = buildTestCloudBackup {
            publicData {  }

            privateData {  }
        }

        val cloudBackup = buildTestCloudBackup {
            publicData {
                generateWallets(walletsCount = 1) { index, uuid, bytes32, bytes20 ->
                    wallet(uuid) {
                        substrateAccountId(bytes32)
                        substrateCryptoType(CryptoType.SR25519)
                        substratePublicKey(bytes32)

                        ethereumPublicKey(bytes32)
                        ethereumAddress(bytes20)

                        chainAccount(chainId(index)) {
                            publicKey(bytes32)
                            accountId(bytes32)
                            cryptoType(CryptoType.ED25519)
                        }
                    }
                }
            }

            privateData {
                generateWallets(walletsCount = 1) { index, uuid, bytes32, _ ->
                    wallet(uuid) {
                        entropy(bytes32)

                        substrate {
                            seed(bytes32)
                            keypair(KeyPairSecrets(bytes32, bytes32, bytes32))
                        }

                        ethereum {
                            entropy(bytes32)
                            derivationPath(ethereumDerivationPath)
                            keypair(KeyPairSecrets(bytes32, bytes32, nonce = null))
                        }

                        chainAccount(accountId = bytes32) {
                            entropy(bytes32)
                            seed(bytes32)
                            derivationPath("//${index}")
                            keypair(KeyPairSecrets(bytes32, bytes32, nonce = null))
                        }
                    }
                }
            }
        }

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupPriorityResolutionStrategy.alwaysCloud())

        facade.applyBackupDiff(diff, cloudBackup)

        val bytes32 = bytes32of(0)

        verify(metaAccountDao).insertMetaAccount(metaAccountWithUuid(walletUUid(0)))
        verify(metaAccountDao).insertChainAccounts(singleChainAccountWithAccountId(bytes32))

        verify(secretStore).putMetaAccountSecrets(eq(0), metaAccountSecretsWithEntropy(bytes32))
        verify(secretStore).putChainAccountSecrets(eq(0), byteArrayEq(bytes32), chainAccountSecretsWithEntropy(bytes32))

        // no deletes happened
        verify(secretStore, never()).clearSecrets(anyLong(), any())
        verify(metaAccountDao, never()).delete(any<List<Long>>())

        // no modifications happened
        verify(metaAccountDao, never()).updateMetaAccount(any())
        verify(metaAccountDao, never()).deleteChainAccounts(any())
    }

    private fun chainAccountSecretsWithEntropy(entropy: ByteArray): EncodableStruct<ChainAccountSecrets> {
        return  argThat { it.entropy.contentEquals(entropy) }
    }

    private fun byteArrayEq(value: ByteArray): ByteArray = argThat { it.contentEquals(value) }

    private fun metaAccountSecretsWithEntropy(entropy: ByteArray): EncodableStruct<MetaAccountSecrets> {
        return  argThat { it.entropy.contentEquals(entropy) }
    }

    private fun metaAccountWithUuid(id: String): MetaAccountLocal {
        return argThat { it.globallyUniqueId == id }
    }

    private fun singleChainAccountWithAccountId(accountId: AccountId): List<ChainAccountLocal> {
        return argThat { it.size == 1 && it.single().accountId.contentEquals(bytes32of(0)) }
    }

    private fun generateWallets(
        walletsCount: Int,
        generator: (metaId: Long, uuid: String, bytes32: ByteArray, bytes20: ByteArray) -> Unit
    ) {
        repeat(walletsCount) { walletIndex ->
            val uuid = walletUUid(walletIndex)
            val bytes32 = bytes32of(walletIndex)
            val bytes20 = bytes20of(walletIndex)

            generator(walletIndex.toLong(), uuid, bytes32, bytes20)
        }
    }

    private fun walletUUid(idx: Int) = "id${idx}"
    private fun bytes32of(byte: Int) = ByteArray(32) { byte.toByte() }
    private fun bytes20of(byte: Int) = ByteArray(20) { byte.toByte() }

    private fun chainId(idx: Long) = "0x${idx}"
}
