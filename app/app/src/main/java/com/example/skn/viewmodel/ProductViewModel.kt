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
        val isLikelyIngredient = isLikelyIngredientSearch(query)

        viewModelScope.launch {
            try {
                // If it looks like an ingredient search and we want to prioritize getting all products
                // to search through descriptions, we can skip API calls or modify the flow here
                val results = mutableListOf<Product>()

                // For likely ingredients, we fetch all products right away for thorough searching
                if (isLikelyIngredient) {
                    Log.d("Search", "Likely ingredient search for: $query")
                    val allProductsResponse = MakeupApiClient.api.getAllProducts()
                    if (allProductsResponse.isSuccessful) {
                        results += allProductsResponse.body().orEmpty()
                        Log.d("Search", "Got ${results.size} products from API to search for ingredient")
                    } else {
                        // API fallback to Firestore
                        val db = Firebase.firestore
                        val firestoreProducts = db.collection("all_products")
                            .get()
                            .await()
                            .toObjects(Product::class.java)
                        results += firestoreProducts
                        Log.d("Search", "Got ${firestoreProducts.size} products from Firestore to search for ingredient")
                    }
                } else {
                    // Regular search via API for non-ingredient queries
                    val brandDeferred = async {
                        MakeupApiClient.api.searchProducts(
                            brand = query,
                            productType = null, productTags = null, productCategory = null,
                            priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                        )
                    }
                    val typeDeferred = async {
                        MakeupApiClient.api.searchProducts(
                            productType = query,
                            brand = null, productCategory = null, productTags = null,
                            priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                        )
                    }
                    val tagDeferred = async {
                        MakeupApiClient.api.searchProducts(
                            productTags = query,
                            brand = null, productType = null, productCategory = null,
                            priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                        )
                    }
                    val categoryDeferred = async {
                        MakeupApiClient.api.searchProducts(
                            productCategory = query,
                            brand = null, productType = null, productTags = null,
                            priceGreaterThan = null, priceLessThan = null, ratingGreaterThan = null, ratingLessThan = null
                        )
                    }

                    val brandResponse = brandDeferred.await()
                    val typeResponse = typeDeferred.await()
                    val tagResponse = tagDeferred.await()
                    val categoryResponse = categoryDeferred.await()

                    if (brandResponse.isSuccessful) results += brandResponse.body().orEmpty()
                    if (typeResponse.isSuccessful) results += typeResponse.body().orEmpty()
                    if (tagResponse.isSuccessful) results += tagResponse.body().orEmpty()
                    if (categoryResponse.isSuccessful) results += categoryResponse.body().orEmpty()

                    // If standard search found nothing, try getting all products as a fallback
                    if (results.isEmpty()) {
                        val allProductsResponse = MakeupApiClient.api.getAllProducts()
                        if (allProductsResponse.isSuccessful) {
                            results += allProductsResponse.body().orEmpty()
                        } else {
                            // If the API call fails, try to get products from Firestore
                            val db = Firebase.firestore
                            val firestoreProducts = db.collection("all_products")
                                .get()
                                .await()
                                .toObjects(Product::class.java)
                            results += firestoreProducts
                        }
                    }
                }

                val distinctResults = results.distinctBy { it.id }
                    .filter { product ->
                        // Basic field search
                        product.name?.lowercase()?.contains(query) == true ||
                                product.brand?.lowercase()?.contains(query) == true ||
                                product.product_type?.lowercase()?.contains(query) == true ||
                                product.category?.lowercase()?.contains(query) == true ||
                                product.tag_list?.any { it.lowercase().contains(query) } == true ||

                                // Enhanced description search for ingredients
                                if (isLikelyIngredient) {
                                    // More thorough description parsing for ingredients
                                    searchDescriptionForIngredient(product.description, query)
                                } else {
                                    // Simple description search for non-ingredients
                                    product.description?.lowercase()?.contains(query) == true
                                }
                    }

                // Log search results
                Log.d("Search", "Found ${distinctResults.size} results for query: $query")

                _products.value = distinctResults
                _recentlySearched.value = (_recentlySearched.value + distinctResults).distinctBy { it.id }.takeLast(10)

                distinctResults.firstOrNull()?.let { product ->
                    logSearchQueryToFirebase(product)
                }

                _error.value = if (distinctResults.isEmpty()) "No products found for '$query'" else null

            } catch (e: Exception) {
                Log.e("Search", "Error searching for $query", e)
                _error.value = "Exception: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Determines if a search query is likely to be an ingredient name
    private fun isLikelyIngredientSearch(query: String): Boolean {
        // Common ingredient keywords
        val ingredientTerms = listOf(
            "acid", "extract", "oil", "butter", "powder", "sodium", "zinc", "oxide",
            "vitamin", "water", "glycerin", "alcohol", "fragrance", "parfum",
            "extract", "seed", "fruit", "leaf", "root", "stem", "flower", "petal",
            "wax", "resin", "clay", "salt", "protein"
        )

        // Check if query contains common ingredient suffixes or terms
        return query.contains(" ").not() && // Single word is more likely an ingredient
                (ingredientTerms.any { query.contains(it) } || // Contains ingredient term
                        commonIngredientNames.any { it.equals(query, ignoreCase = true) }) // Is a common ingredient
    }

    // Common skincare/makeup ingredients (to make exact matching more efficient)
    private val commonIngredientNames = listOf(
        "water", "aqua", "glycerin", "titanium dioxide", "mica", "talc", "silica",
        "iron oxide", "dimethicone", "tocopherol", "parfum", "fragrance", "benzyl",
        "citral", "limonene", "linalool", "glycol", "propylene", "shea butter",
        "coconut oil", "jojoba oil", "argan oil", "aloe vera", "hyaluronic acid",
        "retinol", "niacinamide", "peptide", "ceramide", "squalane", "panthenol",
        "zinc oxide", "salicylic acid", "lactic acid", "glycolic acid"
    )

    // Enhanced description search specifically for ingredient mentions
    private fun searchDescriptionForIngredient(description: String?, query: String): Boolean {
        if (description == null) return false

        val desc = description.lowercase()

        // Early return for direct match
        if (desc.contains(query)) return true

        // More advanced ingredient extraction techniques

        // 1. Look for explicit ingredient listings
        val ingredientSectionMarkers = listOf(
            "ingredients:", "ingredients include:", "contains:", "composition:",
            "ingredient list:", "formulated with:", "ingredients list:", "made with:"
        )

        for (marker in ingredientSectionMarkers) {
            val index = desc.indexOf(marker)
            if (index >= 0) {
                val ingredientSection = desc.substring(index + marker.length)

                // Check for ingredient within common ingredient list delimiters
                val ingredients = ingredientSection.split(",", ";", "•", ".", "&amp;", "&").map { it.trim() }
                if (ingredients.any { it.contains(query) }) {
                    return true
                }
            }
        }

        // 2. Look for ingredients mentioned in product highlights
        val wordList = desc.split(" ", "\n", "\t").map { it.trim() }
        val queryWords = query.split(" ")

        // Check for contiguous matching words
        for (i in 0 until wordList.size - queryWords.size + 1) {
            var match = true
            for (j in queryWords.indices) {
                if (!wordList[i + j].contains(queryWords[j])) {
                    match = false
                    break
                }
            }
            if (match) return true
        }

        // 3. Check for ingredient mentioned near product benefit terms
        val benefitTerms = listOf(
            "infused with", "enriched with", "contains", "with", "featuring",
            "includes", "enhanced with", "fortified with", "powered by"
        )

        for (term in benefitTerms) {
            val index = desc.indexOf(term)
            if (index >= 0) {
                val followingText = desc.substring(index + term.length, (index + term.length + 50).coerceAtMost(desc.length))
                if (followingText.contains(query)) {
                    return true
                }
            }
        }

        return false
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

    fun checkIfProductIsPending(barcode: String) {
        viewModelScope.launch {
            try {
                val snapshot = Firebase.firestore.collection("pending_products")
                    .document(barcode)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val product = Product(
                        name = snapshot.getString("name"),
                        brand = snapshot.getString("brand"),
                        description = snapshot.getString("description"),
                        ingredients = snapshot.getString("ingredients"),
                        image_link = snapshot.getString("front_image_url") // optional
                    )

                    _scannedProduct.value = product
                    _error.value = null
                } else {
                    _scannedProduct.value = null
                    _error.value = "Product not in pending database"
                }
            } catch (e: Exception) {
                _scannedProduct.value = null
                _error.value = "Error checking Firestore: ${e.localizedMessage}"
            }
        }
    }

    fun prefillProductInfoFromUPC(barcode: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = UPCItemApiClient.api.lookupUPC(barcode)
                val item = response.items.firstOrNull()

                if (item != null) {
                    val product = Product(
                        name = item.title,
                        brand = item.brand,
                        description = item.description,
                        image_link = item.images?.firstOrNull()
                    )
                    onResult(product)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("UPC Lookup", "Error: ${e.localizedMessage}", e)
                onResult(null)
            }
        }
    }



    suspend fun submitProductToFirestore(
        name: String,
        brand: String,
        description: String,
        ingredients: String,
        frontUri: Uri?,
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
            if (it.scheme == "content" || it.scheme == "file") {
                val frontRef = storage.reference.child("products/${barcode}_front.jpg")
                frontRef.putFile(it).await()
                val frontUrl = frontRef.downloadUrl.await().toString()
                productData["front_image_url"] = frontUrl
            } else {
                // It's probably a remote URL (from UPC API) – just store it directly
                productData["front_image_url"] = it.toString()
            }
        }


        // Save to Firestore
        firestore.collection("pending_products")
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