package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.feature_cloud_backup_test.CloudBackupBuilder
import io.novafoundation.feature_cloud_backup_test.buildTestCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.isEmpty
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.localVsCloudDiff
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.strategy.BackupDiffStrategy
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CloudBackupExtKtTest {

    @Test
    fun shouldDiffWithEmptyLocalBackup() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("1") {
                }
            }
        }

        val walletOnlyPresentInCloud = cloudBackup.publicData.wallets.first()

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.modified.isEmpty())
        assertEquals(listOf(walletOnlyPresentInCloud), diff.localChanges.added)
    }

    @Test
    fun shouldFindAddLocalAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }

                wallet("1") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val walletOnlyPresentInCloud = cloudBackup.publicData.wallets[1]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.modified.isEmpty())
        assertEquals(listOf(walletOnlyPresentInCloud), diff.localChanges.added)
    }

    @Test
    fun shouldFindRemoveLocalAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }

                wallet("1") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val walletOnlyPresentLocally = localBackup.publicData.wallets[1]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.added.isEmpty())
        assertTrue(diff.localChanges.modified.isEmpty())
        assertEquals(listOf(walletOnlyPresentLocally), diff.localChanges.removed)
    }

    @Test
    fun shouldFindModifiedLocalAccountName() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                    name("old")
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                    name("new")
                }
            }
        }

        val modifiedLocally = cloudBackup.publicData.wallets[0]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.added.isEmpty())
        assertEquals(listOf(modifiedLocally), diff.localChanges.modified)
    }

    @Test
    fun shouldFindModifiedLocalBaseSubstrateAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32) { 1 })
                }
            }
        }

        val modifiedLocally = cloudBackup.publicData.wallets[0]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.added.isEmpty())
        assertEquals(listOf(modifiedLocally), diff.localChanges.modified)
    }

    @Test
    fun shouldFindModifiedLocalBaseEthereumAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    ethereumAddress(ByteArray(20))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    ethereumAddress(ByteArray(20) { 1 })
                }
            }
        }

        val toModifyLocally = cloudBackup.publicData.wallets[0]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.added.isEmpty())
        assertEquals(listOf(toModifyLocally), diff.localChanges.modified)
    }

    @Test
    fun shouldFindModifiedLocalChainAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    chainAccount(chainId = "0") {
                        accountId(ByteArray(32))
                    }
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    chainAccount(chainId = "0") {
                        accountId(ByteArray(32) { 1 })
                    }
                }
            }
        }

        val toModifyLocally = cloudBackup.publicData.wallets[0]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.cloudChanges.isEmpty())
        assertTrue(diff.localChanges.removed.isEmpty())
        assertTrue(diff.localChanges.added.isEmpty())
        assertEquals(listOf(toModifyLocally), diff.localChanges.modified)
    }

    @Test
    fun shouldFindAddCloudAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }

                wallet("1") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val walletOnlyPresentLocally = localBackup.publicData.wallets[1]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.localChanges.isEmpty())
        assertTrue(diff.cloudChanges.removed.isEmpty())
        assertTrue(diff.cloudChanges.modified.isEmpty())
        assertEquals(listOf(walletOnlyPresentLocally), diff.cloudChanges.added)
    }

    @Test
    fun shouldFindRemoveCloudAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }

                wallet("1") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val walletOnlyPresentInCloud = cloudBackup.publicData.wallets[1]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.localChanges.isEmpty())
        assertTrue(diff.cloudChanges.added.isEmpty())
        assertTrue(diff.cloudChanges.modified.isEmpty())
        assertEquals(listOf(walletOnlyPresentInCloud), diff.cloudChanges.removed)
    }

    @Test
    fun shouldFindModifiedCloudAccount() {
        val localBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(1)

                wallet("0") {
                    substrateAccountId(ByteArray(32) { 1 })
                }
            }
        }

        val cloudBackup = buildTestCloudBackupWithoutPrivate {
            publicData {
                modifiedAt(0)

                wallet("0") {
                    substrateAccountId(ByteArray(32))
                }
            }
        }

        val toModifyInCloud = localBackup.publicData.wallets[0]

        val diff = localBackup.localVsCloudDiff(cloudBackup, BackupDiffStrategy.syncWithCloud())

        assertTrue(diff.localChanges.isEmpty())
        assertTrue(diff.cloudChanges.removed.isEmpty())
        assertTrue(diff.cloudChanges.added.isEmpty())
        assertEquals(listOf(toModifyInCloud), diff.cloudChanges.modified)
    }

    private inline fun buildTestCloudBackupWithoutPrivate(crossinline builder: CloudBackupBuilder.() -> Unit) = buildTestCloudBackup {
        builder()

        privateData {

        }
    }
}
