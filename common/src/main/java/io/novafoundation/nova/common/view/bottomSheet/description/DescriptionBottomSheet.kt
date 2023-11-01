package io.novafoundation.nova.common.view.bottomSheet.description

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_description.sheetDescriptionDetails
import kotlinx.android.synthetic.main.bottom_sheet_description.sheetDescriptionTitle

class DescriptionBottomSheet(
    context: Context,
    val titleRes: Int,
    val descriptionRes: Int
) : BaseBottomSheet(context, R.style.BottomSheetDialog), WithContextExtensions by WithContextExtensions(context) {

    init {
        setContentView(R.layout.bottom_sheet_description)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sheetDescriptionTitle.setText(titleRes)
        sheetDescriptionDetails.setText(descriptionRes)
    }
}
