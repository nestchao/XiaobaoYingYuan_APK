package com.xiaobaotv.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaobaotv.app.domain.model.SearchHistoryItem
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.domain.repository.ContentRepository
import com.xiaobaotv.app.domain.repository.SearchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val results: List<VodContent> = emptyList(),
    val error: String? = null,
    val loadMoreError: String? = null,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = false,
    val recentSearches: List<SearchHistoryItem> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeRecentSearches()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepository.getRecentSearches().collect { list ->
                _uiState.update { it.copy(recentSearches = list) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        search(newQuery, debounce = true)
    }

    fun submitSearch(query: String = _uiState.value.query) {
        search(query.trim(), debounce = false)
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                results = emptyList(),
                error = null,
                loadMoreError = null,
                isLoading = query.isNotBlank(),
                isLoadingMore = false,
                currentPage = 0,
                hasMorePages = false
            )
        }
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            if (debounce) delay(500)
            val result = contentRepository.getVodList(query = query.trim())
            coroutineContext.ensureActive()
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = list,
                        currentPage = 1,
                        hasMorePages = list.isNotEmpty()
                    )
                }
                searchHistoryRepository.addSearchQuery(query.trim())
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message ?: "搜索失败，请重试") }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank() || state.isLoading || state.isLoadingMore || !state.hasMorePages) return

        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        searchJob = viewModelScope.launch {
            val result = contentRepository.getVodList(query = query, page = nextPage)
            coroutineContext.ensureActive()
            result.onSuccess { pageResults ->
                _uiState.update { current ->
                    if (current.query != query) current else current.copy(
                        isLoadingMore = false,
                        results = (current.results + pageResults).distinctBy { it.id },
                        currentPage = nextPage,
                        hasMorePages = pageResults.isNotEmpty()
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    if (current.query != query) current else current.copy(
                        isLoadingMore = false,
                        loadMoreError = error.message ?: "加载更多失败，请重试"
                    )
                }
            }
        }
    }

    fun retryLoadNextPage() = loadNextPage()

    fun clearSearch() = search("", debounce = false)

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteSearchQuery(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
        }
    }
}
