package com.zadyraichuk.controller;

import com.zadyraichuk.general.MathUtils;
import com.zadyraichuk.general.PropertiesFile;
import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.variant.Variant;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
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

public class SelectorUIController {

    private static final File CLICK_SOUND = new File("/ui/click.wav");
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static int DURATION = 1000;

    @FXML
    private VBox mainPane;
    @FXML
    private VBox editPane;
    @FXML
    private VBox wheelsPane;

    @FXML
    private TextField resultField;
    @FXML
    private Group wheelGroup;
    @FXML
    private ChoiceBox<String> wheelChoiceBox;

    private AbstractRandomSelector<String, ? extends Variant<String>> selector;
    private boolean wheelAnimating = false;
    private MediaPlayer player = new MediaPlayer(new Media(CLICK_SOUND.toURI().toString()));

    public void init() {
        selector = SelectorDataController.getInstance().getCurrentSelector();
        if (selector != null) {
            renderWheelSelector(SelectorDataController.getInstance().getVariantsListNames(),
                selector.getName());
            renderWheel(selector);
        }
    }

    @FXML
    public void onNewClick() {

    }

    @FXML
    public void onRollClick() {
        if (selector != null && !wheelAnimating) {
            wheelAnimating = true;
            int degree = generateRotationDegree();
            RotateTransition rotate = getRotateTransition(degree);

            int[] soundTime = getSoundTime(selector.getCurrentRotation(), degree, DURATION);
            soundTime = interpolateSoundTime(soundTime, DURATION, rotate.getInterpolator());
            startRollSoundThread(soundTime);
            rotate.play();
        }
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
        wheelChoiceBox.getItems().clear();
        wheelChoiceBox.getItems().addAll(list);

        wheelChoiceBox.getSelectionModel().select(selected);
    }

    private void renderWheel(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
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
        rotate.setDuration(Duration.millis(DURATION));
        rotate.setByAngle(degree);
        rotate.setNode(wheelGroup);

        rotate.setOnFinished(e -> {
            selector.setCurrentRotation((int) wheelGroup.getRotate());
            wheelGroup.setRotate(selector.getCurrentRotation());
            int markerDegree = (selector.getCurrentRotation() + 90) % 360;
            Variant<String> selected = selector.select(markerDegree);
            resultField.setText(selected.getValue());
            wheelAnimating = false;
        });
        return rotate;
    }

    private int generateRotationDegree() {
        if (DURATION < 1000)
            return RANDOM.nextInt(360) + 360 * 2;
        if (DURATION > 3000)
            return RANDOM.nextInt(360) + 360 * 4;
        else
            return RANDOM.nextInt(360) + 360 * 3;
    }

    private int[] getSoundTime(int currentRotation, int degree, int duration) {
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
            .mapToInt(e -> (int) (e * duration))
            .toArray();
    }

    private int[] interpolateSoundTime(int[] soundTime, int duration, Interpolator interpolator) {
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
}
