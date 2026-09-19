package com.openmusic.search.domain.repository

import com.openmusic.search.domain.model.SearchResult

interface SearchRepository {
    /**
     * 跨所有已启用 Provider 并发搜索一页，合并、去重、按音质/相关度排序。
     */
    suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult>
}
