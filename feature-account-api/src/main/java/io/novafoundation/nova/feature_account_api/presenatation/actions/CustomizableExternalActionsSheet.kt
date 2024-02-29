package io.novafoundation.nova.feature_account_api.presenatation.actions

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem

class ExternalActionModel(
    @DrawableRes val iconRes: Int,
    val title: String,
    val onClick: () -> Unit
)

class CustomizableExternalActionsSheet(
    context: Context,
    payload: ExternalActions.Payload,
    onCopy: CopyCallback,
    onViewExternal: ExternalViewCallback,
    val additionalOptions: List<ExternalActionModel>
) : ExternalActionsSheet(context, payload, onCopy, onViewExternal) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        additionalOptions.forEach { externalActionModel ->
            textItem(externalActionModel.iconRes, externalActionModel.title, showArrow = true) {
                externalActionModel.onClick()
            }
        }
    }
}
