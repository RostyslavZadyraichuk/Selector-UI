package com.zadyraichuk.controller;

import com.zadyraichuk.general.MathUtils;
import com.zadyraichuk.general.PropertiesFile;
import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.selector.RationalRandomSelector;
import com.zadyraichuk.variant.Variant;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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

public class SelectorUIController {

    private static final File CLICK_SOUND_FILE = new File("src/main/resources/selector/ui/click.wav");
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final double MAX_SHADOW_OPACITY = 0.4;
    private static final int ANIMATION_DURATION = 200;

    @FXML
    private Pane parentPane;
    @FXML
    private VBox mainPane;
    @FXML
    private VBox editPane;
    @FXML
    private VBox settingsPane;
    @FXML
    private VBox wheelsPane;

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

    private final SelectorDataController selectorController;
    private final MediaPlayer player;

    private AbstractRandomSelector<String, ? extends Variant<String>> selector;
    private Speed speed;
    private boolean wheelAnimating;

    public SelectorUIController() {
        selectorController = SelectorDataController.getInstance();
        player = new MediaPlayer(new Media(CLICK_SOUND_FILE.toURI().toString()));
        wheelAnimating = false;
        speed = Speed.HIGH;
    }

    public void init() {
        if (selectorController.getCurrentSelector() != null) {
            selector = selectorController.getCurrentSelector();
            renderSpeedSelector(speed);
            renderWheelSelector(selectorController.getVariantsListNames(), selector.getName());
            renderWheel(selector);
        }
    }

    @FXML
    public void onNewClick() {

    }

    @FXML
    public void onSettingsClick() {
        showPane(settingsPane);
    }

    @FXML
    public void onRollClick() {
        if (selector != null && !wheelAnimating) {
            wheelAnimating = true;
            RotateTransition rotate = getRotateTransition(speed.getRotationDegree());

            int[] soundTime = getSoundTime(selector.getCurrentRotation(), speed);
            soundTime = interpolateSoundTime(soundTime, speed, rotate.getInterpolator());
            startRollSoundThread(soundTime);

            rotate.play();
        }
    }

    @FXML
    public void onEditClick() {
        showPane(editPane);
    }

    @FXML
    public void onCloseEditClick() {
        hidePane(editPane);
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

        hidePane((Pane) children.get(shadowRectIndex + 1));
    }

    @FXML
    public void hideWheelComboBox() {
        wheelComboBox.getStyleClass().remove("open");
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

    private void renderSpeedSelector(Speed speed) {
        List<Speed> speeds = List.of(Speed.LOW, Speed.MEDIUM, Speed.HIGH);
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

    private void renderWheelSelector(Set<String> names, String selected) {
        ObservableList<String> list = FXCollections.observableList(new ArrayList<>(names));
        wheelComboBox.getItems().clear();
        wheelComboBox.getItems().addAll(list);

        wheelComboBox.getSelectionModel().select(selected);
    }

    private void renderWheel(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
        isRationalCheckBox.setSelected(selector instanceof RationalRandomSelector);

        ObservableList<Node> wheelChildren = wheelGroup.getChildren();
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

    private RotateTransition getRotateTransition(int degree) {
        RotateTransition rotate = new RotateTransition();
        rotate.setAxis(Rotate.Z_AXIS);
        rotate.setDuration(Duration.millis(speed.duration));
        rotate.setByAngle(degree);
        rotate.setNode(wheelGroup);

        //todo check sound with rational list and linear interpolator
        rotate.setInterpolator(Interpolator.LINEAR);

        rotate.setOnFinished(e -> {
            selector.setCurrentRotation((int) wheelGroup.getRotate());
            wheelGroup.setRotate(selector.getCurrentRotation());
            int markerDegree = (selector.getCurrentRotation() + 90) % 360;
            Variant<String> selected = selector.select(markerDegree);
            resultField.setText(selected.getValue());
            if (isRationalCheckBox.isSelected()) {
                renderWheel(selector);
            }
            wheelAnimating = false;
        });
        return rotate;
    }

    //todo too big
    //todo fix some delays with rational lists
    private int[] getSoundTime(int currentRotation, Speed speed) {
        int degree = speed.getRotationDegree();
        double[] oneCycle = selector.getVariantsList().probabilities();
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
            .mapToInt(e -> (int) (e * speed.duration))
            .toArray();
    }

    //todo a little big
    private int[] interpolateSoundTime(int[] soundTime, Speed speed, Interpolator interpolator) {
        int duration = speed.duration;
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
        Thread thread = new Thread(() -> {
            for (int time : soundTime) {
                try {
                    Thread.sleep(time);
                    player.seek(Duration.ZERO);
                    player.play();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    private void showPane(Pane pane) {
        ObservableList<Node> children = parentPane.getChildren();
        int nodeIndex = children.indexOf(pane);
        Node shadowRectangle = children.get(nodeIndex - 1);
        shadowRectangle.setOpacity(0);
        shadowRectangle.setVisible(true);

        FadeTransition opacityTransition = new FadeTransition();
        opacityTransition.setDuration(new Duration(ANIMATION_DURATION));
        opacityTransition.setToValue(MAX_SHADOW_OPACITY);
        opacityTransition.setNode(shadowRectangle);

        TranslateTransition translateTransition = new TranslateTransition();
        translateTransition.setDuration(new Duration(ANIMATION_DURATION));
        double yPos = mainPane.getHeight() - pane.getHeight();
        translateTransition.setToY(yPos);
        translateTransition.setNode(pane);

        opacityTransition.play();
        translateTransition.play();
    }

    private void hidePane(Pane pane) {
        ObservableList<Node> children = parentPane.getChildren();
        int nodeIndex = children.indexOf(pane);
        Node shadowRectangle = children.get(nodeIndex - 1);

        FadeTransition opacityTransition = new FadeTransition();
        opacityTransition.setDuration(new Duration(ANIMATION_DURATION));
        opacityTransition.setToValue(0);
        opacityTransition.setNode(shadowRectangle);
        opacityTransition.setOnFinished(e -> shadowRectangle.setVisible(false));

        TranslateTransition translateTransition = new TranslateTransition();
        translateTransition.setDuration(new Duration(ANIMATION_DURATION));
        double yPos = mainPane.getHeight();
        translateTransition.setToY(yPos);
        translateTransition.setNode(pane);

        opacityTransition.play();
        translateTransition.play();
    }

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
        };

        private final String name;
        private final int duration;

        Speed(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }

        public String getName() {
            return name;
        }

        public int getDuration() {
            return duration;
        }

        public abstract int getRotationDegree();
    }
}
