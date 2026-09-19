package com.openmusic.search.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.openmusic.search.data.local.db.SearchHistoryDao
import com.openmusic.search.data.local.db.entity.SearchHistoryEntity
import com.openmusic.search.domain.model.AudioQualityFilter
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.repository.SearchRepository
import com.openmusic.search.playback.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
    val playerManager: PlayerManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _qualityFilter = MutableStateFlow(AudioQualityFilter.DEFAULT)
    val qualityFilter: StateFlow<AudioQualityFilter> = _qualityFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<SearchResult>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(PagingData.empty())
            else Pager(PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false)) {
                SearchPagingSource(repository, q, 20)
            }.flow.cachedIn(viewModelScope)
        }

    val history: Flow<List<SearchHistoryEntity>> = searchHistoryDao.observeAll()

    fun onQueryChange(q: String) { _query.value = q }

    fun search(q: String) {
        _query.value = q
        if (q.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryDao.insert(SearchHistoryEntity(query = q))
            }
        }
    }

    fun setQualityFilter(filter: AudioQualityFilter) { _qualityFilter.value = filter }

    fun clearHistory() {
        viewModelScope.launch { searchHistoryDao.clear() }
    }

    fun playItem(item: SearchResult, list: List<SearchResult>) {
        val playableList = list.filter { it.playable }
        if (playableList.isEmpty()) return
        val startIndex = playableList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        playerManager.playQueue(playableList, startIndex)
    }
}
