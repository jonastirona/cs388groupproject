package com.example.modmycar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

data class ModHierarchyItem(
    val modId: String,
    val name: String,
    val category: String?,
    val isCompleted: Boolean = false,
    val children: List<ModHierarchyItem> = emptyList()
)

class ModHierarchyAdapter(
    private var mods: List<ModHierarchyItem>,
    private val onModToggled: (modId: String, isCompleted: Boolean) -> Unit,
    private val isReadOnly: Boolean = false
) : RecyclerView.Adapter<ModHierarchyAdapter.ModViewHolder>() {

    private val expandedItems = mutableSetOf<String>()

    class ModViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val modHeaderLayout: LinearLayout = itemView.findViewById(R.id.modHeaderLayout)
        val expandCollapseIcon: ImageView = itemView.findViewById(R.id.expandCollapseIcon)
        val modCheckbox: MaterialCheckBox = itemView.findViewById(R.id.modCheckbox)
        val modNameTextView: TextView = itemView.findViewById(R.id.modNameTextView)
        val modCategoryTextView: TextView = itemView.findViewById(R.id.modCategoryTextView)
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

        // Set mod name
        holder.modNameTextView.text = mod.name
        
        // Set checkbox state
        holder.modCheckbox.isChecked = mod.isCompleted
        
        // Show/hide checkbox based on whether it has children
        if (mod.children.isNotEmpty()) {
            holder.modCheckbox.visibility = View.GONE
        } else {
            holder.modCheckbox.visibility = View.VISIBLE
        }
        
        // Set category if available
        if (!mod.category.isNullOrBlank()) {
            holder.modCategoryTextView.text = mod.category
            holder.modCategoryTextView.visibility = View.VISIBLE
        } else {
            holder.modCategoryTextView.visibility = View.GONE
        }

        // Set expand/collapse icon
        if (mod.children.isNotEmpty()) {
            holder.expandCollapseIcon.visibility = View.VISIBLE
            holder.expandCollapseIcon.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
        } else {
            holder.expandCollapseIcon.visibility = View.GONE
        }

        // Keep text normal for all items (no strikethrough or grey out)
        holder.modNameTextView.alpha = 1.0f
        holder.modNameTextView.paintFlags = 
            holder.modNameTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

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
                val childCheckbox = childView.findViewById<MaterialCheckBox>(R.id.childModCheckbox)

                childNameTextView.text = childMod.name
                childCheckbox.isChecked = childMod.isCompleted
                
                // Keep text normal for all items (no strikethrough or grey out)
                childNameTextView.alpha = 1.0f
                childNameTextView.paintFlags = 
                    childNameTextView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

                // Toggle completion on child click (only if not read-only)
                if (!isReadOnly) {
                    childView.setOnClickListener {
                        onModToggled(childMod.modId, !childMod.isCompleted)
                    }
                } else {
                    childView.isClickable = false
                    childView.alpha = 0.7f // Visual indicator that it's read-only
                    childCheckbox.isEnabled = false
                }

                holder.childrenContainer.addView(childView)
            }
        } else {
            holder.childrenContainer.visibility = View.GONE
        }

        // Expand/collapse on header click
        holder.modHeaderLayout.setOnClickListener {
            if (mod.children.isNotEmpty()) {
                // Always allow expand/collapse
                if (isExpanded) {
                    expandedItems.remove(mod.modId)
                } else {
                    expandedItems.add(mod.modId)
                }
                notifyItemChanged(position)
            } else if (!isReadOnly) {
                // Leaf node without children: toggle completion directly (only if not read-only)
                onModToggled(mod.modId, !mod.isCompleted)
            }
        }
        
        // Visual indicator for read-only mode
        if (isReadOnly && mod.children.isEmpty()) {
            holder.modHeaderLayout.alpha = 0.7f
            holder.modCheckbox.isEnabled = false
        } else {
            holder.modHeaderLayout.alpha = 1.0f
            if (mod.children.isEmpty()) {
                holder.modCheckbox.isEnabled = !isReadOnly
            }
        }
    }

    override fun getItemCount(): Int = mods.size

    fun updateMods(newMods: List<ModHierarchyItem>) {
        mods = newMods
        notifyDataSetChanged()
    }
}

