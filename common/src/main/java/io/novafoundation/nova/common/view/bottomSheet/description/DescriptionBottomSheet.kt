package io.novafoundation.nova.common.view.bottomSheet.description

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheetDescriptionBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet

class DescriptionBottomSheet(
    context: Context,
    val titleRes: Int,
    val descriptionText: String
) : BaseBottomSheet<BottomSheetDescriptionBinding>(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    constructor(context: Context, titleRes: Int, descriptionRes: Int) :
        this(context, titleRes, context.getString(descriptionRes))

    override val binder: BottomSheetDescriptionBinding = BottomSheetDescriptionBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder.sheetDescriptionTitle.setText(titleRes)
        binder.sheetDescriptionDetails.text = descriptionText
    }
}
