package io.novafoundation.nova.common.base

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.showToast
import kotlinx.coroutines.flow.Flow

interface BaseFragmentMixin<T : BaseViewModel> : BaseScreenMixin<T> {

    val fragment: Fragment

    override val providedContext: Context
        get() = fragment.requireContext()

    override val lifecycleOwner: LifecycleOwner
        get() = fragment.viewLifecycleOwner

    fun onBackPressed(action: () -> Unit) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                action()
            }
        }

        fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment.viewLifecycleOwner, callback)
    }

    fun <V> Flow<V>.observe(collector: suspend (V) -> Unit) {
        fragment.lifecycleScope.launchWhenResumed {
            collect(collector)
        }
    }

    fun <V> Flow<V>.observeWhenCreated(collector: suspend (V) -> Unit) {
        fragment.lifecycleScope.launchWhenCreated {
            collect(collector)
        }
    }

    fun <V> Flow<V>.observeWhenVisible(collector: suspend (V) -> Unit) {
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

    @Suppress("UNCHECKED_CAST")
    fun <A> argumentOrNull(key: String): A? = fragment.arguments?.get(key) as? A
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

        viewModel.toastLiveData.observeEvent { view.context.showToast(it) }
    }
}
