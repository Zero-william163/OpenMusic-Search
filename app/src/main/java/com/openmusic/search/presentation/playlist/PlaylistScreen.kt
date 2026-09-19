package com.openmusic.search.presentation.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openmusic.search.data.local.db.FavoriteDao
import com.openmusic.search.data.local.db.entity.FavoriteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao
) : ViewModel() {
    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(id: String) { viewModelScope.launch { favoriteDao.delete(id) } }
}

@Composable
fun PlaylistScreen(viewModel: PlaylistViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("收藏", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有收藏的内容", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(favorites) { f ->
                    Text(
                        text = f.title,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
