package com.zadyraichuk.controller;

import com.zadyraichuk.App;
import com.zadyraichuk.general.MathUtils;
import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.selector.RandomSelector;
import com.zadyraichuk.selector.RationalRandomSelector;
import com.zadyraichuk.variant.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

public class SelectorUIController {

    private static final File CLICK_SOUND_FILE = new File("ui/click.wav");
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    @FXML
    private StackPane parentPane;
    @FXML
    private VBox mainPane;
    @FXML
    private VBox editPane;
    @FXML
    private AnchorPane settingsPane;
    @FXML
    private VBox wheelsPane;
    @FXML
    private TitledPane errorDialog;

    @FXML
    private TextField resultField;
    @FXML
    private Group wheelGroup;
    //todo change to button and choice in separate view
    @FXML
    private ComboBox<String> wheelComboBox;
    @FXML
    private ComboBox<Speed> speedComboBox;
    @FXML
    private CheckBox isRationalCheckBox;
    @FXML
    private ListView<HBox> editListView;
    @FXML
    private TextField editNameField;
    @FXML
    private TextField colorsTextField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveBtn;
    @FXML
    private Button createBtn;

    private final SelectorDataController selectorController;
    private final MediaPlayer player;

    private AbstractRandomSelector<String, ? extends Variant<String>> selector;
    private VariantsList<String> editableCollection;
    private Speed speed;
    private Thread rollSoundThread;
    private boolean wheelAnimating;
    private boolean isAppAlive;

    public SelectorUIController() {
        selectorController = SelectorDataController.getInstance();
        player = new MediaPlayer(new Media(CLICK_SOUND_FILE.toURI().toString()));
        wheelAnimating = false;
        speed = Speed.HIGH;
    }

    public void init() {
        String selected = null;
        if (selectorController.getCurrentSelector() != null) {
            selector = selectorController.getCurrentSelector();
            selected = selector.getName();
            renderWheel(selector);
        }

        renderSpeedSelector(speed);
        renderWheelSelector(selectorController.getVariantsListNames(), selected);
        isAppAlive = true;
    }

    @FXML
    public void onErrorOkClick() {
        hideDialog(errorDialog);
    }

    @FXML
    public void onNewClick() {
        createBtn.setVisible(true);
        saveBtn.setVisible(false);
        AbstractRandomSelector<String, ?> template = selectorController.loadSelectorTemplate();

        editNameField.setText(template.getName());
        editableCollection = template.getVariantsList().stream()
            .collect(() -> {
                    VariantsList<String> variants = new VariantsList<>();
                    AbstractVariantsList list = (AbstractVariantsList) selector.getVariantsList();
                    variants.setPalette(list.getPalette().clone());
                    return variants;
                },
                VariantsList::addColored,
                (c1, c2) -> c2.forEach(c1::add));
        colorsTextField.setText(String.valueOf(editableCollection.getPalette().getColorsCount()));

        slideUpPane(editPane);
        fillEditListView(editableCollection);
    }

    @FXML
    public void onSettingsClick() {
        slideUpPane(settingsPane);
    }

    @FXML
    public void onRollClick() {
        if (selector != null && !wheelAnimating) {
            wheelAnimating = true;
            int rotationDegree = speed.getRotationDegree();
            Transition rotateWheel = Animation.prepareRotate(wheelGroup, speed, rotationDegree);

            //todo check sound with rational list and linear interpolator
//        rotate.setInterpolator(Interpolator.LINEAR);

            rotateWheel.setOnFinished(e -> {
                selector.setCurrentRotation((int) wheelGroup.getRotate() + 90);
                int markerDegree = (selector.getCurrentRotation()) % 360;
                Variant<String> selected = selector.select(markerDegree);
                resultField.setText(selected.getValue());
                if (isRationalCheckBox.isSelected()) {
                    renderWheel(selector);
                }
                wheelAnimating = false;
            });

            int[] soundTime = getSoundTime(selector.getCurrentRotation(), rotationDegree, speed.duration);
            soundTime = interpolateSoundTime(soundTime, speed.duration, rotateWheel.getInterpolator());
            startRollSoundThread(soundTime);

            //todo add smooth animation for arcs when rational selected
            rotateWheel.play();
        }
    }

    @FXML
    public void onEditClick() {
        createBtn.setVisible(false);
        saveBtn.setVisible(true);

        editNameField.setText(selector.getName());
        editableCollection = selector.getVariantsList().stream()
            .collect(() -> {
                    VariantsList<String> variants = new VariantsList<>();
                    AbstractVariantsList list = (AbstractVariantsList) selector.getVariantsList();
                    variants.setPalette(list.getPalette().clone());
                    return variants;
                },
                VariantsList::addColored,
                (c1, c2) -> c2.forEach(c1::add));
        colorsTextField.setText(String.valueOf(editableCollection.getPalette().getColorsCount()));

        slideUpPane(editPane);
        fillEditListView(editableCollection);
    }

    @FXML
    public void onCloseEditClick() {
        slideDownPane(editPane);
    }

    @FXML
    public void onColorsUpClick() {
        int colorsCount = Integer.parseInt(colorsTextField.getText());
        int newColorsCount = Math.min(colorsCount + 1, VariantColorPalette.MAX_COLORS_COUNT);

        if (colorsCount != newColorsCount) {
            colorsTextField.setText(String.valueOf(newColorsCount));
            editableCollection.generateNewPalette(newColorsCount);
            fillEditListView(editableCollection);
        }
    }

    @FXML
    public void onColorsDownClick() {
        int colorsCount = Integer.parseInt(colorsTextField.getText());
        int newColorsCount = Math.max(colorsCount - 1, 1);

        if (colorsCount != newColorsCount) {
            colorsTextField.setText(String.valueOf(newColorsCount));
            editableCollection.generateNewPalette(newColorsCount);
            fillEditListView(editableCollection);
        }
    }

    @FXML
    public void onChangeColorsClick() {
        int colorsCount = Integer.parseInt(colorsTextField.getText());
        editableCollection.generateNewPalette(colorsCount);
        fillEditListView(editableCollection);
    }

    @FXML
    public void onNewVariantClick() {
        Variant<String> newVariant = new Variant<>("New Variant");
        editableCollection.add(newVariant);
        HBox container = getEditVariantContainer(newVariant, editListView.getItems());
        editListView.getItems().add(container);
    }

    @FXML
    public void onSaveClick() {
        String newSelectorName = editNameField.getText();
        RandomSelector newSelector = new RandomSelector(newSelectorName, editableCollection);
        selectorController.updateCurrentSelector(newSelector);
        selector = selectorController.getCurrentSelector();

        slideDownPane(editPane);
        renderWheelSelector(selectorController.getVariantsListNames(), selector.getName());
        renderWheel(selector);
    }

    @FXML
    public void onCreateClick() {
        String newSelectorName = editNameField.getText();
        RandomSelector newSelector = new RandomSelector(newSelectorName, editableCollection);
        selectorController.saveNewSelector(newSelector);
        selector = selectorController.getCurrentSelector();

        slideDownPane(editPane);
        renderWheelSelector(selectorController.getVariantsListNames(), selector.getName());
    }

    @FXML
    public void onCloseSelectClick() {

    }

    @FXML
    public void setRational() {
        boolean isRational = isRationalCheckBox.isSelected();
        if (isRational) {
            selectorController.makeCurrentSelectorRational();
        } else {
            selectorController.makeCurrentSelectorNotRational();
        }
        selector = selectorController.getCurrentSelector();
        renderWheel(selector);
    }

    @FXML
    public void onShadowClick(MouseEvent event) {
        Node clickedRect = (Node) event.getSource();
        ObservableList<Node> children = parentPane.getChildren();
        int shadowRectIndex = children.indexOf(clickedRect);

        slideDownPane((Pane) children.get(shadowRectIndex + 1));
    }

    @FXML
    public void hideWheelComboBox() {
        wheelComboBox.getStyleClass().remove("open");
        String selected = wheelComboBox.getSelectionModel().getSelectedItem();
        selectorController.setCurrentSelector(selected);
        selector = selectorController.getCurrentSelector();
        renderWheel(selector);
    }

    @FXML
    public void showWheelComboBox() {
        wheelComboBox.getStyleClass().add("open");
    }

    @FXML
    public void hideSpeedComboBox() {
        speedComboBox.getStyleClass().remove("open");
        speed = speedComboBox.getSelectionModel().getSelectedItem();
    }

    @FXML
    public void showSpeedComboBox() {
        speedComboBox.getStyleClass().add("open");
    }

    public void shutDown() throws InterruptedException, IOException {
        isAppAlive = false;
        App.PROPERTIES.setProperty("last.used.variant", selector.getName());
        App.PROPERTIES.saveProperties();
        selectorController.updateCurrentSelector(selector);

        if (rollSoundThread != null) {
            rollSoundThread.interrupt();
            rollSoundThread.join();
        }
    }

    private void renderSpeedSelector(Speed speed) {
        List<Speed> speeds = List.of(Speed.values());
        ObservableList<Speed> list = FXCollections.observableList(speeds);
        speedComboBox.getItems().clear();
        speedComboBox.getItems().addAll(list);
        speedComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Speed speed) {
                return speed.name;
            }

            @Override
            public Speed fromString(String string) {
                return Speed.valueOf(string.toUpperCase());
            }
        });

        speedComboBox.getSelectionModel().select(speed);
    }

    //todo make external thread calculation
    private void renderWheelSelector(Set<String> names, String selected) {
        ObservableList<String> list = FXCollections.observableList(new ArrayList<>(names));
        wheelComboBox.getItems().clear();
        wheelComboBox.getItems().addAll(list);

        if (selected != null) {
            wheelComboBox.getSelectionModel().select(selected);
        }
    }

    private void renderWheel(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
        isRationalCheckBox.setSelected(selector instanceof RationalRandomSelector);

        ObservableList<Node> wheelChildren = wheelGroup.getChildren();
        wheelGroup.setRotate(selector.getCurrentRotation());
        wheelChildren.clear();

        Rectangle wheelShape = new Rectangle(360, 360);
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

    //todo make external thread calculation
    private void fillEditListView(VariantsCollection<String,? extends Variant<String>> variants) {
        editListView.getItems().clear();
        editListView.setCellFactory(element -> new EditCell());
        EditCell.linkedCollection = editableCollection;

        editListView.addEventFilter(MouseEvent.ANY, event -> {
            if (event.getTarget() instanceof HBox)
                event.consume();
            else if (event.getTarget() instanceof FontIcon)
                editListView.getSelectionModel().clearSelection();
        });

        editListView.addEventFilter(KeyEvent.ANY, event -> {
            if (event.getTarget() instanceof HBox)
                event.consume();
        });

        for (Variant<String> variant : variants) {
            HBox variantContainer = getEditVariantContainer(variant, editListView.getItems());
            editListView.getItems().add(variantContainer);
        }
    }

    //todo move to special builders
    private HBox getEditVariantContainer(Variant<String> variant,
                                         ObservableList<? extends Node> container) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("edit-element");
        ObservableList<Node> children = box.getChildren();

        ColorPicker picker = getEditColorPicker(variant);
        Separator s1 = getEditSeparator(10, 0, Orientation.VERTICAL);
        TextField valueField = getEditValueTextField(variant);
        Separator s2 = getEditSeparator(5, 0, Orientation.VERTICAL);
        TextField weightField = getEditWeightTextField(variant);
        Separator s3 = getEditSeparator(5, 0, Orientation.VERTICAL);
        VBox weightButtons = getEditWeightButtons(weightField, variant);
        Separator s4 = getEditSeparator(15, 0, Orientation.VERTICAL);
        Button removeButton = getEditRemoveButton(container, variant);
        Separator s5 = getEditSeparator(15, 0, Orientation.VERTICAL);
        FontIcon dragArea = getDragIcon();

        children.add(picker);
        children.add(s1);
        children.add(valueField);
        children.add(s2);
        children.add(weightField);
        children.add(s3);
        children.add(weightButtons);
        children.add(s4);
        children.add(removeButton);
        children.add(s5);
        children.add(dragArea);

        return box;
    }

    private ColorPicker getEditColorPicker(Variant<String> variant) {
        ColorPicker picker = new ColorPicker();
        String hexColor = variant.getColor().getHexColor();
        Color color = Color.valueOf(hexColor);
        picker.setValue(color);
        picker.setStyle("-fx-background-color: " + hexColor + ";");

        picker.setOnHiding(e -> {
            Color selectedColor = picker.getValue();
            int red = (int) (selectedColor.getRed() * 255);
            int green = (int) (selectedColor.getGreen() * 255);
            int blue = (int) (selectedColor.getBlue() * 255);
            VariantColor selected = new VariantColor(red, green, blue);
            picker.setStyle("-fx-background-color: " + selected.getHexColor() + ";");
            variant.setColor(selected);
        });

        picker.getStyleClass().addAll("color-picker", "radius-100");
        return picker;
    }

    private TextField getEditValueTextField(Variant<String> variant) {
        TextField textField = new TextField(variant.getValue());
        textField.getStyleClass().addAll("text-field", "value-field");

        textField.setOnKeyReleased(event -> variant.setValue(textField.getText()));

        return textField;
    }

    private TextField getEditWeightTextField(Variant<String> variant) {
        TextField textField = new TextField(String.valueOf(variant.getVariantWeight()));
        textField.getStyleClass().addAll("text-field", "weight-field");
        textField.setOnKeyTyped(e -> {
            try {
                int weight = Integer.parseInt(textField.getText());
                variant.setVariantWeight(weight);
            } catch (NumberFormatException ex) {
                textField.setText(String.valueOf(variant.getVariantWeight()));
                errorLabel.setText("Invalid weight value");
                showDialog(errorDialog);
            }
        });

        return textField;
    }

    private VBox getEditWeightButtons(TextField weightField, Variant<String> variant) {
        VBox buttonsContainer = new VBox();
        buttonsContainer.getStyleClass().add("nav-container");
        ObservableList<Node> children = buttonsContainer.getChildren();

        Button upButton = getEditWeightUpButton(weightField, variant);
        children.add(upButton);

        Separator separator = getEditSeparator(0, 4, Orientation.HORIZONTAL);
        children.add(separator);

        Button downButton = getEditWeightDownButton(weightField, variant);
        children.add(downButton);

        return buttonsContainer;
    }

    private Button getEditWeightUpButton(TextField weightField, Variant<String> variant) {
        FontIcon icon = new FontIcon("bi-caret-up-fill");
        Button button = new Button("", icon);
        button.getStyleClass().addAll("up-btn", "btn", "orange-btn", "gray-text");

        button.setOnMouseClicked(e -> {
            try {
                int weight = Integer.parseInt(weightField.getText()) + 1;
                variant.setVariantWeight(weight);
                weightField.setText(String.valueOf(weight));
            } catch (Exception ignored) {}
        });

        return button;
    }

    private Button getEditWeightDownButton(TextField weightField, Variant<String> variant) {
        FontIcon icon = new FontIcon("bi-caret-down-fill");
        Button button = new Button("", icon);
        button.getStyleClass().addAll("up-btn", "btn", "orange-btn", "gray-text");

        button.setOnMouseClicked(e -> {
            try {
                int weight = Integer.parseInt(weightField.getText());
                int result = Math.max(1, weight - 1);
                variant.setVariantWeight(result);
                weightField.setText(String.valueOf(result));
            } catch (Exception ignored) {}
        });

        return button;
    }

    private Button getEditRemoveButton(ObservableList<? extends Node> container,
                                       Variant<String> variant) {
        FontIcon icon = new FontIcon("bi-trash-fill");
        Button button = new Button("", icon);
        button.getStyleClass().addAll("remove-btn", "btn", "transparent-btn", "orange-text");

        button.setOnMouseClicked(e -> {
            Node parent = button.getParent();
            container.remove(parent);
            editableCollection.remove(variant);
        });

        return button;
    }

    private FontIcon getDragIcon() {
        FontIcon icon = new FontIcon("bi-list");
        icon.getStyleClass().addAll("orange-text", "drag-area");
        return icon;
    }

    private Separator getEditSeparator(int width,
                                       int height,
                                       Orientation orientation) {
        Separator separator = new Separator(orientation);
        separator.setMinWidth(width);
        separator.setPrefWidth(width);
        separator.setMaxWidth(width);
        separator.setMinHeight(height);
        separator.setPrefHeight(height);
        separator.setMaxHeight(height);
        separator.getStyleClass().add("separator");

        return separator;
    }

    //todo too big
    //todo fix some delays with rational lists
    private int[] getSoundTime(int currentRotation, int degree, int duration) {
        double[] oneCycle = selector.getVariantsList().probabilities();
//        oneCycle = shiftBySelectedVariant(oneCycle);
        int cyclesCount = (int) Math.ceil((degree + currentRotation) / 360.0);
        int endDegree = (degree + currentRotation) % 360;

        double oneCyclePercent = MathUtils.cutRound(360.0 / degree, Variant.DIGITS);
        List<Double> percents = new ArrayList<>(oneCycle.length * cyclesCount);

        int firstCycleEndDegree = endDegree;
        if ((degree + currentRotation) > 360) {
            firstCycleEndDegree = 360;
        }
        List<Double> firstCycle = wheelPartProbabilities(oneCycle, currentRotation, firstCycleEndDegree);
        firstCycle = firstCycle.stream()
            .map(e -> e * oneCyclePercent)
            .map(e -> MathUtils.cutRound(e, Variant.DIGITS))
            .collect(Collectors.toList());
        percents.addAll(firstCycle);

        for (int i = 1; i < cyclesCount - 1; i++) {
            List<Double> cycle = wheelPartProbabilities(oneCycle, 0, 360).stream()
                .map(e -> e * oneCyclePercent)
                .map(e -> MathUtils.cutRound(e, Variant.DIGITS))
                .collect(Collectors.toList());
            percents.addAll(cycle);
        }

        if ((degree + currentRotation) > 360) {
            List<Double> lastCycle = wheelPartProbabilities(oneCycle, 0, endDegree);
            lastCycle = lastCycle.stream()
                .map(e -> e * oneCyclePercent)
                .map(e -> MathUtils.cutRound(e, Variant.DIGITS))
                .collect(Collectors.toList());
            percents.addAll(lastCycle);
        }

        return percents.stream()
            .mapToInt(e -> (int) (e * duration))
            .toArray();
    }

    //todo a little big
    private int[] interpolateSoundTime(int[] soundTime, int duration, Interpolator interpolator) {
        //todo not enough curved function, make custom interpolator
        // or make some operations with interpolated difference that add/subtract to soundTime
//        Interpolator interpolator1 = new Interpolator() {
//            @Override
//            protected double curve(double t) {
//                return 0;
//            }
//        };
        double[] fractions = new double[soundTime.length];
        int[] result = new int[soundTime.length];

        for (int i = 0; i < soundTime.length; i++) {
            fractions[i] = (double) soundTime[i] / duration;

            if (i > 0)
                fractions[i] += fractions[i - 1];
        }

        for (int i = 0; i < soundTime.length; i++) {
            result[i] = interpolator.interpolate(0, duration, fractions[i]);
        }
        for (int i = result.length - 1; i > 0; i--) {
            result[i] -= result[i - 1];
        }
        for (int i = 0; i < result.length; i++) {
            if (result[i] > soundTime[i])
                result[i] = soundTime[i] - (result[i] - soundTime[i]);
            else if (result[i] < soundTime[i])
                result[i] = soundTime[i] + (soundTime[i] - result[i]);
        }

        return result;
    }

    //todo a little big
    private List<Double> wheelPartProbabilities(double[] oneCycle, int startDegree, int endDegree) {
        double startPercent = 0;
        double endPercent = 1;
        List<Double> result = new ArrayList<>(oneCycle.length);

        if (startDegree > 0) {
            startPercent = MathUtils.cutRound(startDegree / 360.0, Variant.DIGITS);
        }
        if (endDegree < 360) {
            endPercent = MathUtils.cutRound(endDegree / 360.0, Variant.DIGITS);
        }

        double percentsSum = 0;
        double previousPercent = startPercent;
        for (double percent : oneCycle) {
            percentsSum += percent;
            if (percentsSum > startPercent && percentsSum <= endPercent) {
                if (percentsSum > endPercent) {
                    result.add(endPercent - previousPercent);
                    break;
                } else {
                    result.add(percentsSum - previousPercent);
                    previousPercent = percentsSum;
                }
            }
        }

        return result;
    }

    private void startRollSoundThread(int[] soundTime) {
        rollSoundThread = new Thread(() -> {
            try {
                for (int time : soundTime) {
                    Thread.sleep(time);
                    if (isAppAlive) {
                        player.seek(Duration.ZERO);
                        player.play();
                    } else {
                        player.stop();
                    }
                }
            } catch (InterruptedException ignored) {}
        });
        rollSoundThread.start();
    }

    private void slideUpPane(Pane pane) {
        Node shadowRectangle = getShadowRectangle(pane);

        Transition fadeRect = Animation.prepareShowFade(shadowRectangle);
        Transition translatePane = Animation.prepareShowTranslate(pane, mainPane);

        fadeRect.play();
        translatePane.play();
    }

    private void slideDownPane(Pane pane) {
        Node shadowRectangle = getShadowRectangle(pane);

        Transition fadeRect = Animation.prepareHideFade(shadowRectangle);
        Transition translatePane = Animation.prepareHideTranslate(pane, mainPane);

        fadeRect.play();
        translatePane.play();
    }

    private void showDialog(TitledPane dialog) {
        Node shadowRectangle = getShadowRectangle(dialog);

        Transition fadeRect = Animation.prepareShowFade(shadowRectangle);
        Transition fadeDialog = Animation.prepareShowFade(dialog, 1);

        fadeRect.play();
        fadeDialog.play();
    }

    private void hideDialog(TitledPane dialog) {
        Node shadowRectangle = getShadowRectangle(dialog);

        Transition fadeRect = Animation.prepareHideFade(shadowRectangle);
        Transition fadeDialog = Animation.prepareHideFade(dialog);

        fadeRect.play();
        fadeDialog.play();
    }

    private Node getShadowRectangle(Node node) {
        ObservableList<Node> children = parentPane.getChildren();
        int nodeIndex = children.indexOf(node);
        return children.get(nodeIndex - 1);
    }

    private static class Animation {

        private static final double MAX_SHADOW_OPACITY = 0.4;
        private static final int ANIMATION_DURATION = 200;

        public static Transition prepareRotate(Node node, Speed speed, int degree) {
            RotateTransition rotate = new RotateTransition();
            rotate.setAxis(Rotate.Z_AXIS);
            rotate.setDuration(Duration.millis(speed.duration));
            rotate.setByAngle(degree);
            rotate.setNode(node);

            return rotate;
        }

        public static Transition prepareShowFade(Node node) {
            node.setOpacity(0);
            node.setVisible(true);
            return prepareFadeTransition(node, Animation.MAX_SHADOW_OPACITY);
        }

        public static Transition prepareShowFade(Node node, double opacity) {
            node.setOpacity(0);
            node.setVisible(true);
            return prepareFadeTransition(node, opacity);
        }

        public static Transition prepareHideFade(Node node) {
            Transition fade = prepareFadeTransition(node, 0);
            fade.setOnFinished(e -> node.setVisible(false));
            return fade;
        }

        public static Transition prepareShowTranslate(Node node, Node parent) {
            node.setVisible(true);
            double yPos = parent.getLayoutBounds().getHeight() - node.getLayoutBounds().getHeight();
            return prepareTranslateTransition(node, yPos);
        }

        public static Transition prepareHideTranslate(Node node, Node parent) {
            double yPos = parent.getLayoutBounds().getHeight();
            Transition translate = prepareTranslateTransition(node, yPos);
            translate.setOnFinished(e -> node.setVisible(false));
            return translate;
        }

        private static Transition prepareFadeTransition(Node node, double target) {
            FadeTransition opacityTransition = new FadeTransition();
            opacityTransition.setDuration(new Duration(ANIMATION_DURATION));
            opacityTransition.setToValue(target);
            opacityTransition.setNode(node);
            return opacityTransition;
        }

        private static Transition prepareTranslateTransition(Node node, double target) {
            TranslateTransition translateTransition = new TranslateTransition();
            translateTransition.setDuration(new Duration(ANIMATION_DURATION));
            translateTransition.setToY(target);
            translateTransition.setNode(node);
            return translateTransition;
        }
    }

    private static class EditCell extends ListCell<HBox> {

        private static HBox temp;
        public static AbstractVariantsList<String, Variant<String>> linkedCollection;

        public EditCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            setOnDragDetected(event -> {
                if (getItem() == null) {
                    return;
                }

                if (event.getTarget() instanceof FontIcon &&
                    ((FontIcon) event.getTarget()).getIconLiteral().equals("bi-list")) {
                    Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(getItem().toString());
                    dragboard.setContent(content);

                    temp = getItem();
                    event.consume();
                }

            });

            setOnDragOver(event -> {
                if (event.getGestureSource() != this &&
                    temp != null) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }

                event.consume();
            });

            setOnDragEntered(event -> {
                if (event.getGestureSource() != this &&
                    temp != null) {
                    setOpacity(0.3);
                }
            });

            setOnDragExited(event -> {
                if (event.getGestureSource() != this &&
                    temp != null) {
                    setOpacity(1);
                }
            });

            setOnDragDropped(event -> {
                if (getItem() == null) {
                    return;
                }

                boolean success = false;

                if (temp != null) {
                    ObservableList<HBox> items = getListView().getItems();
                    int draggedIdx = items.indexOf(temp);
                    int thisIdx = items.indexOf(getItem());

                    items.set(draggedIdx, getItem());
                    items.set(thisIdx, temp);
                    linkedCollection.swap(draggedIdx, thisIdx);

                    List<HBox> itemsCopy = new ArrayList<>(getListView().getItems());
                    getListView().getItems().setAll(itemsCopy);

                    success = true;
                }
                event.setDropCompleted(success);

                event.consume();
            });

            setOnDragDone(event -> {
                temp = null;
                event.consume();
            });


        }

        @Override
        protected void updateItem(HBox item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
            } else {
                setGraphic(item);
            }
        }
    }

    //todo make speed relative to each Selector
    private enum Speed {
        LOW("Low", 3000) {
            @Override
            public int getRotationDegree() {
                return RANDOM.nextInt(360) + 360 * 4;
            }
        },
        MEDIUM("Medium", 2000) {
            @Override
            public int getRotationDegree() {
                return RANDOM.nextInt(360) + 360 * 3;
            }
        },
        HIGH("High", 1000) {
            @Override
            public int getRotationDegree() {
                return RANDOM.nextInt(360) + 360 * 2;
            }
//        },
//        TEST("Test", 10000) {
//            @Override
//            public int getRotationDegree() {
//                return RANDOM.nextInt(360) + 360 * 3;
//            }
        };

        private final String name;
        private final int duration;

        Speed(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }

        public abstract int getRotationDegree();
    }
}
