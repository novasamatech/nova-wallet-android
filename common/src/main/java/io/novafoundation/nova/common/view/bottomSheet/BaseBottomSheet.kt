package io.novafoundation.nova.common.view.bottomSheet

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.viewbinding.ViewBinding
import by.kirich1409.viewbindingdelegate.LifecycleViewBindingProperty
import by.kirich1409.viewbindingdelegate.viewBindingWithLifecycle

abstract class BaseBottomSheet<B : ViewBinding>(
    context: Context,
    style: Int = R.style.BottomSheetDialog,
    private val onCancel: (() -> Unit)? = null,
) :
    BottomSheetDialog(context, style),
    DialogExtensions,
    CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main) {

    protected abstract val binder: B

    private val backgroundAccessObserver: BackgroundAccessObserver

    final override val dialogInterface: DialogInterface
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binder.root)

        window?.decorView
            ?.findViewById<View>(R.id.touch_outside)
            ?.isFocusable = false

        onCancel?.let {
            setOnCancelListener { onCancel.invoke() }
        }
    }

    init {
        backgroundAccessObserver = FeatureUtils.getCommonApi(context)
            .backgroundAccessObserver()

        backgroundAccessObserver.requestAccessFlow
            .onEach { dismiss() }
            .launchIn(this)
    }

    override fun dismiss() {
        coroutineContext.cancel()
        super.dismiss()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        initViewTreeOwners()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        initViewTreeOwners()
    }

    override fun setContentView(
        view: View,
        params: ViewGroup.LayoutParams?,
    ) {
        super.setContentView(view, params)
        initViewTreeOwners()
    }

    private fun initViewTreeOwners() {
        window!!.decorView.setViewTreeLifecycleOwner(this)
    }

    protected inline fun <R : Any, reified VB : ViewBinding> viewBinding(): LifecycleViewBindingProperty<R, VB> {
        return viewBindingWithLifecycle(lifecycle, LayoutInflater.from(context))
    }
}
