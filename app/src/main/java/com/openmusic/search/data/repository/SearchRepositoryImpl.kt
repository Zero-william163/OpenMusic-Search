package com.openmusic.search.data.repository

import android.util.Log
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.domain.model.Source
import com.openmusic.search.domain.provider.SearchProvider
import com.openmusic.search.domain.repository.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索仓库实现。
 *
 * 关键设计：
 *  - 每个 Provider 有独立超时（8s），超时直接跳过，**不等最慢的那个**。
 *  - 先搜"正常源"（Jamendo / IA / Wikimedia / YouTube）。
 *  - 如果全部返回空（真的搜不到 + 所有源都挂），用 SoundHelix 保底——永远不返回空。
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards SearchProvider>
) : SearchRepository {

    companion object {
        private const val TAG = "SearchRepo"
        private const val PROVIDER_TIMEOUT_MS = 8_000L
    }

    override suspend fun search(query: String, page: Int, pageSize: Int): List<SearchResult> {
        val normalProviders = providers.filter {
            it.enabled && it.id != Source.SOUNDHELIX.name
        }
        val fallbackProvider = providers.firstOrNull { it.id == Source.SOUNDHELIX.name && it.enabled }

        val resultsByProvider = coroutineScope {
            normalProviders.map { provider ->
                async {
                    val r = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        runCatching { provider.search(query, page, pageSize) }
                            .getOrElse {
                                Log.w(TAG, "${provider.id} 搜索失败: ${it.message}")
                                emptyList()
                            }
                    }
                    if (r == null) {
                        Log.w(TAG, "${provider.id} 搜索超时 (${PROVIDER_TIMEOUT_MS}ms)")
                        emptyList()
                    } else {
                        Log.d(TAG, "${provider.id} → ${r.size} 条")
                        r
                    }
                }
            }.awaitAll()
        }

        val all = resultsByProvider.flatten()
        Log.d(TAG, "正常源合计 ${all.size} 条")

        if (all.isEmpty() && fallbackProvider != null) {
            // 全部为空 → 保底
            Log.i(TAG, "所有正常源无结果，使用 SoundHelix 保底")
            val fallback = runCatching { fallbackProvider.search(query, page, pageSize) }
                .getOrDefault(emptyList())
            Log.d(TAG, "SoundHelix → ${fallback.size} 条")
            return fallback
        }

        return dedupeAndSort(all, query)
    }

    private fun dedupeAndSort(items: List<SearchResult>, query: String): List<SearchResult> {
        val groups = LinkedHashMap<String, MutableList<SearchResult>>()
        for (item in items) {
            val key = dedupeKey(item)
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        val merged = groups.values.map { group ->
            group.sortedWith(
                compareByDescending<SearchResult> { it.playable }
                    .thenByDescending { it.bitrate ?: -1 }
            ).first()
        }

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
