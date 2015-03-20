package sample;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    ComboBox<String> RulesComboBox, BoundaryRulesCombo;
    @FXML
    VBox TimeVBox;
    @FXML
    Rectangle r0,r1,r2,r3,r4,r5,r6,r7;
    @FXML
    TextField calenTextField,maxtimeTextField;
    @FXML
    Slider calenSlider, maxtimeSlider, zoomSlider;
    @FXML
    Button CreateButton, StepButton, RunButton;
    @FXML
    ScrollPane RightScrollPane;

    BooleanProperty[] ruleBinProperty = new SimpleBooleanProperty[8];
    IntegerProperty maxtime = new SimpleIntegerProperty();
    IntegerProperty calen = new SimpleIntegerProperty();
    DoubleProperty stroke = new SimpleDoubleProperty(1);
    boolean[] ca;
    Rectangle[] cells;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (int i=0;i<8;i++) ruleBinProperty[i]=new SimpleBooleanProperty(false);
        MakeBindings();
        List<String> rulesList= new ArrayList<>(256);
        for (int i=0;i<256;i++) rulesList.add(Integer.toString(i));
        RulesComboBox.setItems(FXCollections.observableList(rulesList));
        RulesComboBox.getSelectionModel().select(0);
        RulesComboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            for (int i = 0; i < 8; i++) ruleBinProperty[i].set(false);
            char[] bin = Integer.toBinaryString(newValue.intValue()).toCharArray();
            int pos = 0;
            for (int i = bin.length - 1; i >= 0; i--) ruleBinProperty[pos++].setValue(bin[i] == '1');
        });
        BoundaryRulesCombo.setItems(FXCollections.observableArrayList(new String[]{"Fixed-1", "Fixed-0", "Periodic", "Repeating", "Mirroring"}));
        BoundaryRulesCombo.getSelectionModel().select(0);
        calenSlider.setMax(500); calenSlider.setMin(3); maxtimeSlider.setMax(1000); maxtimeSlider.setMin(10);
        zoomSlider.setMax(50); zoomSlider.setMin(2); zoomSlider.setValue(16);
        CreateButton.setOnAction(event -> create());
        StepButton.setOnAction(event -> step());
        RunButton.setOnAction(event -> run());
    }

    private void create() {
        HBox cabox = new HBox();
        cells = new Rectangle[calen.get()];
        ca = new boolean[calen.get()];

        for (int i=0;i<cells.length;i++) {
            final int ifi=i;
            final Rectangle rect = new Rectangle(16,16);
            rect.widthProperty().bind(zoomSlider.valueProperty());
            rect.heightProperty().bind(zoomSlider.valueProperty());
            stroke.set(1);
            rect.setFill(Color.WHITE);
            rect.setStroke(Color.BLACK);
            rect.strokeWidthProperty().bind(stroke);
            ca[i] = false;
            rect.fillProperty().addListener((observable, oldValue, newValue) -> {
                if (((Color) newValue).getRed() == 0) ca[ifi] = true;
                else ca[ifi] = false;
            });
            rect.setOnMouseClicked(event1 -> {
                Rectangle source = (Rectangle) event1.getSource();
                Color fill = (Color) source.getFill();
                if (fill.getRed() == 0) source.setFill(Color.WHITE);
                else source.setFill(Color.BLACK);
            });
            cells[i] = rect;
        }
        cabox.getChildren().addAll(cells);
        TimeVBox.getChildren().clear();
        TimeVBox.getChildren().add(cabox);
        StepButton.setDisable(false);
        RunButton.setDisable(false);
    }


    private void step() {
        int n = ca.length;
        boolean[] newca = new boolean[n];
        Rectangle[] newcells = new Rectangle[n];
        HBox newhbox = new HBox();
        for (int i = 0; i < n; i++) {
            boolean[] value = getValues(new int[]{i - 1, i, i + 1});
            int ruleid = boolArrToInt(value);
            newca[i] = ruleBinProperty[ruleid].get();
            newcells[i] = new Rectangle(zoomSlider.getValue(), zoomSlider.getValue(), newca[i] ? Color.BLACK : Color.WHITE);
            newcells[i].widthProperty().bind(zoomSlider.valueProperty());
            newcells[i].heightProperty().bind(zoomSlider.valueProperty());
        }
        newhbox.getChildren().addAll(newcells);
        TimeVBox.getChildren().add(newhbox);
        for (int i = 0; i < n; i++) ca[i] = newca[i];
        stroke.set(0);
    }

    private void run() {
        for (int i= ((int) maxtimeSlider.getValue());i>0;i--) {
            step();
        }
    }


    private boolean[] getValues(int[] ids) {
        boolean[] values=new boolean[3];
        values[1]=ca[ids[1]];
        if (ids[0] == -1) {
            switch (BoundaryRulesCombo.getSelectionModel().getSelectedItem()) {
                case "Fixed-1":
                    values[0]=true;
                    break;
                case "Fixed-0":
                    values[0]=false;
                    break;
                case "Periodic":
                    values[0]=ca[ca.length-1];
                    break;
                case "Mirroring":
                    values[0]=ca[ids[2]];
                    break;
                case "Repeating":
                    values[0]=ca[ids[1]];
                    break;
            }
        }else values[0]=ca[ids[0]];
        if (ids[2] == ca.length) {
            switch (BoundaryRulesCombo.getSelectionModel().getSelectedItem()) {
                case "Fixed-1":
                    values[2]=true;
                    break;
                case "Fixed-0":
                    values[2]=false;
                    break;
                case "Periodic":
                    values[2]=ca[0];
                    break;
                case "Mirroring":
                    values[2]=ca[ids[0]];
                    break;
                case "Repeating":
                    values[2]=ca[ids[1]];
                    break;
            }
        }else values[2]=ca[ids[2]];
        return values;
    }

    private void MakeBindings() {
        Rectangle[] rectArr=new Rectangle[]{r0,r1,r2,r3,r4,r5,r6,r7};
        for (int i=0;i<8;i++){
            final int ifi = i;
            rectArr[i].fillProperty().bind(Bindings.createObjectBinding(() -> ruleBinProperty[ifi].get()? Color.BLACK : Color.WHITE, ruleBinProperty[i]));
        }
        maxtimeTextField.textProperty().bind(Bindings.createStringBinding(() -> Integer.toString((int) maxtimeSlider.getValue()), maxtimeSlider.valueProperty()));
        calenTextField.textProperty().bind(Bindings.createStringBinding(()->Integer.toString((int) calenSlider.getValue()), calenSlider.valueProperty()));
        maxtime.bind(Bindings.createIntegerBinding(() -> (int) maxtimeSlider.getValue(), maxtimeSlider.valueProperty()));
        calen.bind(Bindings.createObjectBinding(() -> (int) calenSlider.getValue(), calenSlider.valueProperty()));
    }

    private int boolArrToInt(boolean[] a) {
        int n = 0, l = a.length;
        for (int i = 0; i < l; ++i) {
            n = (n << 1) + (a[i] ? 1 : 0);
        }
        return n;
    }
}
