package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.doIfPositionValid
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_staking_impl.databinding.ItemEditableStakingTypeBinding

class SetupStakingTypeAdapter(
    private val handler: ItemAssetHandler
) : ListAdapter<EditableStakingTypeRVItem, EditableStakingTypeViewHolder>(SetupStakingTypeDiffUtil()) {

    interface ItemAssetHandler {

        fun stakingTypeClicked(stakingTypeRVItem: EditableStakingTypeRVItem, position: Int)

        fun stakingTargetClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditableStakingTypeViewHolder {
        return EditableStakingTypeViewHolder(handler, ItemEditableStakingTypeBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: EditableStakingTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: EditableStakingTypeViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        resolvePayload(holder, position, payloads) {
            when (it) {
                EditableStakingTypeRVItem::title -> holder.setTitle(item)
                EditableStakingTypeRVItem::conditions -> holder.setConditions(item)
                EditableStakingTypeRVItem::isSelected -> holder.select(item)
                EditableStakingTypeRVItem::isSelectable -> holder.setSelectable(item)
                EditableStakingTypeRVItem::stakingTarget -> holder.setStakingTarget(item)
                EditableStakingTypeRVItem::imageRes -> holder.setImage(item)
            }
        }
    }
}

private class SetupStakingTypeDiffUtil : DiffUtil.ItemCallback<EditableStakingTypeRVItem>() {

    override fun areItemsTheSame(oldItem: EditableStakingTypeRVItem, newItem: EditableStakingTypeRVItem): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: EditableStakingTypeRVItem, newItem: EditableStakingTypeRVItem): Boolean {
        return true
    }

    override fun getChangePayload(oldItem: EditableStakingTypeRVItem, newItem: EditableStakingTypeRVItem): Any? {
        return SetupStakingTypePayloadGenerator.diff(oldItem, newItem)
    }
}

class EditableStakingTypeViewHolder(
    private val clickHandler: SetupStakingTypeAdapter.ItemAssetHandler,
    private val binder: ItemEditableStakingTypeBinding
) : RecyclerView.ViewHolder(binder.root) {

    fun bind(item: EditableStakingTypeRVItem) = with(binder) {
        setTitle(item)
        setConditions(item)
        select(item)
        setSelectable(item)
        setStakingTarget(item)
        setImage(item)

        editableStakingType.setOnClickListener {
            doIfPositionValid { position -> clickHandler.stakingTypeClicked(item, position) }
        }

        editableStakingType.setStakingTargetClickListener {
            doIfPositionValid { position -> clickHandler.stakingTargetClicked(position) }
        }
    }

    fun setTitle(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.setTitle(item.title)
    }

    fun setConditions(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.setConditions(item.conditions)
    }

    fun select(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.select(item.isSelected)
    }

    fun setSelectable(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.setSelectable(item.isSelectable)
    }

    fun setStakingTarget(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.setStakingTarget(item.stakingTarget)
    }

    fun setImage(item: EditableStakingTypeRVItem) {
        binder.editableStakingType.setBackgroundRes(item.imageRes)
    }
}

private object SetupStakingTypePayloadGenerator : PayloadGenerator<EditableStakingTypeRVItem>(
    EditableStakingTypeRVItem::title,
    EditableStakingTypeRVItem::conditions,
    EditableStakingTypeRVItem::isSelected,
    EditableStakingTypeRVItem::isSelectable,
    EditableStakingTypeRVItem::stakingTarget,
    EditableStakingTypeRVItem::imageRes
)
