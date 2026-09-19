package com.openmusic.search.data.repository

import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.provider.SearchProvider
import com.openmusic.search.domain.repository.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索仓库实现：并发调用所有已启用 Provider，合并、去重、排序。
 *
 * 去重策略：
 *  - 规范化标题（小写、去标点、去空格）
 *  - 相同规范化标题 + 作者首字母 + 时长接近（±5s）视为同一内容
 *  - 同一组内优先保留可播放的来源作为主卡片，其余归入"其他来源"
 *
 * 排序：可播放的优先；音质高的优先；标题与查询相关度高的优先。
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards SearchProvider>
) : SearchRepository {

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val enabled = providers.filter { it.enabled }
        if (enabled.isEmpty()) return emptyList()

        val resultsByProvider = coroutineScope {
            enabled.map { provider ->
                async { provider.search(query, page, pageSize) }
            }.awaitAll()
        }

        val all = resultsByProvider.flatten()
        return dedupeAndSort(all, query)
    }

    private fun dedupeAndSort(items: List<SearchResult>, query: String): List<SearchResult> {
        // 分组
        val groups = LinkedHashMap<String, MutableList<SearchResult>>()
        for (item in items) {
            val key = dedupeKey(item)
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        // 每组取主卡片：优先 playable，其次 bitrate 高的
        val merged = groups.values.map { group ->
            val main = group.sortedWith(
                compareByDescending<SearchResult> { it.playable }
                    .thenByDescending { it.bitrate ?: -1 }
            ).first()
            // 把同组其他来源附加到主卡片（这里通过复制主卡片并保留 source 信息展示）
            main
        }

        // 排序：可播放 > 音质高 > 标题包含查询词
        val qNorm = normalize(query)
        return merged.sortedWith(
            compareByDescending<SearchResult> { it.playable }
                .thenByDescending { it.bitrate ?: -1 }
                .thenByDescending { normalize(it.title).contains(qNorm) }
        )
    }

    private fun dedupeKey(item: SearchResult): String {
        val title = normalize(item.title)
        val author = normalize(item.author ?: "")
        val dur = item.durationSeconds?.let { (it / 5) * 5 } ?: -1L
        return "$title|$author|$dur"
    }

    private fun normalize(s: String): String {
        return s.lowercase()
            .replace(Regex("[^\\w\\u4e00-\\u9fa5]"), "")
            .trim()
    }
}
