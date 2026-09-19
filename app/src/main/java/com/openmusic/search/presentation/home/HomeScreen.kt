package com.openmusic.search.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.openmusic.search.presentation.search.SearchResultCard
import com.openmusic.search.presentation.search.SearchViewModel

private val QUICK_SEARCHES = listOf("轻音乐", "Lo-fi", "Piano", "Classical", "Podcast", "雨声", "白噪音", "BBC English")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenOriginal: (String) -> Unit,
    onDownload: (com.openmusic.search.domain.model.SearchResult) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results = viewModel.results.collectAsLazyPagingItems()
    val history by viewModel.history.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索音乐、音频或视频……") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            // 快捷搜索
            Text("快捷搜索", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_SEARCHES.take(4).forEach { tag ->
                    AssistChip(onClick = { viewModel.search(tag) }, label = { Text(tag) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QUICK_SEARCHES.drop(4).forEach { tag ->
                    AssistChip(onClick = { viewModel.search(tag) }, label = { Text(tag) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 最近搜索
            if (history.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("最近搜索", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearHistory() }) { Text("清空") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.take(6).forEach { h ->
                        AssistChip(onClick = { viewModel.search(h.query) }, label = { Text(h.query) })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 搜索结果列表（无限滚动）
        Box(modifier = Modifier.weight(1f)) {
            when {
                results.loadState.refresh is LoadState.Loading && query.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                results.itemCount == 0 && query.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("没有找到相关结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results.itemCount) { index ->
                            results[index]?.let { item ->
                                SearchResultCard(
                                    item = item,
                                    onPlay = {
                                        val loaded = results.itemSnapshotList.items.filterNotNull()
                                        viewModel.playItem(item, loaded)
                                    },
                                    onDownload = { onDownload(item) },
                                    onOpenOriginal = { onOpenOriginal(item.originalUrl) }
                                )
                            }
                        }
                        if (results.loadState.append is LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        if (results.loadState.append is LoadState.Error) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                    Button(onClick = { results.retry() }) { Text("重试") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
