package io.novafoundation.nova.common.base

import android.widget.Toast
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.WithLifecycleExtensions
import io.novafoundation.nova.common.view.dialog.dialog

interface BaseScreenMixin<T : BaseViewModel> : WithContextExtensions, WithLifecycleExtensions {

    val viewModel: T

    fun initViews()

    fun inject()

    fun subscribe(viewModel: T)

    fun showError(errorMessage: String) {
        dialog(providedContext) {
            setTitle(providedContext.getString(R.string.common_error_general_title))
            setMessage(errorMessage)
            setPositiveButton(R.string.common_ok) { _, _ -> }
        }
    }

    fun showErrorWithTitle(title: String, errorMessage: CharSequence?) {
        dialog(providedContext) {
            setTitle(title)
            setMessage(errorMessage)
            setPositiveButton(R.string.common_ok) { _, _ -> }
        }
    }

    fun showMessage(text: String) {
        Toast.makeText(providedContext, text, Toast.LENGTH_SHORT)
            .show()
    }
}
