package io.novafoundation.nova.runtime.extrinsic.extensions

import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.FixedValueTransactionExtension
import java.math.BigInteger

// Signed extension for Avail related to Data Availability Transactions.
// We set it to 0 which is the default value provided by Avail team
class CheckAppId(appId: BigInteger = BigInteger.ZERO) : FixedValueTransactionExtension(
    name = "CheckAppId",
    implicit = null,
    explicit = appId
)
