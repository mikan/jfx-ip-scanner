package com.github.mikan.ipscan

import java.util.Properties

object Version {
    // gradle task "genVersionFile"
    private const val VERSION_FILE = "version.properties"

    fun get(): String {
        val prop = Properties()
        val resource = this.javaClass.classLoader.getResource(VERSION_FILE)
            ?: throw InternalError("$VERSION_FILE unavailable")
        prop.load(resource.openStream())
        return prop.getProperty("app.version", "0.0.0")
    }
}
