package com.example.campusbites.data.cache

import android.util.Log
import com.example.campusbites.domain.model.CommentDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Singleton
class InMemoryReviewCache @Inject constructor(
    private val applicationScope: CoroutineScope
) {

    private val _reviews = MutableStateFlow<HashMap<String, CommentDomain>>(HashMap())

    val reviews: StateFlow<List<CommentDomain>> =
        _reviews.asStateFlow()
            .map { it.values.toList() }
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    fun updateReviews(newReviews: List<CommentDomain>) {
        Log.d("InMemoryReviewCache", "Updating cache with ${newReviews.size} reviews.")
        val newMap = HashMap<String, CommentDomain>()
        newReviews.forEach { review ->
            newMap[review.id] = review
        }
        _reviews.value = newMap
    }

    fun getReviews(): List<CommentDomain> {
        return _reviews.value.values.toList()
    }

    fun getReviewById(reviewId: String): CommentDomain? {
        return _reviews.value[reviewId]
    }

    fun clearCache() {
        Log.d("InMemoryReviewCache", "Clearing review cache.")
        _reviews.value = HashMap()
    }

    init {
        Log.d("InMemoryReviewCache", "InMemoryReviewCache Singleton instance created: ${this.hashCode()}")
    }
}