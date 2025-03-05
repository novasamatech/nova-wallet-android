package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

interface MaxAvailableDeduction {

    fun maxAmountDeductionFor(amountAsset: Chain.Asset): BigInteger
}
