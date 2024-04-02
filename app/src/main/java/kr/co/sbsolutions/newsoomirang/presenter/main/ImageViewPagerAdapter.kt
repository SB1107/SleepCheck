package kr.co.sbsolutions.newsoomirang.presenter.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kr.co.sbsolutions.newsoomirang.databinding.ImageItemBinding

class ImageViewPagerAdapter(private val imageList: List<Int>) :
    RecyclerView.Adapter<ImageViewPagerAdapter.ViewPagerViewHolder>() {

    inner class ViewPagerViewHolder(val binding: ImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(image: Int) {
            binding.img1.setImageResource(image)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewPagerAdapter.ViewPagerViewHolder {
        val binding = ImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewPagerAdapter.ViewPagerViewHolder, position: Int) {
        holder.setData(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

}