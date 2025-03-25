package io.novafoundation.nova.common.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

abstract class BaseBottomSheetFragment<T : BaseViewModel> : BottomSheetDialogFragment(), BaseFragmentMixin<T> {

    @Inject
    override lateinit var viewModel: T

    override val fragment: Fragment
        get() = this

    private val delegate by lazy(LazyThreadSafetyMode.NONE) { BaseFragmentDelegate(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        delegate.onViewCreated(view, savedInstanceState)
    }

    protected fun getBehaviour(): BottomSheetBehavior<*> {
        return (dialog as BottomSheetDialog).behavior
    }
}
