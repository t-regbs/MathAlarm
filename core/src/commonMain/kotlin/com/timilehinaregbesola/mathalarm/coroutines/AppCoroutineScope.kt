package com.timilehinaregbesola.mathalarm.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Application-scoped coroutine scope for background operations.
 * 
 * This replaces GlobalScope usage with a properly managed scope that:
 * - Uses SupervisorJob so child failures don't cancel siblings
 * - Uses Default dispatcher for CPU-intensive work
 * - Can be properly cancelled if needed during app shutdown
 * 
 */
class AppCoroutineScope : CoroutineScope {
    
    private val job = SupervisorJob()
    
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    /**
     * Launch a coroutine in the application scope.
     */
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
