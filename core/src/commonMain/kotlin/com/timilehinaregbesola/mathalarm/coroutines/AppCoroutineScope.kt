package com.timilehinaregbesola.mathalarm.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class AppCoroutineScope(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope {
    
    private val job = SupervisorJob()
    
    override val coroutineContext: CoroutineContext
        get() = dispatcher + job

    fun launch(block: suspend CoroutineScope.() -> Unit) = 
        (this as CoroutineScope).launch(block = block)

    /**
     * Cancel all coroutines in this scope.
     * Should be called during application shutdown if needed.
     */
    fun cancel() {
        job.cancel()
    }
}
