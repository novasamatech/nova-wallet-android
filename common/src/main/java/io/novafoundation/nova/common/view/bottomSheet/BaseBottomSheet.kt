package io.novafoundation.nova.common.view.bottomSheet

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.DialogExtensions

abstract class BaseBottomSheet(context: Context) : BottomSheetDialog(context, R.style.BottomSheetDialog), DialogExtensions {

    final override val dialogInterface: DialogInterface
        get() = this
}
