package com.timilehinaregbesola.mathalarm.presentation.whatsnew

/** Keep released features in order and retain their IDs across app versions. */
internal enum class AnnouncementFeature(val id: String) {
    MATH_CHALLENGES("math-challenges-v1"),
    SKIP_NEXT("skip-next-alarm-v1"),
}

internal fun announcementFeaturesToShow(
    hasSeen: (String) -> Boolean,
): List<AnnouncementFeature> = AnnouncementFeature.entries.filter { !hasSeen(it.id) }
