package com.example.skn.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.api.ChemicalsApiClient
import com.example.skn.model.ChemicalRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChemicalsViewModel : ViewModel() {

    private val _chemicals = MutableStateFlow<List<ChemicalRecord>>(emptyList())
    val chemicals: StateFlow<List<ChemicalRecord>> = _chemicals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val resourceId = "57da6c9a-41a7-44b0-ab8d-815ff2cd5913" // dataset ID

    private val _allChemicals = mutableListOf<ChemicalRecord>()

    // Variables for pagination
    private var currentOffset = 0
    private var totalRecords = 0
    private var hasMoreData = true
    private val pageSize = 10 // Small batch size to avoid transaction issues

    init {
        // Initialize with empty state - load data on demand
    }

    fun loadInitialData() {
        if (_allChemicals.isEmpty()) {
            // Only load if we haven't loaded anything yet
            loadNextPage()
        }
    }

    fun searchChemicals(query: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Reset pagination state
                currentOffset = 0
                _allChemicals.clear()
                hasMoreData = true

                loadChemicalsPage(query)
            } catch (e: Exception) {
                _errorMessage.value = "Exception: ${e.localizedMessage}"
                Log.e("API Exception", "Error in search", e)
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (!hasMoreData || _isLoading.value) return

        viewModelScope.launch {
            loadChemicalsPage()
        }
    }

    private suspend fun loadChemicalsPage(query: String = "") {
        if (!hasMoreData) return

        _isLoading.value = true
        try {
            val response = ChemicalsApiClient.api.searchChemicals(
                resourceId = resourceId,
                query = if (query.isBlank()) null else query,
                limit = pageSize,
                offset = currentOffset
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    val newRecords = body.result.records
                    Log.d("API Pagination", "Fetched ${newRecords.size} records, offset: $currentOffset")

                    totalRecords = body.result.total

                    // Add new records to our collection
                    _allChemicals.addAll(newRecords)

                    // Update offset for next page
                    currentOffset += newRecords.size

                    // Check if we've reached the end
                    hasMoreData = _allChemicals.size < totalRecords && newRecords.isNotEmpty()

                    // Update UI with current results
                    _chemicals.value = _allChemicals.toList() // Create new list to trigger recomposition
                } else {
                    _errorMessage.value = "API Error: ${body?.help ?: "Unknown error"}"
                    Log.e("API Error", "Error in response body: ${body?.help}")
                }
            } else {
                _errorMessage.value = "HTTP Error: ${response.code()} - ${response.message()}"
                Log.e("API Error", "Error response: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            _errorMessage.value = "Exception: ${e.localizedMessage}"
            Log.e("API Exception", "Error fetching page", e)
        } finally {
            _isLoading.value = false
        }
    }

    fun filterChemicals(query: String) {
        if (query.isBlank()) {
            _chemicals.value = _allChemicals
            return
        }

        val keywords = query.lowercase().split(" ").filter { it.isNotBlank() }

        val filtered = _allChemicals.filter { chemical ->
            keywords.all { keyword ->
                listOfNotNull(
                    chemical.productName?.lowercase(),
                    chemical.brandName?.lowercase(),
                    chemical.primaryCategory?.lowercase(),
                    chemical.subCategory?.lowercase(),
                    chemical.chemicalName?.lowercase()
                ).any { field ->
                    field.contains(keyword)
                }
            }
        }

        _chemicals.value = filtered
    }
}