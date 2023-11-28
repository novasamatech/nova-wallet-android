package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.observe
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.source.JsonImportSource
import kotlinx.android.synthetic.main.import_source_json.view.importJsonContent
import kotlinx.android.synthetic.main.import_source_json.view.importJsonNoNetworkInfo
import kotlinx.android.synthetic.main.import_source_json.view.importJsonPasswordInput
import kotlinx.android.synthetic.main.import_source_json.view.importJsonUsernameInput

class JsonImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView<JsonImportSource>(R.layout.import_source_json, context, attrs, defStyleAttr) {

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = importJsonUsernameInput,
            visibilityDependent = emptyList()
        )

    override fun observeSource(source: JsonImportSource, lifecycleOwner: LifecycleOwner) {
        val scope = lifecycleOwner.lifecycle.coroutineScope

        source.jsonContentFlow.observe(scope, importJsonContent::setMessage)

        importJsonContent.setWholeClickListener { source.jsonClicked() }

        source.showJsonInputOptionsEvent.observe(
            lifecycleOwner,
            EventObserver {
                showJsonInputOptionsSheet(source)
            }
        )

        importJsonPasswordInput.content.bindTo(source.passwordFlow, scope)

        importJsonContent.setOnClickListener {
            source.jsonClicked()
        }

        source.showNetworkWarningFlow.observe(scope) {
            importJsonNoNetworkInfo.setVisible(it)
        }
    }

    private fun showJsonInputOptionsSheet(source: JsonImportSource) {
        JsonPasteOptionsSheet(context, source::pasteClicked, source::chooseFileClicked)
            .show()
    }
}
