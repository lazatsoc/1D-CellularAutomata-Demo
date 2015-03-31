package sample;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

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
    @FXML
    Canvas canvas;

    BooleanProperty[] ruleBinProperty = new SimpleBooleanProperty[8];
    IntegerProperty maxtime = new SimpleIntegerProperty();
    IntegerProperty calen = new SimpleIntegerProperty();
    boolean[] ca;
    Rectangle[] cells;
    int steps=0;
    BooleanProperty runningProperty = new SimpleBooleanProperty(true);


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
        RulesComboBox.getSelectionModel().select(30);
        BoundaryRulesCombo.setItems(FXCollections.observableArrayList(new String[]{"Fixed-1", "Fixed-0", "Periodic", "Repeating", "Mirroring"}));
        BoundaryRulesCombo.getSelectionModel().select(2);
        calenSlider.setMax(200); calenSlider.setMin(3); maxtimeSlider.setMax(200); maxtimeSlider.setMin(10);
        zoomSlider.setMax(3.125); zoomSlider.setMin(0.125); zoomSlider.setValue(1);
        calenSlider.setValue(100); maxtimeSlider.setValue(100);
        CreateButton.setOnAction(event -> create());
        StepButton.setOnAction(event -> step());
        RunButton.setOnAction(event -> run());
    }

    private void create() {
        canvas.setVisible(false);
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.setHeight(0);
        canvas.setWidth(0);
        TimeVBox.setVisible(true);
        steps=0;
        HBox cabox = new HBox();
        cells = new Rectangle[calen.get()];
        ca = new boolean[calen.get()];

        for (int i=0;i<cells.length;i++) {
            final int ifi=i;
            final Rectangle rect = new Rectangle(16,16);
            rect.widthProperty().bind(zoomSlider.valueProperty().multiply(16));
            rect.heightProperty().bind(zoomSlider.valueProperty().multiply(16));
            rect.setFill(Color.WHITE);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(1);
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
        runningProperty.set(false);
    }


    private void step() {
        int n = ca.length;
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (canvas.getWidth() == 0) {
            TimeVBox.setVisible(false);
            canvas.setVisible(true);
            canvas.setWidth(16 * n);
            canvas.setHeight(canvas.getHeight() + 16);
            for (int i = 0; i < n; i++) {
                gc.setFill(cells[i].getFill());
                //gc.setFill(Color.BLACK);
                gc.fillRect(16 * i, 0, 16, 16);
            }
            steps++;
        }
        canvas.setHeight(canvas.getHeight() + 16);
        runningProperty.set(true);

        Task<boolean[]> task = new Task<boolean[]>() {
            @Override
            protected boolean[] call() throws Exception {
                boolean[] newca = new boolean[n];

                for (int i = 0; i < n; i++) {
                    boolean[] value = getValues(new int[]{i - 1, i, i + 1});
                    int ruleid = boolArrToInt(value);
                    newca[i] = ruleBinProperty[ruleid].get();
                }
                return newca;
            }
        };
        task.setOnSucceeded(event -> {
            boolean[] newca = (boolean[]) event.getSource().getValue();
            for (int i = 0; i < n; i++) {
                final int finalI = i;
                ca[i] = newca[i];
                gc.setFill(newca[finalI] ? Color.BLACK : Color.WHITE);
                gc.fillRect(16 * finalI, 16 * steps, 16, 16);
            }
            steps++;
            runningProperty.set(false);
        });
        Thread t = new Thread(task);
        t.start();
    }

    private void run() {
        int n = ca.length;
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (canvas.getWidth() == 0) {
            TimeVBox.setVisible(false);
            canvas.setVisible(true);
            canvas.setWidth(16 * n);
            canvas.setHeight(canvas.getHeight() + 16);
            for (int i = 0; i < n; i++) {
                gc.setFill(cells[i].getFill());
                //gc.setFill(Color.BLACK);
                gc.fillRect(16 * i, 0, 16, 16);
            }
            steps++;
        }
        runningProperty.set(true);

        Task<boolean[][]> task = new Task<boolean[][]>() {
            @Override
            protected boolean[][] call() throws Exception {
                int maxtime = (int) maxtimeSlider.getValue();
                boolean[][] newcas = new boolean[maxtime][];
                for (int time=0;time<maxtime;time++) {
                    newcas[time]=new boolean[n];
                    for (int i = 0; i < n; i++) {
                        boolean[] value = getValues(new int[]{i - 1, i, i + 1});
                        int ruleid = boolArrToInt(value);
                        newcas[time][i] = ruleBinProperty[ruleid].get();
                    }
                    ca=newcas[time];
                }
                return newcas;
            }
        };
        task.setOnSucceeded(event -> {
            boolean[][] newcas = (boolean[][]) event.getSource().getValue();
            canvas.setHeight(canvas.getHeight() + 16*newcas.length);
            for (int time=0;time<newcas.length;time++) {
                for (int i = 0; i < newcas[0].length; i++) {
                    gc.setFill(newcas[time][i] ? Color.BLACK : Color.WHITE);
                    gc.fillRect(16 * i, 16 * steps, 16, 16);
                }
                steps++;
            }
            runningProperty.set(false);
        });
        Thread t = new Thread(task);
        t.start();
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
        Scale scale=new Scale(1,1);
        scale.xProperty().bind(zoomSlider.valueProperty());
        scale.yProperty().bind(zoomSlider.valueProperty());
        canvas.getTransforms().add(scale);
        StepButton.disableProperty().bind(runningProperty);
        RunButton.disableProperty().bind(runningProperty);
    }

    private int boolArrToInt(boolean[] a) {
        int n = 0, l = a.length;
        for (int i = 0; i < l; ++i) {
            n = (n << 1) + (a[i] ? 1 : 0);
        }
        return n;
    }
}
