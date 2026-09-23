package com.timilehinaregbesola.mathalarm.presentation.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Retains the visit's eligibility across rotation, but refreshes it after backgrounding. */
class ReviewVisitViewModel : ViewModel() {
    var visit by mutableStateOf(0)
        private set
    var coordinator: InAppReviewCoordinator? = null
        private set
    private var needsNewVisit = true

    fun onStart(createCoordinator: () -> InAppReviewCoordinator?) {
        if (!needsNewVisit) return
        coordinator = createCoordinator()
        visit++
        needsNewVisit = false
    }

    fun onStop(changingConfiguration: Boolean) {
        coordinator?.invalidatePendingRequest()
        needsNewVisit = !changingConfiguration
    }
}
