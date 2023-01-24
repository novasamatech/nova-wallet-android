package io.novafoundation.nova.common.base

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.dialog.dialog
import kotlinx.coroutines.flow.Flow

interface BaseFragmentMixin<T : BaseViewModel> : WithContextExtensions {

    val fragment: Fragment

    val viewModel: T

    override val providedContext: Context
        get() = fragment.requireContext()

    fun initViews()

    fun inject()

    fun subscribe(viewModel: T)

    fun showError(errorMessage: String) {
        dialog(fragment.requireContext()) {
            setTitle(fragment.getString(R.string.common_error_general_title))
            setMessage(errorMessage)
            setPositiveButton(R.string.common_ok) { _, _ -> }
        }
    }

    fun showErrorWithTitle(title: String, errorMessage: String?) {
        dialog(fragment.requireContext()) {
            setTitle(title)
            setMessage(errorMessage)
            setPositiveButton(R.string.common_ok) { _, _ -> }
        }
    }

    fun showMessage(text: String) {
        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_SHORT)
            .show()
    }

    fun onBackPressed(action: () -> Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                action()
            }
        }

        fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment.viewLifecycleOwner, callback)
    }

    fun <V> LiveData<Event<V>>.observeEvent(observer: (V) -> Unit) {
        observe(fragment.viewLifecycleOwner, EventObserver(observer::invoke))
    }

    fun <V> Flow<V>.observe(collector: suspend (V) -> Unit) {
        fragment.lifecycleScope.launchWhenResumed {
            collect(collector)
        }
    }

    fun <V> Flow<V>.observeCancelling(collector: suspend (V) -> Unit) {
        fragment.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            collect(collector)
        }
    }

    fun <V> LiveData<V>.observe(observer: (V) -> Unit) {
        observe(fragment.viewLifecycleOwner, observer)
    }

    fun EditText.bindTo(liveData: MutableLiveData<String>) = bindTo(liveData, fragment.viewLifecycleOwner)

    @Suppress("UNCHECKED_CAST")
    fun <A> argument(key: String): A = fragment.arguments!![key] as A
}

class BaseFragmentDelegate<T : BaseViewModel>(
    private val mixin: BaseFragmentMixin<T>
) {

    fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(mixin) {
        inject()
        initViews()
        subscribe(viewModel)

        viewModel.errorLiveData.observeEvent(::showError)

        viewModel.errorWithTitleLiveData.observeEvent {
            showErrorWithTitle(it.first, it.second)
        }

        viewModel.messageLiveData.observeEvent(::showMessage)
    }
}
