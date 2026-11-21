package com.example.ui.leaderboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.UiState
import com.example.domain.repository.CollectorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class LeaderboardSort(val label: String) {
    TOTAL_DUCKS("Most Ducks"),
    UNIQUE_SPECIES("Species"),
    TRADES("Top Traders"),
    GRAPES("Grapes")
}

class LeaderboardViewModel(
    collectorRepository: CollectorRepository
) : ViewModel() {

    private val _selectedSort = MutableStateFlow(LeaderboardSort.TOTAL_DUCKS)
    val selectedSort: StateFlow<LeaderboardSort> = _selectedSort.asStateFlow()

    val uiState: StateFlow<UiState<List<LeaderboardEntry>>> = combine(
        collectorRepository.observeLeaderboard(),
        _selectedSort
    ) { entries, sort ->
        val publicEntries = entries.filter { !it.isAdminAccount }
        val sorted = when (sort) {
            LeaderboardSort.TOTAL_DUCKS -> publicEntries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.duckCount }
                    .thenByDescending { it.uniqueSpeciesCount }
                    .thenByDescending { it.tradesCompleted }
                    .thenByDescending { it.grapeBalance }
            )
            LeaderboardSort.UNIQUE_SPECIES -> publicEntries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.uniqueSpeciesCount }
                    .thenByDescending { it.duckCount }
                    .thenByDescending { it.tradesCompleted }
            )
            LeaderboardSort.TRADES -> publicEntries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.tradesCompleted }
                    .thenByDescending { it.duckCount }
                    .thenByDescending { it.grapeBalance }
            )
            LeaderboardSort.GRAPES -> publicEntries.sortedWith(
                compareByDescending<LeaderboardEntry> { it.grapeBalance }
                    .thenByDescending { it.duckCount }
                    .thenByDescending { it.tradesCompleted }
            )
        }
        UiState.Success(sorted) as UiState<List<LeaderboardEntry>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing leaderboard", root)
            emit(UiState.Error(root.message ?: "Unable to load leaderboard right now."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun selectSort(sort: LeaderboardSort) {
        _selectedSort.value = sort
    }

    private companion object {
        const val TAG = "LeaderboardVM"
    }
}
