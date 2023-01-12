package io.novafoundation.nova.common.view.bottomSheet

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.DialogExtensions
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

abstract class BaseBottomSheet(context: Context, style: Int = R.style.BottomSheetDialog) :
    BottomSheetDialog(context, style),
    DialogExtensions,
    CoroutineScope {

    private val backgroundAccessObserver: BackgroundAccessObserver

    private var job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    final override val dialogInterface: DialogInterface
        get() = this

    init {
        backgroundAccessObserver = FeatureUtils.getCommonApi(context)
            .backgroundAccessObserver()

        backgroundAccessObserver.requestAccessFlow
            .onEach { dismiss() }
            .launchIn(this)
    }

    override fun dismiss() {
        job.cancel()
        super.dismiss()
    }
}
