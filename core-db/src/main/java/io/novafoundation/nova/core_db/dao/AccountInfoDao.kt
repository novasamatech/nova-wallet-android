package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.novafoundation.nova.core_db.model.AccountInfoLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AccountInfoDao {

    @Query("select * from account_infos where chainId = :chainId")
    abstract suspend fun getAccountInfo(chainId: String): AccountInfoLocal

    @Query("select * from account_infos where chainId = :chainId")
    abstract fun observeAccountInfo(chainId: String): Flow<AccountInfoLocal>

    @Update
    abstract suspend fun updateAccountInfo(account: AccountInfoLocal)

    @Insert
    abstract suspend fun insertAccountInfo(account: AccountInfoLocal)
}
