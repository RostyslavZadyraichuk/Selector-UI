package com.zadyraichuk.controller;

import com.zadyraichuk.App;
import com.zadyraichuk.general.ResourceLoader;
import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.selector.RandomSelector;
import com.zadyraichuk.selector.RationalRandomSelector;
import com.zadyraichuk.variant.Variant;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectorDataController {

    private static final String VARIANTS_DIR;

    private static final String FILE_EXTENSION = "json";

    private static SelectorDataController instance;

    // TODO change key to String = Selector.name + hash
    private final Map<Long, AbstractRandomSelector<String, ? extends Variant<String>>> selectors;

    private AbstractRandomSelector<String, ? extends Variant<String>> currentSelector;

    static {
        VARIANTS_DIR = App.USER_PATH + "variants/";
        ResourceLoader.loadResource(
            "/variants/templates/Template." + FILE_EXTENSION,
            App.USER_PATH + "variants/templates/",
            "Template",
            FILE_EXTENSION);
    }

    private SelectorDataController() {
        selectors = new HashMap<>();
        setUpAllVariants();

        if (selectors.isEmpty()) {
            currentSelector = loadSelectorTemplate();
        } else {
            var defaultSelector = selectors.values().stream()
                .findFirst()
                .orElse(loadSelectorTemplate());

            currentSelector = Optional.ofNullable(App.PROPERTIES)
                .map(props -> props.getProperty("last.used.variant.id"))
                .map(Long::parseLong)
                .map(selectors::get)
                .orElse(null);

            currentSelector = currentSelector == null ? defaultSelector : currentSelector;
        }
    }

    public static SelectorDataController getInstance() {
        if (instance == null) {
            instance = new SelectorDataController();
        }
        return instance;
    }

    public void setCurrentSelector(long selectorId) {
        AbstractRandomSelector<String, ? extends Variant<String>> selector = selectors.get(selectorId);
        if (selector == null) {
            currentSelector = loadSelectorTemplate();
        } else {
            // TODO save in file via Selector IO thread and change
            currentSelector = selector;
            App.PROPERTIES.setProperty("last.used.variant.id", String.valueOf(currentSelector.getId()));
        }
    }

    public AbstractRandomSelector<String, ? extends Variant<String>> getCurrentSelector() {
        return currentSelector;
    }

    public void updateCurrentSelector(AbstractRandomSelector<String, ? extends Variant<String>> newSelector) {
        if (newSelector != null && selectors.containsKey(newSelector.getId())) {
            selectors.remove(newSelector.getId());
            if (currentSelector.getId() != newSelector.getId()) {
                File oldFile = new File(getSelectorPath(currentSelector));
                SelectorIO.delete(oldFile.toPath());
            }

            saveNewSelector(newSelector);
        }
    }

    public void saveNewSelector(AbstractRandomSelector<String, ? extends Variant<String>> newSelector) {
        selectors.put(newSelector.getId(), newSelector);

        try {
            File newFile = new File(getSelectorPath(newSelector));
            SelectorIO.write(newSelector, newFile.toPath());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        currentSelector = newSelector;
    }

    public void removeSelector(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
        selectors.remove(selector.getId());
        File oldFile = new File(getSelectorPath(selector));
        SelectorIO.delete(oldFile.toPath());

        currentSelector = selectors.values().stream().findFirst().orElse(null);
    }

    public Set<IdNamePair> getVariantPairsForSelector() {
        return selectors.values().stream()
            .map(selector -> new IdNamePair(selector.getId(), selector.getName()))
            .collect(Collectors.toSet());
    }

    public void makeCurrentSelectorRational() {
        if (currentSelector != null &&
            !(currentSelector instanceof RationalRandomSelector)) {
            currentSelector = RationalRandomSelector.of(currentSelector);
            selectors.put(currentSelector.getId(), currentSelector);
        }
    }

    public void makeCurrentSelectorNotRational() {
        if (currentSelector != null &&
            !(currentSelector instanceof RandomSelector)) {
            currentSelector = RandomSelector.of(currentSelector);
            selectors.put(currentSelector.getId(), currentSelector);
        }
    }

    public AbstractRandomSelector<String, ?> loadSelectorTemplate() {
        try {
            File templateFile = new File(VARIANTS_DIR + "templates/Template." + FILE_EXTENSION);
            // selectors.put("Template", template);
            return SelectorIO.read(templateFile.toPath());
        } catch (IOException | NullPointerException e) {
            System.out.println("Cannot read template");
        }

        return new RandomSelector("Empty List");
    }

    private String getSelectorPath(AbstractRandomSelector<String, ? extends Variant<String>> selector) {
        return VARIANTS_DIR + selector.getName() + '_' + selector.getId() + '.' + FILE_EXTENSION;
    }

    private void setUpAllVariants() {
        File directory = new File(VARIANTS_DIR);

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    String fileNameWithExt = file.getName();

                    if (fileNameWithExt.endsWith('.' + FILE_EXTENSION)) {
                        try {
                            AbstractRandomSelector<String, ? extends Variant<String>> selector =
                                SelectorIO.read(Path.of(file.getPath()));
                            selectors.put(selector.getId(), selector);
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
