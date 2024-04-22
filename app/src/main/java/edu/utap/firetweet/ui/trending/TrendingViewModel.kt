package edu.utap.firetweet.ui.trending

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class TrendingViewModel : ViewModel() {

    private val _hashtags = MutableLiveData<List<Pair<String, Int>>>()
    val hashtags: LiveData<List<Pair<String, Int>>> = _hashtags

    fun loadHashtags() {
        FirebaseFirestore.getInstance().collection("tweets")
            .get()
            .addOnSuccessListener { snapshots ->
                val hashtagCounts = mutableMapOf<String, Int>()
                snapshots.documents.forEach { doc ->
                    val text = doc.getString("text") ?: ""
                    val words = text.split(" ")
                    val uniqueHashtags = words.filter { it.startsWith("#") }.map { it.replace(Regex("\\p{Punct}"), "") }.distinct()
                    uniqueHashtags.forEach { hashtag ->
                        hashtagCounts[hashtag] = (hashtagCounts[hashtag] ?: 0) + 1
                    }
                }
                val topHashtags = hashtagCounts.entries.sortedByDescending { it.value }.take(20).map { Pair(it.key, it.value) }
                _hashtags.value = topHashtags
            }
            .addOnFailureListener { e ->
                Log.w("TrendingViewModel", "Error fetching documents", e)
            }
    }
}
