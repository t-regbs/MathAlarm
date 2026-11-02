package com.timilehinaregbesola.mathalarm.presentation

import com.rickclephas.kmp.observableviewmodel.ViewModel
import androidx.lifecycle.ViewModelStore

/** Native owners clear observation. Accepted application work has a separate lifetime. */
abstract class SharedFeatureViewModel : ViewModel() {
    var isClosed: Boolean = false
        private set

    fun close() {
        if (isClosed) return
        ViewModelStore().apply { put("feature", this@SharedFeatureViewModel); clear() }
    }

    override fun onCleared() {
        isClosed = true
        super.onCleared()
    }
}
