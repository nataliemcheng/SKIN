package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.api.ApiClient
import com.example.skn.api.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchAllProducts() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = ApiClient.api.getAllProducts()
                if (response.isSuccessful) {
                    _products.value = response.body() ?: emptyList()
                    _error.value = null
                } else {
                    _error.value = "Error: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Exception: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun searchProducts(
        productType: String? = null,
        productCategory: String? = null,
        productTags: String? = null,
        brand: String? = null,
        priceGreaterThan: Float? = null,
        priceLessThan: Float? = null,
        ratingGreaterThan: Float? = null,
        ratingLessThan: Float? = null
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = ApiClient.api.searchProducts(
                    productType = productType,
                    productCategory = productCategory,
                    productTags = productTags,
                    brand = brand,
                    priceGreaterThan = priceGreaterThan,
                    priceLessThan = priceLessThan,
                    ratingGreaterThan = ratingGreaterThan,
                    ratingLessThan = ratingLessThan
                )
                if (response.isSuccessful) {
                    _products.value = response.body() ?: emptyList()
                    _error.value = null
                } else {
                    _error.value = "Error: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Exception: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }
}