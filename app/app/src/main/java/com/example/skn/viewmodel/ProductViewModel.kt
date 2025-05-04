package com.example.skn.viewmodel

import android.net.Uri
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
import com.example.skn.api.UPCItemApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await


class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct


    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _recentlySearched = MutableStateFlow<List<Product>>(emptyList())
    val recentlySearched: StateFlow<List<Product>> = _recentlySearched

    private val _favoriteProducts = MutableStateFlow<List<Product>>(emptyList())
    val favoriteProducts: StateFlow<List<Product>> = _favoriteProducts

    enum class TagType { GOOD, BAD, NONE }
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
            "imageLink" to product.image_link,
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
                        image_link = doc.getString("imageLink"),
                        rating = null,
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
                            Log.d("Firestore", "All products saved to Firestore")
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "Failed to save products to Firestore", it)
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

    fun searchProducts(userSearch: String) {
        _loading.value = true

        val query = userSearch.trim().lowercase()

        viewModelScope.launch {
            try {
                // search the query with brand, productType, productTags, and productCategory (has to be separate searches because of AND search)
                val brandDeferred = async {
                    MakeupApiClient.api.searchProducts(
                        brand = query,
                        productType = null, productTags = null, productCategory = null, priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                    )
                }
                val typeDeferred = async {
                    MakeupApiClient.api.searchProducts(
                        productType = query,
                        brand = null, productCategory = null, productTags = null, priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                    )
                }
                val tagDeferred = async {
                    MakeupApiClient.api.searchProducts(
                        productTags = query,
                        brand = null, productType = null, productCategory = null, priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                    )
                }
                val categoryDeferred = async {
                    MakeupApiClient.api.searchProducts(
                        productCategory = query,
                        brand = null, productType = null, productTags = null, priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                    )
                }

                val brandResponse = brandDeferred.await()
                val typeResponse = typeDeferred.await()
                val tagResponse = tagDeferred.await()
                val categoryResponse = categoryDeferred.await()

                val results = mutableListOf<Product>()

                if (brandResponse.isSuccessful) results += brandResponse.body().orEmpty()
                if (typeResponse.isSuccessful) results += typeResponse.body().orEmpty()
                if (tagResponse.isSuccessful) results += tagResponse.body().orEmpty()
                if (categoryResponse.isSuccessful) results += categoryResponse.body().orEmpty()

                val distinctResults = results.distinctBy { it.id }

                _products.value = distinctResults
                _recentlySearched.value = (_recentlySearched.value + distinctResults).distinctBy { it.id }.takeLast(10)

                distinctResults.firstOrNull()?.let { product ->
                    logSearchQueryToFirebase(product)
                }

                _error.value = if (distinctResults.isEmpty()) "No products found for '$query'" else null

            } catch (e: Exception) {
                _error.value = "Exception: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }


    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.email

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
            .collection("search_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
                Log.d("Firestore", "Loaded ${productList.size} products from Firestore")
            }
            .addOnFailureListener {
                _error.value = "Failed to load products from Firestore"
                Log.e("Firestore", "Error loading products", it)
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

    fun fetchProductFromUpc(upc: String) {
        viewModelScope.launch {
            try {
                val response = UPCItemApiClient.api.lookupUPC(upc)
                val item = response.items.firstOrNull()

                if (item != null ) {
                    val product = Product(
                        name = item.title,
                        brand = item.brand,
                        description = item.description,
                        image_link = item.images?.firstOrNull()
                    )

                    // Firestore check (if you have logic to verify brand match, etc)
                    item.brand?.let { brand ->
                        val exists = checkFirestoreForBrand(brand)
                        if (exists) {
                            _scannedProduct.value = product
                        } else {
                            _scannedProduct.value = null
                            _error.value = "Product not in database"
                        }
                    }
                } else {
                    _scannedProduct.value = null
                    _error.value = "No product found for this UPC"
                }
            } catch (e: Exception) {
                _scannedProduct.value = null
                _error.value = e.localizedMessage
            }
        }
    }

    private suspend fun checkFirestoreForBrand(brand: String): Boolean {
        val snapshot = Firebase.firestore.collection("all_products")
            .whereEqualTo("brand", brand)
            .get()
            .await()

        return !snapshot.isEmpty
    }

    suspend fun submitProductToFirestore(
        name: String,
        brand: String,
        description: String,
        ingredients: String,
        frontUri: Uri?,
        backUri: Uri?,
        barcode: String
    ) {
        val storage = Firebase.storage
        val firestore = Firebase.firestore
        val productData = mutableMapOf<String, Any>(
            "name" to name,
            "brand" to brand,
            "description" to description,
            "ingredients" to ingredients,
            "barcode" to barcode
        )

        // Upload front image
        frontUri?.let {
            val frontRef = storage.reference.child("products/${barcode}_front.jpg")
            frontRef.putFile(it).await()
            val frontUrl = frontRef.downloadUrl.await().toString()
            productData["front_image_url"] = frontUrl
        }

        // Upload back image
        backUri?.let {
            val backRef = storage.reference.child("products/${barcode}_back.jpg")
            backRef.putFile(it).await()
            val backUrl = backRef.downloadUrl.await().toString()
            productData["back_image_url"] = backUrl
        }

        // Save to Firestore
        firestore.collection("pending_products")  // Or "all_products"
            .document(barcode)
            .set(productData)
            .await()
    }


    fun resetState() {
        _scannedProduct.value = null
        _error.value = null
    }

    fun logSearchManually(it: Any) {

    }
}