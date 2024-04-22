package edu.utap.firetweet.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel : ViewModel() {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    init {
        loadUserName()
    }

    fun setUserName(newUserName: String) {
        _userName.value = newUserName
    }

    fun loadUserName() {
        val user = FirebaseAuth.getInstance().currentUser
        _userName.value = user?.displayName ?: "Anonymous"
    }

}

