package com.timilehinaregbesola.mathalarm.framework

/**
 * Enum to represent the app theme selected by the user.
 *
 * @property id the theme id
 */
enum class DataStoreThemeOptions(val id: Int) {

    /**
     * Light app theme.
     */
    LIGHT(0),

    /**
     * Dark app theme.
     */
    DARK(1),

    /**
     * System-based app theme.
     */
    SYSTEM(2)
}
