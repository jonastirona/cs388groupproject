package com.example.modmycar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: PostRepository = LocalPostRepository()
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    fun loadInitial() {
        viewModelScope.launch {
            val data = repository.getFeed(limit = 20, offset = 0)
            _posts.value = data
        }
    }

    fun loadMore(currentCount: Int) {
        viewModelScope.launch {
            val data = repository.getFeed(limit = 20, offset = currentCount)
            _posts.value = _posts.value + data
        }
    }
}
