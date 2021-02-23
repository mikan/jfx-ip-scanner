package com.github.mikan.ipscan.ui

import com.github.mikan.ipscan.Version
import com.github.mikan.ipscan.domain.NetworkCommand
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.stage.Stage

class App : Application() {
    override fun start(stage: Stage) {
        stage.title = "JFX IP Scanner v" + Version.get()
        stage.scene = Scene(FXMLLoader.load(this::class.java.getResource("/fxml/home.fxml")))
        stage.show()
    }

    companion object {
        fun errorDialog(contentText: String) {
            val alert = Alert(Alert.AlertType.ERROR)
            alert.contentText = contentText
            alert.showAndWait()
        }

        fun startCommandStage(command: NetworkCommand, vararg params: String) {
            val loader = FXMLLoader(this::class.java.getResource("/fxml/command.fxml"))
            val stage = Stage()
            stage.title = "JFX IP Scanner"
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
