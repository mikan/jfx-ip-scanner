package com.github.mikan.ipscan.ui

import com.github.mikan.ipscan.Version
import com.github.mikan.ipscan.domain.NetworkCommand
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.stage.Stage
import java.util.ResourceBundle

class App : Application() {
    override fun start(stage: Stage) {
        stage.title = messages.getString("app.name") + " v" + Version.get()
        stage.scene = Scene(FXMLLoader.load(this::class.java.getResource("/fxml/home.fxml"), messages))
        stage.show()
    }

    companion object {
        internal val messages: ResourceBundle = ResourceBundle.getBundle("messages")

        fun errorDialog(contentText: String) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.contentText = contentText
            alert.showAndWait()
        }

        fun startCommandStage(command: NetworkCommand, vararg params: String) {
            val loader = FXMLLoader(this::class.java.getResource("/fxml/command.fxml"), messages)
            val stage = Stage()
            stage.title = messages.getString("app.name")
            stage.scene = Scene(loader.load())
            val controller = loader.getController<CommandController>()
            controller.okButton.setOnAction {
                controller.stopCommand()
                stage.close()
            }
            controller.startCommand(command, *params)
            stage.show()
        }
    }
}
