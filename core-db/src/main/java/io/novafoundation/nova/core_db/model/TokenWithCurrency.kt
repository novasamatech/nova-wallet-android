package io.novafoundation.nova.core_db.model

import androidx.room.Embedded

class TokenWithCurrency(
    @Embedded
    val token: TokenLocal?,

    @Embedded
    val currency: CurrencyLocal
)
