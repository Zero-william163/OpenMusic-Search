package com.openmusic.search.presentation.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.repository.SearchRepository

class SearchPagingSource(
    private val repository: SearchRepository,
    private val query: String,
    private val pageSize: Int = 20
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val page = params.key ?: 1
        return try {
            val results = repository.search(query, page, pageSize)
            LoadResult.Page(
                data = results,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (results.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
