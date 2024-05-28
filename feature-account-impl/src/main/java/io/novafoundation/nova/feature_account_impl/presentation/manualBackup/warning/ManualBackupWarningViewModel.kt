package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.mixin.condition.buttonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload

class ManualBackupWarningViewModel(
    private val resourceManager: ResourceManager,
    private val router: AccountRouter,
    private val conditionMixinFactory: ConditionMixinFactory,
    private val payload: ManualBackupCommonPayload
) : BaseViewModel() {

    val conditionMixin = conditionMixinFactory.createConditionMixin(
        coroutineScope = viewModelScope,
        conditionsCount = 3
    )

    val buttonState = conditionMixin.buttonState(
        enabledState = resourceManager.getString(R.string.common_confirm),
        disabledState = resourceManager.getString(R.string.manual_backup_warning_disabled_button)
    ).shareInBackground()

    fun continueClicked() {
        router.openManualBackupSecrets(payload)
    }

    fun backClicked() {
        router.back()
    }
}
