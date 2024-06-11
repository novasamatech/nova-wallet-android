package io.novafoundation.nova.feature_account_api.presenatation.sign

import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant

interface LedgerSignCommunicator : SignInterScreenCommunicator {

    fun setUsedVariant(variant: LedgerVariant)
}
