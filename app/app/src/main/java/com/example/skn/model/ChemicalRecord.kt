package com.example.skn.model

import com.squareup.moshi.Json

data class ChemicalSearchResponse(
    val help: String?,
    val success: Boolean,
    val result: ChemicalResult
)

data class ChemicalResult(
    val resource_id: String,
    val fields: List<Field>?,
    val records: List<ChemicalRecord>,
    val links: Links?,
    val limit: Int,
    val total: Int
)

data class Field(
    val id: String?,
    val type: String?
)

data class Links(
    val start: String?,
    val next: String?
)

data class ChemicalRecord(
    @Json(name = "_id") val id: Int?,
    @Json(name = "CDPHId") val cdphId: String?,
    @Json(name = "ProductName") val productName: String?,
    @Json(name = "CSFId") val csfId: String?,
    @Json(name = "CSF") val csf: String?,
    @Json(name = "CompanyId") val companyId: String?,
    @Json(name = "CompanyName") val companyName: String?,
    @Json(name = "BrandName") val brandName: String?,
    @Json(name = "PrimaryCategoryId") val primaryCategoryId: String?,
    @Json(name = "PrimaryCategory") val primaryCategory: String?,
    @Json(name = "SubCategoryId") val subCategoryId: String?,
    @Json(name = "SubCategory") val subCategory: String?,
    @Json(name = "CasId") val casId: String?,
    @Json(name = "CasNumber") val casNumber: String?,
    @Json(name = "ChemicalId") val chemicalId: String?,
    @Json(name = "ChemicalName") val chemicalName: String?,
    @Json(name = "InitialDateReported") val initialDateReported: String?,
    @Json(name = "MostRecentDateReported") val mostRecentDateReported: String?,
    @Json(name = "DiscontinuedDate") val discontinuedDate: String?,
    @Json(name = "ChemicalCreatedAt") val chemicalCreatedAt: String?,
    @Json(name = "ChemicalUpdatedAt") val chemicalUpdatedAt: String?,
    @Json(name = "ChemicalDateRemoved") val chemicalDateRemoved: String?,
    @Json(name = "ChemicalCount") val chemicalCount: Int?
)