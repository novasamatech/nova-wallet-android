package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload

const val CONDITION_ID_1 = 0
const val CONDITION_ID_2 = 1
const val CONDITION_ID_3 = 2

class ManualBackupWarningViewModel(
    private val router: AccountRouter,
    private val conditionMixinFactory: ConditionMixinFactory,
    private val payload: ManualBackupCommonPayload
) : BaseViewModel() {

    val conditionMixin = conditionMixinFactory.createConditionMixin(
        coroutineScope = viewModelScope,
        conditionsCount = 3,
        enabledButtonText = R.string.common_confirm,
        disabledButtonText = R.string.manual_backup_warning_disabled_button
    )

    fun continueClicked() {
        router.openManualBackupSecrets(payload)
    }

    fun backClicked() {
        router.back()
    }

    fun conditionClicked(conditionIndex: Int, checked: Boolean) {
        conditionMixin.checkCondition(conditionIndex, checked)
    }
}
