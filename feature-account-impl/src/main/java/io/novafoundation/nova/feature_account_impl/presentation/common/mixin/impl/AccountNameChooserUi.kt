package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import android.view.View
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.utils.observeInLifecycle
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithAccountNameChooserMixin

fun setupAccountNameChooserUi(
    viewModel: WithAccountNameChooserMixin,
    ui: EditText,
    owner: LifecycleOwner,
    additionalViewsToControlVisibility: List<View> = emptyList(),
) {
    ui.onTextChanged {
        viewModel.accountNameChooser.nameChanged(it)
    }

    viewModel.accountNameChooser.nameState.observeInLifecycle(owner.lifecycleScope) { state ->
        val isVisible = state is AccountNameChooserMixin.State.Input

        ui.setVisible(isVisible)
        additionalViewsToControlVisibility.forEach { it.setVisible(isVisible) }

        if (state is AccountNameChooserMixin.State.Input) {
            if (state.value != ui.text.toString()) {
                ui.setText(state.value)
            }
        }
    }
}
