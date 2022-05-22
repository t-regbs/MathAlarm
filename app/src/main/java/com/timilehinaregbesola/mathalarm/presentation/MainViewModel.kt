package com.timilehinaregbesola.mathalarm.presentation

import androidx.lifecycle.ViewModel
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.framework.Usecases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val usecases: Usecases) : ViewModel() {
    fun loadCurrentTheme(): Flow<AppThemeOptions> = usecases.loadAppTheme().map { it }
}
