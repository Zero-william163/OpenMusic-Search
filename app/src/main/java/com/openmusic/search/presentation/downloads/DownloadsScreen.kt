package com.openmusic.search.presentation.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.openmusic.search.data.local.db.DownloadDao
import com.openmusic.search.data.local.db.entity.DownloadEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadDao: DownloadDao
) : ViewModel() {
    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("下载", style = MaterialTheme.typography.titleLarge)
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(downloads) { d ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(d.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "状态：${d.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (d.status == "DOWNLOADING") {
                            LinearProgressIndicator(
                                progress = { d.progress / 100f },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
