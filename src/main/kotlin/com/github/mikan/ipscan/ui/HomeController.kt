package com.github.mikan.ipscan.ui

import com.github.mikan.ipscan.Version
import com.github.mikan.ipscan.domain.NetworkAdapter
import com.github.mikan.ipscan.domain.NetworkCommand
import com.github.mikan.ipscan.service.NetworkAdapterService
import com.github.mikan.ipscan.service.ValidationService
import com.neovisionaries.oui.Oui
import com.neovisionaries.oui.OuiCsvParser
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextInputDialog
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import java.io.InputStream
import java.net.URL
import java.util.Collections
import java.util.ResourceBundle
import java.util.Scanner
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class HomeController : Initializable {
    private val nThreads = 255
    private val pingTimeoutSec = 1
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    private val adapters: ObservableList<NetworkAdapter> = FXCollections.observableArrayList()
    private val foundList: ObservableList<ScanResult> = FXCollections.observableArrayList()
    private val scanResultComparator = ScanResultComparator()
    private val scanning = AtomicBoolean()
    private lateinit var oui: Oui

    @FXML
    private lateinit var networkAdapterChoiceBox: ChoiceBox<NetworkAdapter>

    @FXML
    private lateinit var startScanButton: Button

    @FXML
    private lateinit var stopScanButton: Button

    @FXML
    private lateinit var scanResultTable: TableView<ScanResult>

    @FXML
    private lateinit var ipColumn: TableColumn<ScanResult, String>

    @FXML
    private lateinit var macColumn: TableColumn<ScanResult, String>

    @FXML
    private lateinit var vendorColumn: TableColumn<ScanResult, String>

    @FXML
    private lateinit var progressBox: HBox

    @FXML
    private lateinit var progressIndicator: ProgressIndicator

    @FXML
    private lateinit var resultLabel: Label

    @FXML
    private lateinit var globalIpLabel: Label

    @FXML
    private lateinit var networkAdaptersLabel: Label

    @FXML
    private lateinit var utilityRefreshButton: Button

    data class ScanResult(val ip: String, val mac: String, val vendor: String)

    private class ScanResultComparator : Comparator<ScanResult> {
        override fun compare(o1: ScanResult, o2: ScanResult): Int {
            val o1a = o1.ip.split(".").map { it.toInt() }.toIntArray()
            val o2a = o2.ip.split(".").map { it.toInt() }.toIntArray()
            var r = 0
            var i = 0
            while (i < o1a.size && i < o2a.size) {
                r = o1a[i].compareTo(o2a[i])
                if (r != 0) {
                    return r
                }
                i++
            }
            return r
        }
    }

    @Override
    override fun initialize(url: URL, rb: ResourceBundle?) {
        networkAdapterChoiceBox.itemsProperty().value = adapters
        scanResultTable.itemsProperty().value = foundList
        ipColumn.cellValueFactory = PropertyValueFactory("ip")
        macColumn.cellValueFactory = PropertyValueFactory("mac")
        vendorColumn.cellValueFactory = PropertyValueFactory("vendor")
        progressBox.children.remove(progressIndicator)

        adapters.addAll(NetworkAdapterService.listNetworkAdapters())
        if (!adapters.isEmpty()) {
            networkAdapterChoiceBox.value = adapters[0]
        }
        startScanButton.isDisable = adapters.isEmpty()
        stopScanButton.isDisable = true

        updateUtility(App.messages.getString("home.label.click_refresh"), adapters)

        val csv = this.javaClass.classLoader.getResource("oui.csv")
            ?: throw InternalError("oui.csv unavailable")
        oui = Oui(OuiCsvParser().parse(csv))
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleMenuExit(event: ActionEvent) {
        Platform.exit()
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleMenuVersion(event: ActionEvent) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = App.messages.getString("app.name")
        alert.headerText = App.messages.getString("home.label.about")
        alert.contentText =
            """
            ${alert.title} v${Version.get()}
            (JRE v${System.getProperty("java.version")} / Kotlin v${KotlinVersion.CURRENT})
            
            ${App.messages.getString("app.copyright")}
            """.trimIndent()
        alert.showAndWait()
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleStartScan(event: ActionEvent) {
        if (scanning.get()) {
            scanError("Too short operation")
            return
        }
        startScanButton.isDisable = true
        stopScanButton.isDisable = false
        resultLabel.textFill = Color.BLACK
        resultLabel.text = App.messages.getString("home.progress.scanning")
        progressBox.children.add(0, progressIndicator)
        foundList.clear()
        val adapter = networkAdapterChoiceBox.value
        val stop = AtomicBoolean()
        threadPool = Executors.newFixedThreadPool(nThreads)
        scanning.set(true)
        adapter.ipV4[0].subnetIps().forEach {
            if (stop.get()) {
                return@forEach
            }
            if (it == adapter.ipV4[0].address) {
                val label = App.messages.getString("home.label.this_pc")
                foundList.add(ScanResult(it, label, label))
                Collections.sort(foundList, scanResultComparator)
                return@forEach
            }
            threadPool.submit {
                try {
                    if (NetworkCommand.execPing(it, 1, pingTimeoutSec)) {
                        found(it)
                    }
                } catch (e: Throwable) {
                    stop.set(true)
                }
            }
        }
        threadPool.shutdown()
        Thread {
            try {
                threadPool.awaitTermination(3, TimeUnit.MINUTES)
                scanning.set(false)
                Platform.runLater {
                    startScanButton.isDisable = false
                    stopScanButton.isDisable = true
                    if (stop.get()) {
                        scanTerminated()
                    } else {
                        scanSuccess()
                    }
                }
            } catch (e: InterruptedException) {
                Platform.runLater { scanError(e.localizedMessage) }
            }
        }.start()
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleStopScan(event: ActionEvent) {
        resultLabel.text = App.messages.getString("home.progress.stopping")
        threadPool.shutdownNow()
        stopScanButton.isDisable = true
    }

    private fun found(ip: String) {
        val p = Runtime.getRuntime().exec(NetworkCommand.encodeArpCommand(ip))
        val result = readAll(p.inputStream)
        p.waitFor()
        val mac = ValidationService.extractMacAddress(result.lowercase().replace("-", ":"))
        val vendor = if (mac.isEmpty()) "" else oui.getName(mac) ?: ""
        Platform.runLater {
            foundList.add(ScanResult(ip, mac, vendor))
            Collections.sort(foundList, scanResultComparator)
        }
    }

    private fun scanSuccess() {
        resultLabel.text = "✔" + App.messages.getString("home.progress.complete")
        resultLabel.textFill = Color.GREEN
        progressBox.children.remove(progressIndicator)
    }

    private fun scanError(message: String) {
        resultLabel.text = App.messages.getString("home.progress.error_prefix") + " " + message
        resultLabel.textFill = Color.RED
        progressBox.children.remove(progressIndicator)
    }

    private fun scanTerminated() {
        resultLabel.text = App.messages.getString("home.progress.terminated")
        resultLabel.textFill = Color.BLACK
        progressBox.children.remove(progressIndicator)
    }

    private fun readAll(inputStream: InputStream): String {
        var result = ""
        Scanner(inputStream).use { scan ->
            while (scan.hasNextLine()) {
                result += scan.nextLine() + "\n"
            }
        }
        return result
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleUtilityRefresh(event: ActionEvent) {
        utilityRefreshButton.isDisable = true
        Thread {
            try {
                val globalIp = NetworkAdapterService.globalIp()
                val adapters = NetworkAdapterService.listNetworkAdapters()
                Platform.runLater {
                    updateUtility(globalIp, adapters)
                }
            } catch (e: Throwable) {
                Platform.runLater {
                    App.errorDialog(App.messages.getString("home.dialog.error.refresh") + e.localizedMessage)
                }
            } finally {
                Platform.runLater { utilityRefreshButton.isDisable = false }
            }
        }.start()
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handlePing(event: ActionEvent) {
        val dialog = TextInputDialog("")
        dialog.title = App.messages.getString("home.button.ping")
        dialog.headerText = dialog.title
        dialog.contentText = App.messages.getString("command.label.ping.target")
        val ipAddress = dialog.showAndWait()
        if (!ipAddress.isPresent) {
            return
        }
        App.startCommandStage(NetworkCommand.PING, ipAddress.get())
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleArp(event: ActionEvent) {
        val adapters = NetworkAdapterService.listNetworkAdapters()
        when (adapters.size) {
            0 -> App.errorDialog(App.messages.getString("command.dialog.error.adapter"))
            1 -> App.startCommandStage(NetworkCommand.ARP, adapters[0].ipV4[0].address, adapters[0].name)
            else -> {
                val choice = adapters.map { it.ipV4[0].address }
                val dialog = ChoiceDialog(choice[0], choice)
                dialog.title = App.messages.getString("home.button.arp")
                dialog.headerText = dialog.title
                dialog.contentText = App.messages.getString("home.choice.adapter")
                val ipAddress = dialog.showAndWait()
                if (!ipAddress.isPresent) {
                    return
                }
                val ifName = adapters.first { it.ipV4[0].address == ipAddress.get() }.name
                App.startCommandStage(NetworkCommand.ARP, ipAddress.get(), ifName)
            }
        }
    }

    @FXML
    @Suppress("UNUSED_PARAMETER")
    fun handleTraceRoute(event: ActionEvent) {
        val dialog = TextInputDialog("")
        dialog.title = App.messages.getString("home.button.traceroute")
        dialog.headerText = dialog.title
        dialog.contentText = App.messages.getString("command.label.traceroute.target")
        val target = dialog.showAndWait()
        if (!target.isPresent || target.isEmpty) {
            return
        }
        App.startCommandStage(NetworkCommand.TRACEROUTE, target.get())
    }

    private fun updateUtility(globalIp: String, adapters: List<NetworkAdapter>) {
        if (globalIp.isEmpty()) {
            globalIpLabel.text = App.messages.getString("home.label.unknown")
        } else {
            val hostname = NetworkAdapterService.hostFromIp(globalIp)
            if (hostname.isEmpty()) {
                globalIpLabel.text = globalIp
            } else {
                globalIpLabel.text = "$globalIp ($hostname)"
            }
        }
        if (adapters.isEmpty()) {
            networkAdaptersLabel.text = App.messages.getString("command.label.not_detected")
        } else {
            var adaptersText = ""
            adapters.forEach {
                adaptersText += "${it.name} (${it.displayName})\n"
                it.ipV4.forEach { ip ->
                    val prefix = App.messages.getString("command.label.address_prefix")
                    adaptersText += "• $prefix ${ip.address} /${ip.length}\n"
                }
            }
            networkAdaptersLabel.text = adaptersText
        }
    }
}
