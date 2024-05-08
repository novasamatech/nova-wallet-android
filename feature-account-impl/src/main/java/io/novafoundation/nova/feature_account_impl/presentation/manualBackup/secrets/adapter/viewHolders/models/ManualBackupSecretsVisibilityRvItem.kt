package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models

import android.view.View

abstract class ManualBackupSecretsVisibilityRvItem : ManualBackupSecretsRvItem {

    abstract var isShown: Boolean
        protected set

    fun makeShown() = apply { isShown = true }
}
