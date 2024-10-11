package io.novafoundation.nova.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

abstract class BaseBottomSheetFragment<T : BaseViewModel, B : ViewBinding> : BottomSheetDialogFragment(), BaseFragmentMixin<T> {

    @Inject
    override lateinit var viewModel: T

    protected abstract val binder: B

    override val fragment: Fragment
        get() = this

    private val delegate by lazy(LazyThreadSafetyMode.NONE) { BaseFragmentDelegate(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        delegate.onViewCreated(view, savedInstanceState)
    }
}
