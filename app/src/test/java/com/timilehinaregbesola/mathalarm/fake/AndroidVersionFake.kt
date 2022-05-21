package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersion

class AndroidVersionFake : AndroidVersion {

    var version: Int = 0

    override val currentVersion: Int
        get() = version
}
