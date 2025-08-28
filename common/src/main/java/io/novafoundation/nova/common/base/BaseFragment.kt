package io.novafoundation.nova.common.base

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
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

        applyInsetsToChildrenLegacy()
        applyInsets(view)

        delegate.onViewCreated(view, savedInstanceState)
    }

    open fun applyInsets(rootView: View) {
        rootView.applySystemBarInsets()
    }

    /**
     * Fix insets for android 7-10.
     * For some reason Fragments doesn't send insets to their children after root view so we push them forcibly
     * TODO: I haven't found the reason of this issue so I think this fix is temporary until we found the reason
     */
    private fun applyInsetsToChildrenLegacy() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binder.root) { view, insets ->
                val viewGroup = (view as? ViewGroup) ?: return@setOnApplyWindowInsetsListener insets
                viewGroup.children.forEach {
                    ViewCompat.dispatchApplyWindowInsets(it, insets)
                }
                insets
            }
        }
    }
}
