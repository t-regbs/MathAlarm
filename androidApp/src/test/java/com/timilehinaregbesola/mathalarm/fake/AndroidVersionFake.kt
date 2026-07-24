package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersion

/**
 * Fake implementation of AndroidVersion for testing.
 */
class AndroidVersionFake(
    var version: Int = 26
) : AndroidVersion {
    override val currentVersion: Int
        get() = version
}
