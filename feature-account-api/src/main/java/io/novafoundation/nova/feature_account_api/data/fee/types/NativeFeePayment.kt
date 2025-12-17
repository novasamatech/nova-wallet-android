package io.novafoundation.nova.feature_account_api.data.fee.types

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

class NativeFeePayment : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        // no modifications needed
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        return nativeFee
    }
}
