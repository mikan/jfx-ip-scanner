module com.github.mikan.ipscan {
    requires java.desktop;
    requires java.net.http;
    requires jdk.crypto.cryptoki;
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires nv.oui;
    exports com.github.mikan.ipscan.ui to javafx.graphics, javafx.fxml;
    opens com.github.mikan.ipscan.ui to javafx.base, javafx.fxml;
}
