package com.zadyraichuk.controller;

import com.zadyraichuk.general.PropertiesFile;
import com.zadyraichuk.selector.AbstractRandomSelector;
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

public class SelectorUIController {

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

    public void init() {
        AbstractRandomSelector<String, ? extends Variant<String>> currentSelector =
            SelectorDataController.getInstance().getCurrentSelector();
        if (currentSelector != null) {
            renderWheelSelector(SelectorDataController.getInstance().getVariantsListNames(),
                currentSelector.getName());
            renderWheel(currentSelector);
        }
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

    private void renderWheelSelector(Set<String> names, String selected) {
        ObservableList<String> list = FXCollections.observableList(new ArrayList<>(names));
        wheelSelector.getItems().clear();
        wheelSelector.getItems().addAll(list);

        wheelSelector.getSelectionModel().select(selected);
    }

    private void renderWheel(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
        ObservableList<Node> wheelChildren = wheelContainer.getChildren();
        wheelChildren.clear();

        Rectangle wheelShape = new Rectangle(360, 360);
        wheelShape.setFill(Color.BLACK);
        wheelShape.setVisible(false);
        wheelChildren.add(wheelShape);

        generateWheelElements(wheelChildren, selector.getVariantsList().iterator());
    }

    private void generateWheelElements(ObservableList<Node> parent, Iterator<? extends Variant<String>> iterator) {
        double currentRotate = 0;
        double usedDegreesSum = 0;
        boolean hasNext = iterator.hasNext();
        Variant<String> variant;

        while (hasNext) {
            variant = iterator.next();
            double arcLength = variant.getCurrentPercent() * 360;

            Arc arc = new Arc(0, 0, 180, 180, currentRotate, arcLength);
            arc.setLayoutX(180);
            arc.setLayoutY(180);
            arc.setFill(Paint.valueOf(variant.getColor().getHexColor()));
            arc.setStroke(Color.valueOf("#b5b5b5"));
            arc.setStrokeType(StrokeType.INSIDE);
            arc.setType(ArcType.ROUND);
            double labelRotation = (currentRotate + arcLength / 2) % 360 * (-1);

            currentRotate = (currentRotate + arcLength) % 360;
            usedDegreesSum += arcLength;
            hasNext = iterator.hasNext();

            if (!hasNext) {
                double fullCircleDiff = 360 - usedDegreesSum;
                arc.setLength(arcLength + fullCircleDiff);
            }

            parent.add(arc);
            Label label = generateLabel(variant.getValue(), labelRotation);
            parent.add(label);
        }
    }

    private Label generateLabel(String value, double rotation) {
        Label label = new Label(value);
        label.setTranslateX(180);
        label.setTranslateY(165);
        label.setMaxWidth(175);
        label.setMinWidth(175);
        label.setPadding(new Insets(0, 0, 0, 50));
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("wheel-label");

        Rotate rotate = Rotate.rotate(rotation, 0, 15);
        label.getTransforms().add(rotate);
        return label;
    }

}
