package com.openfin.desktop.demo;

import com.openfin.desktop.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LaunchManifestDemo extends Application {

    private final static Logger logger = LoggerFactory.getLogger(LaunchManifestDemo.class.getName());
    private final static String WINDOW_TITLE = "Launch Manifest Demo";

    private DesktopConnection desktopConnectionGM;  // connection to Watchlist by Giant Machines
    private DesktopConnection desktopConnectionSL;  // connection to StockFlux by Scott Logic
    private Button btnGiantMachine, btnScottLogic;


    @Override
    public void start(Stage stage) {
        btnGiantMachine = new Button();
        btnGiantMachine.setText("Launch Watchlist by Giant Machines");
        btnGiantMachine.setLayoutX(10);
        btnGiantMachine.setLayoutY(10);
        btnGiantMachine.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchGiantMachine();
            }
        });

        btnScottLogic = new Button();
        btnScottLogic.setText("Launch StockFlux by Scott Logic");
        btnScottLogic.setLayoutX(10);
        btnScottLogic.setLayoutY(50);
        btnScottLogic.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                launchScottLogic();
            }
        });

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.getChildren().add(btnGiantMachine);
        anchorPane.getChildren().add(btnScottLogic);

        //Creating a Group object
        Group root = new Group();

        //Retrieving the observable list object
        ObservableList list = root.getChildren();

        //Creating a scene object
        Scene scene = new Scene(anchorPane, 800, 800);

        //Setting title to the Stage
        stage.setTitle(WINDOW_TITLE);

        //Adding scene to the stage
        stage.setScene(scene);

        //Displaying the contents of the stage
        stage.show();

    }

    private void launchGiantMachine() {
        if (desktopConnectionGM == null) {
            RuntimeConfiguration cfg = new RuntimeConfiguration();
            cfg.setManifestLocation("https://openfin.giantmachines.com/public/app.json");
            try {
                desktopConnectionGM = new DesktopConnection("Watchlist Java");
                desktopConnectionGM.connect(cfg, new DesktopStateListener() {
                    @Override
                    public void onReady() {
                        logger.info("Connected to OpenFin Runtime hosting Watchlist by Giant Machines");
                    }

                    @Override
                    public void onClose(String error) {
                    }

                    @Override
                    public void onError(String reason) {
                    }

                    @Override
                    public void onMessage(String message) {
                    }

                    @Override
                    public void onOutgoingMessage(String message) {
                    }
                }, 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void launchScottLogic() {
        if (desktopConnectionSL == null) {
            RuntimeConfiguration cfg = new RuntimeConfiguration();
            cfg.setManifestLocation("http://scottlogic.github.io/StockFlux/master/app.json");
            try {
                desktopConnectionSL = new DesktopConnection("StockFlux Java");
                desktopConnectionSL.connect(cfg, new DesktopStateListener() {
                    @Override
                    public void onReady() {
                        logger.info("Connected to OpenFin Runtime hosting StockFlux by Scott Logic");
                    }

                    @Override
                    public void onClose(String error) {
                    }

                    @Override
                    public void onError(String reason) {
                    }

                    @Override
                    public void onMessage(String message) {
                    }

                    @Override
                    public void onOutgoingMessage(String message) {
                    }
                }, 60);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        launch(args);
    }



}
