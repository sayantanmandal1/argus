package io.argus.android

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.argus.android.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * The single screen: a server-URL field, a query box, and a scrollable list of ranked results.
 * Searches are performed off the main thread with coroutines and rendered as they arrive.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = ResultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.results.layoutManager = LinearLayoutManager(this)
        binding.results.adapter = adapter

        binding.searchButton.setOnClickListener { runSearch() }
        binding.query.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else {
                false
            }
        }
    }

    private fun runSearch() {
        val base = binding.serverUrl.text.toString().trim().ifEmpty { getString(R.string.server_default) }
        val query = binding.query.text.toString().trim()
        binding.status.text = getString(R.string.searching)
        lifecycleScope.launch {
            try {
                val result = ArgusClient.search(base, query)
                adapter.submit(result.hits)
                binding.status.text = getString(R.string.hits_format, result.total, result.hits.size)
            } catch (e: Exception) {
                adapter.submit(emptyList())
                binding.status.text = getString(R.string.error_format, e.message ?: "unknown")
            }
        }
    }
}
