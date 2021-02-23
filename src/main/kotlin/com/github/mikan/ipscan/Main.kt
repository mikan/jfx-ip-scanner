package com.github.mikan.ipscan

import com.github.mikan.ipscan.ui.App
import javafx.application.Application

fun main() {
    println("JFX IP Scanner v" + Version.get() + " on JRE v" + System.getProperty("java.version"))
    Application.launch(App::class.java)
}
