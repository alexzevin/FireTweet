package edu.utap.firetweet.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.utap.firetweet.databinding.FragmentHomeBinding
import android.util.Log
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import edu.utap.firetweet.adapter.TweetAdapter
import edu.utap.firetweet.model.Tweet

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.buttonPost.setOnClickListener {
            postTweet()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = TweetAdapter(emptyList(), false)
        recyclerView.adapter = adapter
        FirebaseFirestore.getInstance().collection("tweets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }
                val tweetList = snapshots?.documents?.map { doc ->
                    Tweet(
                        userName = doc.getString("userName") ?: "Unknown",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } ?: listOf()
                Log.d("HomeFragment", "Number of tweets: ${tweetList.size}")
                adapter.setTweets(tweetList)
            }
        val swipeRefreshLayout: SwipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            fetchTweets(adapter, swipeRefreshLayout)
        }
        fetchTweets(adapter, swipeRefreshLayout)
        binding.editTextPost.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //
            }
            override fun afterTextChanged(s: Editable) {
                val charCount = s.length
                if (charCount > 0) {
                    binding.charCount.text = "$charCount/180"
                } else {
                    binding.charCount.text = ""
                }
            }
        })
    }

    fun fetchTweets(adapter: TweetAdapter, swipeRefreshLayout: SwipeRefreshLayout) {
        FirebaseFirestore.getInstance().collection("tweets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                val tweetList = snapshots.documents.map { doc ->
                    Tweet(
                        userName = doc.getString("userName") ?: "Unknown",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }
                adapter.setTweets(tweetList)
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.w("HomeFragment", "Error fetching documents", e)
                swipeRefreshLayout.isRefreshing = false
            }
    }

    fun postTweet() {
        val tweetText = binding.editTextPost.text.toString()
        if (tweetText.isNotEmpty()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userName = currentUser?.displayName ?: "Unknown User"

            val tweet = hashMapOf(
                "userName" to userName,
                "text" to tweetText,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("tweets")
                .add(tweet)
                .addOnSuccessListener { documentReference ->
                    Log.d("Tweet", "DocumentSnapshot added with ID: ${documentReference.id}")
                    val newTweet = Tweet(
                        id = documentReference.id,
                        userName = userName,
                        text = tweetText,
                        timestamp = System.currentTimeMillis()
                    )
                    binding.editTextPost.text.clear()
                    hideKeyboard()
                }
                .addOnFailureListener { e ->
                    Log.w("Tweet", "Error adding document", e)
                }
        } else {
            Toast.makeText(context, "Please enter text to post", Toast.LENGTH_SHORT).show()
        }
    }

    fun hideKeyboard() {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusedView = activity?.currentFocus
        currentFocusedView?.let {
            inputMethodManager?.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}