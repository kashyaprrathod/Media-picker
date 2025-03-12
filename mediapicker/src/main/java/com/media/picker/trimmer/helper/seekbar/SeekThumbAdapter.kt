package com.media.picker.trimmer.helper.seekbar

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.media.picker.databinding.RowSeekThumbBinding

class SeekThumbAdapter(private val thumbCount: Int, val cellWidth: Int) : RecyclerView.Adapter<SeekThumbAdapter.VHolder>() {

    private val arlThumb = ArrayList<Bitmap?>()

    fun addThumb(bitmap: Bitmap?) {
        val positionStart = arlThumb.size
        arlThumb.add(bitmap)
        Log.e("TAG", "addThumb: ${arlThumb.size}")
        notifyItemInserted(positionStart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHolder {
        return VHolder(RowSeekThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return arlThumb.size
    }

    inner class VHolder(val binding: RowSeekThumbBinding) : ViewHolder(binding.root) {
        fun bind() {
            Log.e("TAG", "bind: ")
            try {
                binding.cv.layoutParams.width = cellWidth
                binding.cv.requestLayout()
                binding.img.setImageBitmap(arlThumb[absoluteAdapterPosition])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}