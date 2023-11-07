package io.novafoundation.nova.core_db.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity(tableName = "account_infos")
class AccountInfoLocal(
    @PrimaryKey
    val chainId: String,
    val consumers: BigInteger,
    val providers: BigInteger,
    val sufficients: BigInteger,
    @Embedded(prefix = "data_") val data: AccountInfoDataLocal
) {

    companion object {
        fun empty(chainId: String) = AccountInfoLocal(
            chainId = chainId,
            consumers = BigInteger.ZERO,
            providers = BigInteger.ZERO,
            sufficients = BigInteger.ZERO,
            data = AccountInfoDataLocal(
                free = BigInteger.ZERO,
                reserved = BigInteger.ZERO,
                frozen = BigInteger.ZERO
            )
        )
    }
}

class AccountInfoDataLocal(
    val free: BigInteger,
    val reserved: BigInteger,
    val frozen: BigInteger
)
