package edu.utap.firetweet.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.firetweet.model.Tweet
import edu.utap.firetweet.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.Spannable
import android.graphics.Color

class TweetAdapter(private var tweets: List<Tweet>, private val enableDelete: Boolean) : RecyclerView.Adapter<TweetAdapter.TweetViewHolder>() {

    class TweetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameTextView: TextView = view.findViewById(R.id.userName)
        val textView: TextView = view.findViewById(R.id.tweetText)
        val timestampTextView: TextView = view.findViewById(R.id.timestamp)
        val deleteIcon: ImageView = view.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tweet_item, parent, false)
        return TweetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        val tweet = tweets[position]
        holder.userNameTextView.text = tweet.userName
        holder.textView.text = colorHashtags(tweet.text)
        holder.timestampTextView.text = SimpleDateFormat("h:mm a \u2022 MMM. d, yyyy", Locale.getDefault()).format(Date(tweet.timestamp))
        holder.deleteIcon.visibility = if (enableDelete) View.VISIBLE else View.GONE
        holder.deleteIcon.setOnClickListener {
            deleteTweet(position)
        }
    }

    fun setTweets(newTweets: List<Tweet>) {
        this.tweets = newTweets
        notifyDataSetChanged()
    }

    private fun deleteTweet(position: Int) {
        val tweet = tweets[position]
        FirebaseFirestore.getInstance().collection("tweets")
            .document(tweet.id)
            .delete()
            .addOnSuccessListener {
                Log.d("TweetAdapter", "Tweet deleted successfully.")
                tweets = tweets.filterIndexed { index, _ -> index != position }
                notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("TweetAdapter", "Error deleting tweet", e)
            }
    }

    private fun colorHashtags(text: String): SpannableString {
        val spannableString = SpannableString(text)
        val words = text.split(" ")
        var index = 0
        for (word in words) {
            if (word.startsWith("#")) {
                var hashtagEnd = word.length
                while (hashtagEnd > 1 && !Character.isLetterOrDigit(word[hashtagEnd - 1])) {
                    hashtagEnd--
                }
                spannableString.setSpan(
                    ForegroundColorSpan(Color.parseColor("#FFA500")),
                    index,
                    index + hashtagEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (hashtagEnd < word.length) {
                    spannableString.setSpan(
                        ForegroundColorSpan(Color.BLACK),
                        index + hashtagEnd,
                        index + word.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                spannableString.setSpan(
                    ForegroundColorSpan(Color.BLACK),
                    index,
                    index + word.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            index += word.length + 1
        }
        return spannableString
    }

    override fun getItemCount() = tweets.size
}
