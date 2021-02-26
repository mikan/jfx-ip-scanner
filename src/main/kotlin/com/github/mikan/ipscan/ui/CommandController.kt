package com.github.mikan.ipscan.ui

import com.github.mikan.ipscan.domain.NetworkCommand
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import java.io.IOException
import java.net.URL
import java.util.ResourceBundle
import java.util.Scanner

class CommandController : Initializable {
    private var process: Process? = null

    @FXML
    private lateinit var titleLabel: Label

    @FXML
    private lateinit var resultTextArea: TextArea

    @FXML
    private lateinit var progressBox: HBox

    @FXML
    private lateinit var progressIndicator: ProgressIndicator

    @FXML
    private lateinit var resultLabel: Label

    @FXML
    lateinit var okButton: Button

    private interface CompleteHandler {
        fun onComplete(result: String, status: Int)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
    }

    fun startCommand(command: NetworkCommand, vararg params: String) {
        when (command) {
            NetworkCommand.PING -> {
                titleLabel.text = App.messages.getString("home.button.ping")
                if (params.size != 1) {
                    error(App.messages.getString("command.dialog.error.param") + " $params")
                    return
                }
                Thread { startPing(params[0]) }.start()
            }
            NetworkCommand.ARP -> {
                titleLabel.text = App.messages.getString("home.button.arp")
                if (params.size != 2) {
                    error(App.messages.getString("command.dialog.error.param") + " $params")
                    return
                }
                Thread { startArp(params[0], params[1]) }.start()
            }
            NetworkCommand.TRACEROUTE -> {
                titleLabel.text = App.messages.getString("home.button.traceroute")
                if (params.size != 1) {
                    error(App.messages.getString("command.dialog.error.param") + " $params")
                    return
                }
                Thread { startTraceRoute(params[0]) }.start()
            }
        }
    }

    fun stopCommand() {
        if (process?.isAlive == true) {
            process?.destroy()
        }
    }

    private fun startPing(target: String) {
        execute(
            NetworkCommand.encodePingCommand(target, 3, 3000),
            object : CompleteHandler {
                override fun onComplete(result: String, status: Int) {
                    if (NetworkCommand.validatePingResult(status, resultTextArea.text)) {
                        success(App.messages.getString("command.progress.success"))
                    } else {
                        error(App.messages.getString("command.progress.failed"))
                    }
                }
            }
        )
    }

    private fun startArp(ifAddress: String, ifName: String) {
        execute(
            NetworkCommand.encodeArpCommand(ifAddress, ifName),
            object : CompleteHandler {
                override fun onComplete(result: String, status: Int) {
                    if (status == 0) {
                        success(App.messages.getString("command.progress.complete"))
                    } else {
                        error(App.messages.getString("command.progress.exit_status_prefix") + " " + status)
                    }
                }
            }
        )
    }

    private fun startTraceRoute(target: String) {
        execute(
            NetworkCommand.encodeTraceRouteCommand(target),
            object : CompleteHandler {
                override fun onComplete(result: String, status: Int) {
                    if (status == 0) {
                        success(App.messages.getString("command.progress.complete"))
                    } else {
                        error(App.messages.getString("command.progress.exit_status_prefix") + " " + status)
                    }
                }
            }
        )
    }

    private fun execute(command: String, handler: CompleteHandler) {
        try {
            val p = Runtime.getRuntime().exec(command)
            process = p
            Scanner(p.inputStream).use {
                while (it.hasNextLine()) {
                    val line = it.nextLine()
                    Platform.runLater { resultTextArea.appendText(line + "\n") }
                }
            }
            Thread {
                try {
                    p.waitFor()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Platform.runLater { error(App.messages.getString("command.progress.terminated")) }
                    return@Thread
                }
                Platform.runLater {
                    handler.onComplete(resultTextArea.text, p.exitValue())
                }
            }.start()
        } catch (e: IOException) {
            Platform.runLater { error(App.messages.getString("command.progress.failed_run_prefix") + " " + e.localizedMessage) }
            return
        }
    }

    private fun success(message: String) {
        resultLabel.text = "✔$message"
        resultLabel.textFill = Color.GREEN
        progressIndicator.isVisible = false
        progressBox.children.remove(progressIndicator)
    }

    private fun error(message: String) {
        resultLabel.text = App.messages.getString("home.progress.error_prefix") + " $message"
        resultLabel.textFill = Color.RED
        progressIndicator.isVisible = false
        progressBox.children.remove(progressIndicator)
    }
}
