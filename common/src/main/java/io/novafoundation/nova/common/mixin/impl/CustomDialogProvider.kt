package io.novafoundation.nova.common.mixin.impl

import android.view.ContextThemeWrapper
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.dialog.dialog

class CustomDialogProvider : CustomDialogDisplayer.Presentation {

    override val showCustomDialog = MutableLiveData<Event<CustomDialogDisplayer.Payload>>()

    override fun displayDialog(payload: CustomDialogDisplayer.Payload) {
        showCustomDialog.postValue(Event(payload))
    }
}

fun <V> BaseFragment<V>.setupCustomDialogDisplayer(
    viewModel: V,
) where V : BaseViewModel, V : CustomDialogDisplayer {
    viewModel.showCustomDialog.observeEvent {
        displayDialogFor(it)
    }
}

fun BaseFragment<*>.displayDialogFor(payload: CustomDialogDisplayer.Payload) {
    val baseContext = requireContext()
    val themedContext = payload.customStyle?.let { ContextThemeWrapper(requireContext(), it) } ?: baseContext

    dialog(themedContext) {
        setTitle(payload.title)
        setMessage(payload.message)

        setPositiveButton(payload.okAction.title) { _, _ ->
            payload.okAction.action()
        }

        payload.cancelAction?.let { negativeAction ->
            setNegativeButton(negativeAction.title) { _, _ ->
                negativeAction.action()
            }
        }
    }
}
