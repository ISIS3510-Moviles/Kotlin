package com.example.campusbites.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.campusbites.data.local.ListStringConverter

@Entity(tableName = "pending_product_actions")
@TypeConverters(ListStringConverter::class)
data class PendingProductActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // "CREATE", "UPDATE", "DELETE"
    val productId: String?, // ID del producto si es UPDATE/DELETE, o ID local temporal si es CREATE
    val name: String?,
    val description: String?,
    val price: Float?,
    val photo: String?,
    val restaurantId: String?, // ID del restaurante asociado, crucial para refrescar caché
    val ingredientsIds: List<String>?,
    val foodTagsIds: List<String>?,
    val dietaryTagsIds: List<String>?,
    val timestamp: Long = System.currentTimeMillis()
)