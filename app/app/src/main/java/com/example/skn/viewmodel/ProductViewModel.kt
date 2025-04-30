package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.api.MakeupApiClient
import com.example.skn.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log


class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _recentlySearched = MutableStateFlow<List<Product>>(emptyList())
    val recentlySearched: StateFlow<List<Product>> = _recentlySearched

    private val _favoriteProducts = MutableStateFlow<List<Product>>(emptyList())
    val favoriteProducts: StateFlow<List<Product>> = _favoriteProducts

    enum class TagType { GOOD, BAD }
    private val _skinTags = MutableStateFlow<Map<Int, TagType>>(emptyMap())
    val skinTags: StateFlow<Map<Int, TagType>> = _skinTags

    init {
        loadRecentSearchesFromFirebase()
        loadFavoritesFromFirestore()
        loadSkinTagsFromFirestore()
    }

    fun toggleFavorite(product: Product) {
        val updatedList = _favoriteProducts.value.toMutableList()
        if (updatedList.contains(product)) {
            updatedList.remove(product)
            _favoriteProducts.value = updatedList
            removeFavoriteFromFirestore(product.id)
        } else {
            updatedList.add(product)
            _favoriteProducts.value = updatedList
            addFavoriteToFirestore(product)
        }
    }

    private fun addFavoriteToFirestore(product: Product) {
        val db = Firebase.firestore
        val userId = getCurrentUserId() ?: return

        val data = hashMapOf(
            "productId" to product.id,
            "name" to product.name,
            "brand" to product.brand,
            "productType" to product.product_type,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("user_activity")
            .document(userId)
            .collection("favorites")
            .document(product.id.toString()) // use product ID as the document ID
            .set(data)
            .addOnSuccessListener { Log.d("Favorites", "Saved ${product.name} to favorites") }
            .addOnFailureListener { Log.e("Favorites", "Failed to save favorite", it) }
    }

    private fun loadFavoritesFromFirestore() {
        val db = Firebase.firestore
        val userId = getCurrentUserId() ?: return

        db.collection("user_activity")
            .document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                val favoriteList = result.map { doc ->
                    Product(
                        id = doc.getLong("productId")?.toInt() ?: -1,
                        name = doc.getString("name"),
                        brand = doc.getString("brand"),
                        product_type = doc.getString("productType"),
                        rating = null,
                        image_link = null,
                        api_featured_image = null,
                        category = null,
                        created_at = null,
                        currency = null,
                        description = null,
                        price = null,
                        price_sign = null,
                        product_api_url = null,
                        product_link = null,
                        tag_list = emptyList(),
                        updated_at = null,
                        website_link = null,
                    )
                }
                _favoriteProducts.value = favoriteList
                Log.d("Favorites", "Loaded ${favoriteList.size} favorites from Firestore")
            }
            .addOnFailureListener {
                Log.e("Favorites", "Failed to load favorites", it)
            }
    }

    private fun removeFavoriteFromFirestore(productId: Int) {
        val db = Firebase.firestore
        val userId = getCurrentUserId() ?: return

        db.collection("user_activity")
            .document(userId)
            .collection("favorites")
            .document(productId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("Favorites", "🗑Removed productId=$productId from favorites")
            }
            .addOnFailureListener {
                Log.e("Favorites", "Failed to remove favorite", it)
            }
    }


    fun fetchAllProducts() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val db = Firebase.firestore
                val response = MakeupApiClient.api.getAllProducts()
                if (response.isSuccessful) {
                    val productList = response.body() ?: emptyList()
                    _products.value = productList
                    _error.value = null

                    // ✅ Save to Firestore under "all_products"
                    val batch = db.batch()
                    val collectionRef = db.collection("all_products")
                    productList.forEach { product ->
                        val docRef = collectionRef.document(product.id.toString())
                        batch.set(docRef, product)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("Firestore", "✅ All products saved to Firestore")
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "❌ Failed to save products to Firestore", it)
                        }

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
                val response = MakeupApiClient.api.searchProducts(
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
                    val result = response.body() ?: emptyList()
                    _products.value = result
                    _recentlySearched.value = (_recentlySearched.value + result).distinctBy { it.id }.takeLast(10)

                    // ✅ Log the first matched product's brand, type, and id
                    result.firstOrNull()?.let { product ->
                        logSearchQueryToFirebase(product)
                    }

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

    private fun getCurrentUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun logSearchQueryToFirebase(product: Product) {
        val db = Firebase.firestore
        val userId = getCurrentUserId() ?: return

        val data = hashMapOf(
            "productId" to product.id,
            "name" to product.name,
            "brand" to product.brand,
            "productType" to product.product_type,
            "rating" to product.rating,
            "imageLink" to product.image_link,
            "timestamp" to System.currentTimeMillis(),
            "type" to "search"
        )

        db.collection("user_activity")
            .document(userId)
            .collection("search_history")
            .add(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Logged search for product: ${product.name}")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to log product search", it)
            }
    }


    private fun loadRecentSearchesFromFirebase() {
        val db = Firebase.firestore
        val userId = getCurrentUserId() ?: return

        db.collection("user_activity")
            .document(userId)
            .collection("test_logs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val recentProducts = result.map {
                    Product(
                        id = it.getLong("productId")?.toInt() ?: -1,
                        name = it.getString("name"),
                        brand = it.getString("brand"),
                        product_type = it.getString("productType"),
                        rating = it.getDouble("rating")?.toFloat(),
                        image_link = it.getString("imageLink"),

                        // The rest can stay null if not stored
                        api_featured_image = null,
                        category = null,
                        created_at = null,
                        currency = null,
                        description = null,
                        price = null,
                        price_sign = null,
                        product_api_url = null,
                        product_link = null,
                        tag_list = emptyList(),
                        updated_at = null,
                        website_link = null,
                    )
                }
                _recentlySearched.value = recentProducts
                Log.d("Firestore", "Loaded ${recentProducts.size} recent searches from Firestore")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to load recent searches", it)
            }
    }

    fun loadProductsFromFirestore() {
        _loading.value = true
        val db = Firebase.firestore
        db.collection("all_products")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.map { doc ->
                    doc.toObject(Product::class.java)
                }
                _products.value = productList
                Log.d("Firestore", "✅ Loaded ${productList.size} products from Firestore")
            }
            .addOnFailureListener {
                _error.value = "Failed to load products from Firestore"
                Log.e("Firestore", "❌ Error loading products", it)
            }
            .addOnCompleteListener {
                _loading.value = false
            }
    }
    private fun loadSkinTagsFromFirestore() {
        val uid = getCurrentUserId() ?: return
        Firebase.firestore
            .collection("user_activity")
            .document(uid)
            .collection("skinTags")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _skinTags.value = snap.documents.mapNotNull { doc ->
                    val typeStr = doc.getString("tagType") ?: return@mapNotNull null
                    val pid = doc.id.toIntOrNull() ?: return@mapNotNull null
                    pid to TagType.valueOf(typeStr)
                }.toMap()
            }
    }
    fun toggleSkinTag(product: Product, tag: TagType) {
        val pid = product.id
        val current = _skinTags.value[pid]
        if (current == tag) removeSkinTag(pid)
        else setSkinTag(pid, tag)
    }

    private fun setSkinTag(productId: Int, tag: TagType) {
        val uid = getCurrentUserId() ?: return
        Firebase.firestore
            .collection("user_activity")
            .document(uid)
            .collection("skinTags")
            .document(productId.toString())
            .set(mapOf(
                "tagType" to tag.name,
                "timestamp" to System.currentTimeMillis()
            ))
    }

    private fun removeSkinTag(productId: Int) {
        val uid = getCurrentUserId() ?: return
        Firebase.firestore
            .collection("user_activity")
            .document(uid)
            .collection("skinTags")
            .document(productId.toString())
            .delete()
    }

}

