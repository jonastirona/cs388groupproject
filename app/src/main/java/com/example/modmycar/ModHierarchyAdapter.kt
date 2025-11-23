package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ModHierarchyItem(
    val modId: String,
    val name: String,
    val category: String?,
    val children: List<ModHierarchyItem> = emptyList()
)

class ModHierarchyAdapter(
    private val mods: List<ModHierarchyItem>
) : RecyclerView.Adapter<ModHierarchyAdapter.ModViewHolder>() {

    private val expandedItems = mutableSetOf<String>()

    class ModViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val modHeaderLayout: LinearLayout = itemView.findViewById(R.id.modHeaderLayout)
        val expandCollapseIcon: ImageView = itemView.findViewById(R.id.expandCollapseIcon)
        val modNameTextView: TextView = itemView.findViewById(R.id.modNameTextView)
        val childrenContainer: LinearLayout = itemView.findViewById(R.id.childrenContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mod_hierarchy, parent, false)
        return ModViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModViewHolder, position: Int) {
        val mod = mods[position]
        val isExpanded = expandedItems.contains(mod.modId)

        holder.modNameTextView.text = mod.name

        // Set expand/collapse icon
        holder.expandCollapseIcon.setImageResource(
            if (isExpanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )

        // Show/hide children
        if (mod.children.isNotEmpty()) {
            holder.childrenContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Clear existing children views
            holder.childrenContainer.removeAllViews()
            
            // Add child mods
            mod.children.forEach { childMod ->
                val childView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_child_mod, holder.childrenContainer, false)
                val childNameTextView = childView.findViewById<TextView>(R.id.childModNameTextView)
                childNameTextView.text = childMod.name
                holder.childrenContainer.addView(childView)
            }
        } else {
            holder.childrenContainer.visibility = View.GONE
        }

        holder.modHeaderLayout.setOnClickListener {
            if (mod.children.isNotEmpty()) {
                if (isExpanded) {
                    expandedItems.remove(mod.modId)
                } else {
                    expandedItems.add(mod.modId)
                }
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = mods.size
}

