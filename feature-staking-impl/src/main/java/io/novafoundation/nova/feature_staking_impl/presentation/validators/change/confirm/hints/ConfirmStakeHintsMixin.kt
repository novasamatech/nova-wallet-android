package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.coroutines.CoroutineScope

class ConfirmStakeHintsMixinFactory(
    private val resourceManager: ResourceManager,
) {

    fun create(coroutineScope: CoroutineScope): HintsMixin = ConfirmStakeHintsMixin(
        resourceManager = resourceManager,
        coroutineScope = coroutineScope,
    )
}

private class ConfirmStakeHintsMixin(
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> {
        return changeValidatorsHints()
    }

    private fun changeValidatorsHints(): List<String> = listOf(
        validatorsChangeHint()
    )

    private fun validatorsChangeHint(): String {
        return resourceManager.getString(R.string.staking_your_validators_changing_title)
    }
}
