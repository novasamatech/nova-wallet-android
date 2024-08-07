package io.novafoundation.nova.feature_account_impl.data.fee.types

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

internal class NativeFeePayment : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        // no modifications needed
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        return nativeFee
    }
}
