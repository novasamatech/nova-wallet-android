package io.novafoundation.nova.feature_account_impl.presentation.language

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageModel
import io.novafoundation.nova.feature_account_impl.databinding.ItemLanguageBinding

class LanguagesAdapter(
    private val languagesItemHandler: LanguagesItemHandler
) : ListAdapter<LanguageModel, LanguageViewHolder>(LanguagesDiffCallback) {

    interface LanguagesItemHandler {

        fun checkClicked(languageModel: LanguageModel)
    }

    private var selectedItem: LanguageModel? = null

    fun updateSelectedLanguage(newSelection: LanguageModel) {
        val positionToHide = selectedItem?.let { selected ->
            currentList.indexOfFirst { selected.iso == it.iso }
        }

        val positionToShow = currentList.indexOfFirst {
            newSelection.iso == it.iso
        }

        selectedItem = newSelection

        positionToHide?.let { notifyItemChanged(it) }
        notifyItemChanged(positionToShow)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LanguageViewHolder {
        return LanguageViewHolder(ItemLanguageBinding.inflate(viewGroup.inflater(), viewGroup, false))
    }

    override fun onBindViewHolder(languageViewHolder: LanguageViewHolder, position: Int) {
        val languageModel = getItem(position)
        val isChecked = languageModel.iso == selectedItem?.iso

        languageViewHolder.bind(languageModel, languagesItemHandler, isChecked)
    }
}

class LanguageViewHolder(private val binder: ItemLanguageBinding) : RecyclerView.ViewHolder(binder.root) {

    fun bind(language: LanguageModel, handler: LanguagesAdapter.LanguagesItemHandler, isChecked: Boolean) {
        with(itemView) {
            binder.languageNameTv.text = language.displayName
            binder.languageNativeNameTv.text = language.nativeDisplayName

            binder.languageCheck.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE

            setOnClickListener { handler.checkClicked(language) }
        }
    }
}

object LanguagesDiffCallback : DiffUtil.ItemCallback<LanguageModel>() {
    override fun areItemsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
        return oldItem.iso == newItem.iso
    }

    override fun areContentsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
        return oldItem == newItem
    }
}
