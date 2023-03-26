package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import kotlinx.coroutines.flow.MutableStateFlow

interface InputFlowProvider {

    val inputFlow: MutableStateFlow<String>
}

class RealInputFlowProvider : InputFlowProvider {
    override val inputFlow = MutableStateFlow<String>("")
}
