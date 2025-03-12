package com.media.picker.cropper

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.media.picker.cropper.helper.util.model.AspectRatio
import com.media.picker.databinding.RowRatioBinding

class AspectRatioAdapter(
    val list: ArrayList<AspectRatio>,
    val onRatioSelected: (AspectRatio) -> Unit
) : RecyclerView.Adapter<AspectRatioAdapter.VHolder>() {

    private var selectedPosition: Int = 0

    @SuppressLint("NotifyDataSetChanged")
    fun selectAspectRatio(ratio: AspectRatio) {
        selectedPosition = list.indexOfFirst { ratio.widthRatio == it.widthRatio && ratio.heightRatio == it.heightRatio }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHolder {
        return VHolder(RowRatioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class VHolder(private val binding: RowRatioBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
        fun bind(position: Int) {
            val constraintLayout = binding.cl
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)

            // Apply the new ratio
            constraintSet.setDimensionRatio(binding.frm.id, "${list[position].widthRatio}:${list[position].heightRatio}")

            // Apply the updated constraints
            constraintSet.applyTo(constraintLayout)

            if (selectedPosition == position) {
                binding.frm.alpha = 1.0f
            } else {
                binding.frm.alpha = 0.5f
            }

            binding.txtRatio.text = "${list[position].widthRatio.toInt()}:${list[position].heightRatio.toInt()}"

            binding.root.setOnClickListener {
                selectedPosition = position
                onRatioSelected(list[position])
                notifyDataSetChanged()
            }
        }
    }
}