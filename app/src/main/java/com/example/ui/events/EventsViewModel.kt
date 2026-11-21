package com.example.ui.events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DailyMissionCatalog
import com.example.domain.model.EventEntry
import com.example.domain.model.RubberDuck
import com.example.domain.model.UiState
import com.example.domain.model.Wallet
import com.example.domain.model.WeeklyEvent
import com.example.domain.repository.CollectorRepository
import com.example.domain.repository.EventRepository
import com.example.domain.repository.MissionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventsDashboardData(
    val profile: CollectorProfile? = null,
    val wallet: Wallet? = null,
    val myDucks: List<RubberDuck> = emptyList(),
    val events: List<WeeklyEvent> = emptyList(),
    val selectedEvent: WeeklyEvent? = null,
    val entries: List<EventEntry> = emptyList(),
    val votedEntryIds: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModel(
    private val currentUserId: String,
    private val collectorRepository: CollectorRepository,
    private val eventRepository: EventRepository,
    private val missionRepository: MissionRepository
) : ViewModel() {

    private val todayDateKey: String = DailyMissionCatalog.todayDateKey()

    private val _selectedEventId = MutableStateFlow<String?>(null)
    private val _votedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val eventsFlow = eventRepository.observeEvents()

    private val entriesFlow = combine(eventsFlow, _selectedEventId) { events, selectedId ->
        events.firstOrNull { it.id == selectedId } ?: events.firstOrNull { it.active } ?: events.firstOrNull()
    }.flatMapLatest { selected ->
        if (selected == null) {
            flowOf(null to emptyList<EventEntry>())
        } else {
            eventRepository.observeEventEntries(selected.id).map { entries -> selected to entries }
        }
    }

    private val collectorTripleFlow = combine(
        collectorRepository.observeProfile(currentUserId),
        collectorRepository.observeWallet(currentUserId),
        collectorRepository.observeMyDucks(currentUserId)
    ) { profile, wallet, ducks ->
        Triple(profile, wallet, ducks)
    }

    val uiState: StateFlow<UiState<EventsDashboardData>> = combine(
        collectorTripleFlow,
        eventsFlow,
        entriesFlow,
        _votedIds
    ) { (profile, wallet, ducks), events, (selected, entries), voted ->
        EventsDashboardData(
            profile = profile,
            wallet = wallet,
            myDucks = ducks,
            events = events,
            selectedEvent = selected,
            entries = entries,
            votedEntryIds = voted
        )
    }
        .map<EventsDashboardData, UiState<EventsDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing weekly events", root)
            emit(UiState.Error(root.message ?: "Failed to load weekly events."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            entriesFlow
                .catch { error -> unwrapFirestoreError(error) }
                .collect { (selected, entries) ->
                    if (selected != null && entries.isNotEmpty()) {
                        val voted = eventRepository.fetchVotedEntryIds(
                            eventId = selected.id,
                            userId = currentUserId,
                            entryIds = entries.map { it.id }
                        )
                        _votedIds.value = _votedIds.value + voted
                    }
                }
        }
    }

    fun selectEvent(eventId: String) {
        _selectedEventId.value = eventId
    }

    fun submitDuckEntry(duck: RubberDuck, caption: String) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val event = data.selectedEvent ?: return
        val profile = data.profile ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = eventRepository.submitEntry(currentUserId, profile, event.id, duck, caption)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "${duck.name} entered ${event.title}!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not submit duck entry."
            }
        }
    }

    fun cheerEntry(entry: EventEntry) {
        if (entry.userId == currentUserId) {
            _actionMessage.value = "You can vote for other collectors' ducks!"
            return
        }
        val voterName = (uiState.value as? UiState.Success)?.data?.profile?.displayName
            ?.ifBlank { "Collector" } ?: "Collector"
        viewModelScope.launch {
            _isBusy.value = true
            val result = eventRepository.voteForEntry(
                userId = currentUserId,
                voterName = voterName,
                eventId = entry.eventId,
                entry = entry
            )
            _isBusy.value = false
            result.onSuccess { earnedGrapes ->
                _votedIds.value = _votedIds.value + entry.id
                _actionMessage.value = "+$earnedGrapes Bonus Grapes earned for voting on ${entry.duckName}!"
                launch {
                    missionRepository.recordMissionProgress(
                        userId = currentUserId,
                        dateKey = todayDateKey,
                        missionId = DailyMissionCatalog.MISSION_CHEER_EVENT
                    )
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not vote for this entry."
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "EventsVM"
    }
}
