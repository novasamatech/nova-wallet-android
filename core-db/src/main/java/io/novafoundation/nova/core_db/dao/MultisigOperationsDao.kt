package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.MultisigOperationCallLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MultisigOperationsDao {

    @Query("SELECT * FROM multisig_operation_call")
    abstract fun observeOperations(): Flow<List<MultisigOperationCallLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOperation(operation: MultisigOperationCallLocal)
}
