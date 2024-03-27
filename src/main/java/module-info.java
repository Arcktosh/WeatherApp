module com.example.weather {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.net.http;
    requires android.json;

    opens com.example.weather to javafx.fxml;
    exports com.example.weather;
}