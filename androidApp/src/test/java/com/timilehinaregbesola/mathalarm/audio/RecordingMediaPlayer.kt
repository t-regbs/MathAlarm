package com.timilehinaregbesola.mathalarm.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Looper
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowMediaPlayer
import java.util.concurrent.CopyOnWriteArrayList

/** Records the thread at the actual platform boundary implicated by production ANRs. */
@Implements(MediaPlayer::class)
class RecordingMediaPlayer : ShadowMediaPlayer() {
    @Implementation
    override fun setDataSource(context: Context, uri: Uri) {
        record("setDataSource")
        beforeSource?.invoke()
        super.setDataSource(context, uri)
    }
    @Implementation
    override fun prepare() {
        record("prepare")
        super.prepare()
    }
    @Implementation
    override fun _release() {
        record("release")
        super._release()
    }
    private fun record(operation: String) {
        calls += operation to (Looper.myLooper() == Looper.getMainLooper())
    }
    companion object {
        @Volatile var beforeSource: (() -> Unit)? = null
        val calls = CopyOnWriteArrayList<Pair<String, Boolean>>()
    }
}
