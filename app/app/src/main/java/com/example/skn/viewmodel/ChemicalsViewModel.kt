package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.api.ChemicalsApiClient
import com.example.skn.model.ChemicalRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChemicalsViewModel : ViewModel() {

    // State
    private val _chemicals = MutableStateFlow<List<ChemicalRecord>>(emptyList())
    val chemicals: StateFlow<List<ChemicalRecord>> = _chemicals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Internal data
    private val _allChemicals = mutableListOf<ChemicalRecord>()


    // Pagination
    private val resourceId = "57da6c9a-41a7-44b0-ab8d-815ff2cd5913"
    private val pageSize = 10
    private var currentOffset = 0
    private var totalRecords = 0
    private var hasMoreData = true

//    fun loadInitialData() {
//        if (_allChemicals.isEmpty()) {
//            loadNextPage()
//        }
//    }

    fun searchChemicals(query: String = "") {
        viewModelScope.launch {
            resetPagination()
            loadChemicalsPage(query)
        }
    }

//    fun loadNextPage() {
//        if (!hasMoreData || _isLoading.value) return
//        viewModelScope.launch { loadChemicalsPage() }
//    }

    private suspend fun loadChemicalsPage(query: String = "") {
        if (!hasMoreData) return

        _isLoading.value = true
        _errorMessage.value = null

        try {
            val response = ChemicalsApiClient.api.searchChemicals(
                resourceId = resourceId,
                query = query.takeIf { it.isNotBlank() },
                limit = pageSize,
                offset = currentOffset
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val newRecords = body.result.records
                    totalRecords = body.result.total

                    _allChemicals.addAll(newRecords)
                    currentOffset += newRecords.size
                    hasMoreData = _allChemicals.size < totalRecords && newRecords.isNotEmpty()
                    _chemicals.value = _allChemicals.toList()

                } else {
                    val message = body?.help ?: "Unknown API error"
                    _errorMessage.value = "API Error: $message"
                }
            } else {
                _errorMessage.value = "HTTP Error: ${response.code()} - ${response.message()}"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Exception: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
        }
    }

    private fun resetPagination() {
        _allChemicals.clear()
        currentOffset = 0
        totalRecords = 0
        hasMoreData = true
        _chemicals.value = emptyList()
        _errorMessage.value = null
    }
}
