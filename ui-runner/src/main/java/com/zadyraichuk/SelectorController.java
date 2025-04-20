package com.zadyraichuk;

import com.zadyraichuk.general.PropertiesFile;
import com.zadyraichuk.variant.Variant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;

public class SelectorController {

    @FXML
    private VBox mainPanel;
    @FXML
    private VBox editPanel;
    @FXML
    private VBox wheelsPanel;

    @FXML
    private TextField resultField;
    @FXML
    private Group wheelContainer;
    @FXML
    private ChoiceBox<String> wheelSelector;

    private PropertiesFile properties;

    public void init(PropertiesFile properties) {
        this.properties = properties;

        String selectedCollection = properties.getProperty("last.used.variants");
        renderWheelSelector(SelectorLogic.getInstance().getVariantsListNames());
        renderWheel(SelectorLogic.getInstance().getVariantsList(selectedCollection));

    }

    @FXML
    public void onNewClick() {
        
    }

    @FXML
    public void onRollClick() {

    }

    @FXML
    public void onEditClick() {

    }

    @FXML
    public void onCloseEditClick() {
    }

    @FXML
    public void onNewVariantClick() {

    }

    @FXML
    public void onSaveClick() {

    }

    @FXML
    public void onCloseSelectClick() {

    }

    private void renderWheelSelector(Set<String> names) {
        ObservableList<String> list = FXCollections.observableList(new ArrayList<>(names));
        wheelSelector.getItems().clear();
        wheelSelector.getItems().addAll(list);

        String selected = properties.getProperty("last.used.variants");
        wheelSelector.getSelectionModel().select(selected);
    }

    private void renderWheel(ColoredVariantsList variants) {
        ObservableList<Node> wheelChildren = wheelContainer.getChildren();
        wheelChildren.clear();

        Rectangle wheelShape = new Rectangle(360, 360);
        wheelShape.setFill(Color.BLACK);
        wheelShape.setVisible(false);
        wheelChildren.add(wheelShape);

        generateWheelElements(wheelChildren, variants.iterator());
    }

    private void generateWheelElements(ObservableList<Node> parent, Iterator<Variant> iterator) {
        double currentRotate = 90;
        double usedDegreesSum = 0;
        boolean hasNext = iterator.hasNext();
        ColoredRationalVariant variant;
        
        while (hasNext) {
            variant = (ColoredRationalVariant) iterator.next();
            double arcLength = variant.getCurrentPercent() * 360;

            Arc arc = new Arc(0, 0, 180, 180, currentRotate, arcLength);
            arc.setLayoutX(180);
            arc.setLayoutY(180);
            arc.setFill(Paint.valueOf(variant.getColor().getHexColor()));
            arc.setStroke(Color.valueOf("#b5b5b5"));
            arc.setStrokeType(StrokeType.INSIDE);
            arc.setType(ArcType.ROUND);
            double labelRotation = (currentRotate + arcLength / 2) % 360;

            currentRotate = (currentRotate + arcLength) % 360;
            usedDegreesSum += arcLength;
            hasNext = iterator.hasNext();

            if (!hasNext) {
                double fullCircleDiff = 360 - usedDegreesSum;
                arc.setLength(arcLength + fullCircleDiff);
            }

            parent.add(arc);
//            Label label = generateLabel(variant.getValue(), labelRotation);
//            parent.add(label);
        }
    }

    private Label generateLabel(String value, double rotation) {
        Label label = new Label(value);
        label.setTranslateX(180);
        label.setTranslateY(164);
        label.setMaxWidth(170);
        label.setMinWidth(170);
        label.setPadding(new Insets(0, 0, 0, 50));
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("wheel-label");

        Rotate rotate = Rotate.rotate(rotation, 0, 24);
        label.getTransforms().add(rotate);
        return label;
    }

}
