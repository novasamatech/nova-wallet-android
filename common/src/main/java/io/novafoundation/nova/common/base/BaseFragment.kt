package io.novafoundation.nova.common.base

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

abstract class BaseFragment<T : BaseViewModel> : Fragment(), BaseFragmentMixin<T> {

    @Inject override lateinit var viewModel: T

    override val fragment: Fragment
        get() = this

    private val delegate by lazy(LazyThreadSafetyMode.NONE) { BaseFragmentDelegate(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        delegate.onViewCreated(view, savedInstanceState)
    }

}

