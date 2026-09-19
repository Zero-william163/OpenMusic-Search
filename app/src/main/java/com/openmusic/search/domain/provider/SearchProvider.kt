package com.openmusic.search.domain.provider

import com.openmusic.search.domain.model.SearchResult

/**
 * 搜索 Provider 抽象。每个来源独立实现搜索与分页。
 * UI 不直接依赖具体平台，只依赖此接口。
 */
interface SearchProvider {
    val id: String
    val displayName: String
    val enabled: Boolean get() = true
    val requiresApiKey: Boolean get() = false

    /**
     * 搜索一页结果。
     * @param query 关键词
     * @param page  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 该页结果列表；空列表表示该来源已无更多结果。
     */
    suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult>
}
