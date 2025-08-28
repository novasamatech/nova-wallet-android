package io.novafoundation.nova.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.utils.applySystemBarInsets
import javax.inject.Inject

abstract class BaseFragment<T : BaseViewModel, B : ViewBinding> : Fragment(), BaseFragmentMixin<T> {

    @Inject
    override lateinit var viewModel: T

    protected lateinit var binder: B
        private set

    override val fragment: Fragment
        get() = this

    private val delegate by lazy(LazyThreadSafetyMode.NONE) { BaseFragmentDelegate(this) }

    protected abstract fun createBinding(): B

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binder = createBinding()
        return binder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets(view)
        delegate.onViewCreated(view, savedInstanceState)
    }

    open fun applyInsets(rootView: View) {
        rootView.applySystemBarInsets()
    }
}
