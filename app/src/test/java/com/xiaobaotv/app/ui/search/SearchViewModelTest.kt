package com.xiaobaotv.app.ui.search

import com.xiaobaotv.app.domain.model.SearchHistoryItem
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.domain.repository.ContentRepository
import com.xiaobaotv.app.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val requests = mutableListOf<String?>()
    private val requestedPages = mutableListOf<Int>()
    private var response: Result<List<VodContent>> = Result.success(emptyList())
    private var responseDelay = 0L
    private val repository = object : ContentRepository {
        override suspend fun getVodList(typeId: Int?, page: Int, limit: Int, query: String?, categoryId: Int?, ids: String?): Result<List<VodContent>> {
            requests += query
            requestedPages += page
            delay(responseDelay)
            return response
        }
        override suspend fun getVodDetail(id: Int): Result<VodContent> = error("Unused")
        override suspend fun cleanupStaleCache() = Unit
    }
    private val history = object : SearchHistoryRepository {
        override fun getRecentSearches() = flowOf(emptyList<SearchHistoryItem>())
        override suspend fun addSearchQuery(query: String) = Unit
        override suspend fun deleteSearchQuery(query: String) = Unit
        override suspend fun clearAll() = Unit
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun historyCanRepeatTheSameQueryAfterClearing() = runTest(dispatcher) {
        val model = SearchViewModel(repository, history)
        model.submitSearch("movie")
        advanceUntilIdle()
        model.clearSearch()
        model.submitSearch("movie")
        advanceUntilIdle()
        assertEquals(listOf("movie", "movie"), requests)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun clearingCancelsAnInFlightRequest() = runTest(dispatcher) {
        responseDelay = 1000
        response = Result.failure(IllegalStateException("Old error"))
        val model = SearchViewModel(repository, history)
        model.submitSearch("old")
        runCurrent()
        model.clearSearch()
        advanceUntilIdle()
        assertEquals("", model.uiState.value.query)
        assertNull(model.uiState.value.error)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun typingDebouncesAndBlankQueriesDoNotSearch() = runTest(dispatcher) {
        val model = SearchViewModel(repository, history)
        model.onQueryChange("a")
        runCurrent()
        advanceTimeBy(200)
        model.onQueryChange("ab")
        advanceUntilIdle()
        assertEquals(listOf("ab"), requests)
        model.onQueryChange("   ")
        advanceUntilIdle()
        assertEquals(1, requests.size)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun failedQueryCanBeRetriedImmediately() = runTest(dispatcher) {
        val model = SearchViewModel(repository, history)
        response = Result.failure(IllegalStateException("Offline"))
        model.submitSearch("movie")
        advanceUntilIdle()
        assertEquals("Offline", model.uiState.value.error)
        response = Result.success(emptyList())
        model.submitSearch()
        advanceUntilIdle()
        assertNull(model.uiState.value.error)
        assertEquals(2, requests.size)
    }

    @Test fun loadingMoreAppendsTheNextSearchPage() = runTest(dispatcher) {
        val first = vod(id = 1, name = "蜡笔小新")
        val laterMatch = vod(id = 35234481, name = "蜡笔小新第五季")
        val model = SearchViewModel(repository, history)

        response = Result.success(listOf(first))
        model.submitSearch("蜡笔小新")
        advanceUntilIdle()

        response = Result.success(listOf(laterMatch))
        model.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), requestedPages)
        assertEquals(listOf(first, laterMatch), model.uiState.value.results)
        assertTrue(model.uiState.value.hasMorePages)
    }

    private fun vod(id: Int, name: String) = VodContent(
        id = id,
        name = name,
        pic = "",
        actor = null,
        director = null,
        area = null,
        year = null,
        content = null,
        remarks = null,
        score = null,
        typeId = 0,
        typeName = null
    )
}
