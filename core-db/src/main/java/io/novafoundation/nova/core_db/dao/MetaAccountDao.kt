package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountIdsLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountPositionUpdate
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.RelationJoinedMetaAccountInfo
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Fetch meta account where either
 * 1. chain account for specified chain is present and its accountId matches
 * 2. chain account for specified is missing but one of base accountIds matches
 *
 * Note that if both chain account and base accounts are present than we should filter out entries where chain account matches but base accounts does not
 */
@Language("RoomSql")
private const val FIND_BY_ADDRESS_WHERE_CLAUSE = """
    LEFT JOIN chain_accounts as c ON m.id = c.metaId AND c.chainId = :chainId
    WHERE
    (c.accountId IS NOT NULL AND c.accountId = :accountId)
    OR (c.accountId IS NULL AND (substrateAccountId = :accountId OR ethereumAddress = :accountId))
    ORDER BY (CASE WHEN isSelected THEN 0 ELSE 1 END)
    """

@Language("RoomSql")
private const val FIND_ACCOUNT_BY_ADDRESS_QUERY = """
            SELECT * FROM meta_accounts as m
            $FIND_BY_ADDRESS_WHERE_CLAUSE
"""

@Language("RoomSql")
private const val FIND_NAME_BY_ADDRESS_QUERY = """
            SELECT name FROM meta_accounts as m
            $FIND_BY_ADDRESS_WHERE_CLAUSE
"""

@Language("RoomSql")
private const val META_ACCOUNTS_WITH_BALANCE_PART = """
    SELECT 
    m.id, 
    a.freeInPlanks, 
    a.reservedInPlanks, 
    (SELECT SUM(amountInPlanks) FROM contributions WHERE chainId = a.chainId AND assetId = a.assetId AND metaId = m.id) offChainBalance,
    ca.precision, 
    t.rate
    FROM meta_accounts as m
    INNER JOIN assets as a ON  a.metaId = m.id
    INNER JOIN chain_assets AS ca ON a.assetId = ca.id AND a.chainId = ca.chainId
    INNER JOIN currencies as currency ON currency.selected = 1
    INNER JOIN tokens as t ON t.tokenSymbol = ca.symbol AND t.currencyId = currency.id
"""

@Language("RoomSql")
private const val META_ACCOUNTS_WITH_BALANCE_QUERY = """
    $META_ACCOUNTS_WITH_BALANCE_PART
    ORDER BY m.position
"""

@Language("RoomSql")
private const val META_ACCOUNT_WITH_BALANCE_QUERY = """
    $META_ACCOUNTS_WITH_BALANCE_PART
    WHERE m.id == :metaId
"""

@Dao
interface MetaAccountDao {

    @Transaction
    suspend fun insertProxiedMetaAccount(
        metaAccount: MetaAccountLocal,
        chainAccount: (metaId: Long) -> ChainAccountLocal,
        proxyAccount: (metaId: Long) -> ProxyAccountLocal
    ): Long {
        val metaId = insertMetaAccount(metaAccount)
        insertChainAccount(chainAccount(metaId))
        insertProxy(proxyAccount(metaId))

        return metaId
    }

    @Transaction
    suspend fun withTransaction(action: suspend () -> Unit) {
        action()
    }

    @Insert
    suspend fun insertMetaAccount(metaAccount: MetaAccountLocal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMetaAccount(metaAccount: MetaAccountLocal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainAccount(chainAccount: ChainAccountLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainAccounts(chainAccounts: List<ChainAccountLocal>)

    @Delete
    suspend fun deleteChainAccounts(chainAccounts: List<ChainAccountLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxyLocal: ProxyAccountLocal)

    @Query("SELECT * FROM meta_accounts")
    suspend fun getMetaAccounts(): List<MetaAccountLocal>

    @Query("SELECT globallyUniqueId, id FROM meta_accounts")
    suspend fun getMetaAccountIds(): List<MetaAccountIdsLocal>

    @Query("SELECT * FROM meta_accounts WHERE status = :status")
    @Transaction
    suspend fun getMetaAccountsInfoByStatus(status: MetaAccountLocal.Status): List<RelationJoinedMetaAccountInfo>

    @Query("SELECT COUNT(*) FROM meta_accounts WHERE status = :status")
    @Transaction
    suspend fun getMetaAccountsQuantityByStatus(status: MetaAccountLocal.Status): Int

    @Query("SELECT id FROM meta_accounts WHERE status = :status")
    suspend fun getMetaAccountIdsByStatus(status: MetaAccountLocal.Status): List<Long>

    @Query("SELECT * FROM meta_accounts WHERE status = :status")
    suspend fun getMetaAccountsByStatus(status: MetaAccountLocal.Status): List<RelationJoinedMetaAccountInfo>

    @Query("SELECT * FROM meta_accounts")
    suspend fun getMetaAccountsInfo(): List<MetaAccountLocal>

    @Query("SELECT * FROM meta_accounts")
    fun getJoinedMetaAccountsInfoFlow(): Flow<List<RelationJoinedMetaAccountInfo>>

    @Query("SELECT * FROM meta_accounts WHERE status = :status")
    fun getJoinedMetaAccountsInfoByStatusFlow(status: MetaAccountLocal.Status): Flow<List<RelationJoinedMetaAccountInfo>>

    @Query(META_ACCOUNTS_WITH_BALANCE_QUERY)
    fun metaAccountsWithBalanceFlow(): Flow<List<MetaAccountWithBalanceLocal>>

    @Query(META_ACCOUNT_WITH_BALANCE_QUERY)
    fun metaAccountWithBalanceFlow(metaId: Long): Flow<List<MetaAccountWithBalanceLocal>>

    @Query("SELECT * FROM proxy_accounts WHERE chainId = :chainId")
    suspend fun getProxyAccounts(chainId: String): List<ProxyAccountLocal>

    @Query("UPDATE meta_accounts SET isSelected = (id = :metaId)")
    suspend fun selectMetaAccount(metaId: Long)

    @Update(entity = MetaAccountLocal::class)
    suspend fun updatePositions(updates: List<MetaAccountPositionUpdate>)

    @Query("SELECT * FROM meta_accounts WHERE id = :metaId")
    @Transaction
    suspend fun getJoinedMetaAccountInfo(metaId: Long): RelationJoinedMetaAccountInfo

    @Query("SELECT type FROM meta_accounts WHERE id = :metaId")
    suspend fun getMetaAccountType(metaId: Long): MetaAccountLocal.Type?

    @Query("SELECT * FROM meta_accounts WHERE isSelected = 1")
    @Transaction
    fun selectedMetaAccountInfoFlow(): Flow<RelationJoinedMetaAccountInfo?>

    @Query("SELECT * FROM meta_accounts WHERE id = :metaId")
    @Transaction
    fun metaAccountInfoFlow(metaId: Long): Flow<RelationJoinedMetaAccountInfo?>

    @Query("SELECT EXISTS ($FIND_ACCOUNT_BY_ADDRESS_QUERY)")
    fun isMetaAccountExists(accountId: AccountId, chainId: String): Boolean

    @Query(FIND_ACCOUNT_BY_ADDRESS_QUERY)
    @Transaction
    fun getMetaAccountInfo(accountId: AccountId, chainId: String): RelationJoinedMetaAccountInfo?

    @Query(FIND_NAME_BY_ADDRESS_QUERY)
    fun metaAccountNameFor(accountId: AccountId, chainId: String): String?

    @Query("UPDATE meta_accounts SET name = :newName WHERE id = :metaId")
    suspend fun updateName(metaId: Long, newName: String)

    @Query(
        """
        WITH RECURSIVE accounts_to_delete AS (
            SELECT id, parentMetaId FROM meta_accounts WHERE id = :metaId
            UNION ALL
            SELECT m.id, m.parentMetaId
            FROM meta_accounts m
            JOIN accounts_to_delete r ON m.parentMetaId = r.id
        )
        DELETE FROM meta_accounts WHERE id IN (SELECT id FROM accounts_to_delete)
    """
    )
    suspend fun delete(metaId: Long)

    @Query(
        """
        WITH RECURSIVE accounts_to_delete AS (
            SELECT id, parentMetaId FROM meta_accounts WHERE id IN (:metaIds)
            UNION ALL
            SELECT m.id, m.parentMetaId
            FROM meta_accounts m
            JOIN accounts_to_delete r ON m.parentMetaId = r.id
        )
        DELETE FROM meta_accounts WHERE id IN (SELECT id FROM accounts_to_delete)
    """
    )
    suspend fun delete(metaIds: List<Long>)

    @Query("SELECT COALESCE(MAX(position), 0)  + 1 FROM meta_accounts")
    suspend fun nextAccountPosition(): Int

    @Query("SELECT * FROM meta_accounts WHERE isSelected = 1")
    suspend fun selectedMetaAccount(): RelationJoinedMetaAccountInfo?

    @Transaction
    suspend fun insertMetaAndChainAccounts(
        metaAccount: MetaAccountLocal,
        createChainAccounts: suspend (metaId: Long) -> List<ChainAccountLocal>
    ): Long {
        val metaId = insertMetaAccount(metaAccount)

        insertChainAccounts(createChainAccounts(metaId))

        return metaId
    }

    @Query("SELECT EXISTS(SELECT * FROM meta_accounts WHERE status = :status)")
    suspend fun hasMetaAccountsByStatus(status: MetaAccountLocal.Status): Boolean

    @Query("UPDATE meta_accounts SET status = :status WHERE id IN (:metaIds)")
    suspend fun changeAccountsStatus(metaIds: List<Long>, status: MetaAccountLocal.Status)

    @Query("DELETE FROM meta_accounts WHERE status = :status ")
    fun removeMetaAccountsByStatus(status: MetaAccountLocal.Status)
}

class MetaAccountWithBalanceLocal(
    val id: Long,
    val freeInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,
    val offChainBalance: BigInteger?,
    val precision: Int,
    val rate: BigDecimal?
)
