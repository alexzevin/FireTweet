package edu.utap.firetweet.model

data class Tweet(
    var id: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)