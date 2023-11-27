package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountPositionUpdate
import io.novafoundation.nova.core_db.model.chain.account.RelationJoinedMetaAccountInfo
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Fetch meta account where
 * accountId = meta.substrateAccountId
 * or hex(accountId) = meta.ethereumAddress
 * or there is a child chain account which have child.accountId = accountId
 */
@Language("RoomSql")
private const val FIND_BY_ADDRESS_WHERE_CLAUSE = """
        WHERE substrateAccountId = :accountId
        OR ethereumAddress = :accountId
        OR  id = (
            SELECT id FROM meta_accounts AS m
                INNER JOIN chain_accounts as c ON m.id = c.metaId
                WHERE  c.accountId = :accountId
            )
        ORDER BY (CASE WHEN isSelected THEN 0 ELSE 1 END)
    """

@Language("RoomSql")
private const val FIND_ACCOUNT_BY_ADDRESS_QUERY = """
            SELECT * FROM meta_accounts 
            $FIND_BY_ADDRESS_WHERE_CLAUSE
"""

@Language("RoomSql")
private const val FIND_NAME_BY_ADDRESS_QUERY = """
            SELECT name FROM meta_accounts 
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

    @Insert
    suspend fun insertMetaAccount(metaAccount: MetaAccountLocal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainAccount(chainAccount: ChainAccountLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChainAccounts(chainAccounts: List<ChainAccountLocal>)

    @Query("SELECT * FROM meta_accounts")
    fun getMetaAccounts(): List<MetaAccountLocal>

    @Query("SELECT * FROM meta_accounts")
    @Transaction
    suspend fun getJoinedMetaAccountsInfo(): List<RelationJoinedMetaAccountInfo>

    @Query("SELECT * FROM meta_accounts")
    suspend fun getMetaAccountsInfo(): List<MetaAccountLocal>

    @Query("SELECT * FROM meta_accounts")
    fun getJoinedMetaAccountsInfoFlow(): Flow<List<RelationJoinedMetaAccountInfo>>

    @Query(META_ACCOUNTS_WITH_BALANCE_QUERY)
    fun metaAccountsWithBalanceFlow(): Flow<List<MetaAccountWithBalanceLocal>>

    @Query(META_ACCOUNT_WITH_BALANCE_QUERY)
    fun metaAccountWithBalanceFlow(metaId: Long): Flow<List<MetaAccountWithBalanceLocal>>

    @Query("UPDATE meta_accounts SET isSelected = (id = :metaId)")
    suspend fun selectMetaAccount(metaId: Long)

    @Update(entity = MetaAccountLocal::class)
    suspend fun updatePositions(updates: List<MetaAccountPositionUpdate>)

    @Query("SELECT * FROM meta_accounts WHERE id = :metaId")
    @Transaction
    suspend fun getJoinedMetaAccountInfo(metaId: Long): RelationJoinedMetaAccountInfo

    @Query("SELECT * FROM meta_accounts WHERE isSelected = 1")
    @Transaction
    fun selectedMetaAccountInfoFlow(): Flow<RelationJoinedMetaAccountInfo?>

    @Query("SELECT * FROM meta_accounts WHERE id = :metaId")
    @Transaction
    fun metaAccountInfoFlow(metaId: Long): Flow<RelationJoinedMetaAccountInfo?>

    @Query("SELECT EXISTS ($FIND_ACCOUNT_BY_ADDRESS_QUERY)")
    fun isMetaAccountExists(accountId: AccountId): Boolean

    @Query(FIND_ACCOUNT_BY_ADDRESS_QUERY)
    @Transaction
    fun getMetaAccountInfo(accountId: AccountId): RelationJoinedMetaAccountInfo?

    @Query(FIND_NAME_BY_ADDRESS_QUERY)
    fun metaAccountNameFor(accountId: AccountId): String?

    @Query("UPDATE meta_accounts SET name = :newName WHERE id = :metaId")
    suspend fun updateName(metaId: Long, newName: String)

    @Query("DELETE FROM meta_accounts WHERE id = :metaId")
    suspend fun delete(metaId: Long)

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
}

class MetaAccountWithBalanceLocal(
    val id: Long,
    val freeInPlanks: BigInteger,
    val reservedInPlanks: BigInteger,
    val offChainBalance: BigInteger?,
    val precision: Int,
    val rate: BigDecimal?
)
