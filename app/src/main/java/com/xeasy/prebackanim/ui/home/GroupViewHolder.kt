package com.xeasy.prebackanim.ui.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.RecyclerView
import com.xeasy.prebackanim.R

class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleText: TextView = itemView.findViewById(R.id.tv_title)
    private val arrowIcon: ImageView = itemView.findViewById(R.id.iv_arrow)

    /*fun bind(group: ListItem.Group) {
        titleText.text = group.title
        itemView.setOnClickListener {
            (it.findFragment() as? HomeFragment)?.adapter?.toggleGroup(group.title)
            toggleArrow()
        }
    }*/

    fun bind(group: ExpandableAdapter.ListItem.Group) {
        titleText.text = group.title
        itemView.setOnClickListener {
            val parent = itemView.findFragment() as? HomeFragment
            val adapterPosition = bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                parent?.adapter?.toggleGroup(group.title, adapterPosition)
                toggleArrow()
            }
        }
    }

    private fun toggleArrow() {
        arrowIcon.animate()
            .rotationBy(180f)
            .setDuration(200)
            .start()
    }
}

class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val contentText: TextView = itemView.findViewById(R.id.tv_content)

    fun bind(child: ExpandableAdapter.ListItem.Child) {
        contentText.text = child.content
    }
}