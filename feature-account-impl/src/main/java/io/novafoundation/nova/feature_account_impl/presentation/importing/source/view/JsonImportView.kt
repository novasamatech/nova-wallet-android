package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.nameInputFilters
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.ImportSource
import io.novafoundation.nova.feature_account_impl.presentation.importing.source.model.JsonImportSource
import kotlinx.android.synthetic.main.import_source_json.view.importJsonContent
import kotlinx.android.synthetic.main.import_source_json.view.importJsonNoNetworkInfo
import kotlinx.android.synthetic.main.import_source_json.view.importJsonPasswordInput
import kotlinx.android.synthetic.main.import_source_json.view.importJsonUsernameInput

class JsonImportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImportSourceView(R.layout.import_source_json, context, attrs, defStyleAttr) {

    override val nameInputViews: ImportAccountNameViews
        get() = ImportAccountNameViews(
            nameInput = importJsonUsernameInput,
            visibilityDependent = emptyList()
        )

    init {
        importJsonUsernameInput.editText!!.filters = nameInputFilters()
    }

    override fun observeSource(source: ImportSource, lifecycleOwner: LifecycleOwner) {
        require(source is JsonImportSource)

        source.jsonContentLiveData.observe(lifecycleOwner, Observer(importJsonContent::setMessage))

        source.showJsonInputOptionsEvent.observe(
            lifecycleOwner,
            EventObserver {
                showJsonInputOptionsSheet(source)
            }
        )

        importJsonPasswordInput.content.bindTo(source.passwordLiveData, lifecycleOwner)

        importJsonContent.setActionClickListener {
            source.chooseFileClicked()
        }

        importJsonContent.setOnClickListener {
            source.jsonClicked()
        }

        source.showNetworkWarningLiveData.observe(lifecycleOwner) {
            importJsonNoNetworkInfo.setVisible(it)
        }
    }

    private fun showJsonInputOptionsSheet(source: JsonImportSource) {
        JsonPasteOptionsSheet(context, source::pasteClicked, source::chooseFileClicked)
            .show()
    }
}
