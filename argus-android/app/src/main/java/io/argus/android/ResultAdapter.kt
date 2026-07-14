package io.argus.android

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.argus.android.databinding.ItemResultBinding

/** Renders the list of search hits into result cards. */
class ResultAdapter : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    private val items = ArrayList<Hit>()

    fun submit(hits: List<Hit>) {
        items.clear()
        items.addAll(hits)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hit: Hit) {
            binding.title.text = hit.title
            binding.meta.text = "#${hit.docId}   score ${"%.4f".format(hit.score)}"
            binding.body.text = hit.body
        }
    }
}
