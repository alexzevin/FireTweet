package edu.utap.firetweet.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.firetweet.databinding.FragmentProfileBinding
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import edu.utap.firetweet.adapter.TweetAdapter
import edu.utap.firetweet.model.Tweet
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var tweetAdapter: TweetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileViewModel.loadUserName()
        profileViewModel.userName.observe(viewLifecycleOwner) {
            binding.userNameEditText.setText(it)
        }
        tweetAdapter = TweetAdapter(emptyList(), true)
        binding.profileTweetsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tweetAdapter
        }
        loadUserTweets()
        binding.toggleEditSaveButton.setOnClickListener {
            if (binding.userNameEditText.isEnabled) {
                val newUsername = binding.userNameEditText.text.toString()
                if (newUsername.isNotBlank() && newUsername != profileViewModel.userName.value) {
                    saveUsername(profileViewModel.userName.value!!, newUsername)
                }
                binding.userNameEditText.isEnabled = false
                binding.toggleEditSaveButton.text = "Edit Username"
            } else {
                binding.userNameEditText.isEnabled = true
                binding.userNameEditText.requestFocus()
                binding.toggleEditSaveButton.text = "Save Username"
            }
        }
    }

    private fun saveUsername(oldUsername: String, newUsername: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build()
            firebaseUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    profileViewModel.setUserName(newUsername)
                    updateAllUserTweets(oldUsername, newUsername) {
                        loadUserTweets()
                    }
                } else {
                    Log.e("ProfileUpdate", "Failed to update Firebase Auth profile", task.exception)
                }
            }
        }
    }

    private fun updateAllUserTweets(oldUsername: String, newUsername: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("tweets")
            .whereEqualTo("userName", oldUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("UpdateTweets", "No tweets found with the username: $oldUsername")
                    onComplete()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    val docRef = document.reference
                    batch.update(docRef, "userName", newUsername)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("UpdateTweets", "All tweets have been updated with the new username: $newUsername")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("UpdateTweets", "Error updating tweets", e)
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UpdateTweets", "Error fetching tweets with the username: $oldUsername", e)
                onComplete()
            }
    }

    private fun loadUserTweets() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            FirebaseFirestore.getInstance().collection("tweets")
                .whereEqualTo("userName", firebaseUser.displayName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("ProfileFragment", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    val tweetList = snapshots?.documents?.mapNotNull { doc ->
                        val tweet = doc.toObject(Tweet::class.java)
                        tweet?.id = doc.id
                        tweet
                    } ?: emptyList()
                    tweetAdapter.setTweets(tweetList)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
