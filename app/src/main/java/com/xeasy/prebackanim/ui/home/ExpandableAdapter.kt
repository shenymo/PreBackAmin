package com.xeasy.prebackanim.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xeasy.prebackanim.R
class ExpandableAdapter(
    private val originalData: Map<String, List<String>>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

//    private val expandedGroups = mutableSetOf<String>()
    private val expandedGroups = mutableMapOf<String, Int>()
    private val items = mutableListOf<ListItem>()

    sealed class ListItem {
        data class Group(val title: String) : ListItem()
        data class Child(val content: String, val parentTitle: String) : ListItem()
    }

    init {
        originalData.keys.forEach { group ->
            items.add(ListItem.Group(group))
        }
    }

    // 添加稳定ID配置
    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is ListItem.Group -> "group_${item.title}".hashCode().toLong()
            is ListItem.Child -> "child_${item.parentTitle}_${item.content}".hashCode().toLong()
        }
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.Group -> VIEW_TYPE_GROUP
        is ListItem.Child -> VIEW_TYPE_CHILD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_GROUP -> GroupViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        )
        else -> ChildViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_child, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Group -> (holder as GroupViewHolder).bind(item)
            is ListItem.Child -> (holder as ChildViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size

    fun toggleGroup(groupTitle: String, position: Int) {
        var realPosition = position
        if (expandedGroups.contains(groupTitle)) {
            collapseGroup(groupTitle, position)
        } else {
            expandedGroups.forEach { (group, positionValue) ->
                collapseGroup(group, positionValue)
                if ( positionValue < position ) {
                    val children = originalData[group]?.map { ListItem.Child(it, group) } ?: return
                    realPosition = position - children.size
                }
            }
            expandGroup(groupTitle, realPosition)
        }

    }

    private fun expandGroup(groupTitle: String, position: Int) {
        val children = originalData[groupTitle]?.map { ListItem.Child(it, groupTitle) } ?: return
        expandedGroups[groupTitle] = position
        items.addAll(position + 1, children)
        notifyItemRangeInserted(position + 1, children.size)
    }

    private fun collapseGroup(groupTitle: String, position: Int) {
        val childItems = mutableListOf<ListItem.Child>()
        var count = 0

        // 精确查找需要删除的子项
        for (i in (position + 1) until items.size) {
            when (val item = items[i]) {
                is ListItem.Child -> if (item.parentTitle == groupTitle) {
                    childItems.add(item)
                    count++
                } else break // 遇到其他组的子项立即停止
                is ListItem.Group -> break // 遇到下一个主项立即停止
            }
        }

        expandedGroups.remove(groupTitle)
        items.removeAll(childItems)
        notifyItemRangeRemoved(position + 1, count)
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_CHILD = 1
    }
}