package com.timilehinaregbesola.mathalarm.sound

data class AlarmSound(val id: String, val displayName: String)

/** iOS bundled sound library; Android uses its native device sound picker. */
object AlarmSoundCatalog {
    const val DEFAULT_SOUND = "alarm_orbit"

    val sounds = listOf(
        AlarmSound("alarm_daybreak", "Daybreak"),
        AlarmSound(DEFAULT_SOUND, "Orbit"),
        AlarmSound("alarm_rally", "Rally"),
        AlarmSound("alarm_glass_garden", "Glass Garden"),
        AlarmSound("alarm_stepping_stones", "Stepping Stones"),
        AlarmSound("alarm_clear_signal", "Clear Signal"),
    )

    fun find(tone: String): AlarmSound? {
        val id = tone.trim().removeSuffix(".caf").removeSuffix(".wav")
        return sounds.firstOrNull { it.id == id }
    }

    /** Empty or unavailable tones use the library default. */
    fun iosResourceName(tone: String): String =
        find(tone)?.id ?: DEFAULT_SOUND
}
