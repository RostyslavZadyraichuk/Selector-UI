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
import java.util.Set;

public class SelectorDataController {

    private static final String VARIANTS_DIR;

    private static final String FILE_EXTENSION = "json";

    private static SelectorDataController instance;

    // TODO change key to String = Selector.name + hash
    private final Map<String, AbstractRandomSelector<String, ? extends Variant<String>>> selectors;

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
            if (App.PROPERTIES == null) {
                System.out.println("Not found properties file. Used first available selector.");
                currentSelector = selectors.values().stream().findFirst().orElse(null);
            } else {
                String lastUsedSelector = App.PROPERTIES.getProperty("last.used.variant");
                currentSelector = selectors.get(lastUsedSelector);
            }
        }
    }

    public static SelectorDataController getInstance() {
        if (instance == null) {
            instance = new SelectorDataController();
        }
        return instance;
    }

    public void setCurrentSelector(String name) {
        AbstractRandomSelector<String, ? extends Variant<String>> selector = selectors.get(name);
        if (selector == null) {
            currentSelector = loadSelectorTemplate();
        } else {
            // TODO save in file via Selector IO thread and change
            currentSelector = selector;
            App.PROPERTIES.setProperty("last.used.variant", currentSelector.getName());
        }
    }

    public AbstractRandomSelector<String, ? extends Variant<String>> getCurrentSelector() {
        return currentSelector;
    }

    public void updateCurrentSelector(AbstractRandomSelector<String, ? extends Variant<String>> newSelector) {
        if (selectors.containsKey(newSelector.getName())) {
            selectors.remove(currentSelector.getName());
            if (!currentSelector.getName().equals(newSelector.getName())) {
                File oldFile = new File(VARIANTS_DIR + currentSelector.getName() + '.' + FILE_EXTENSION);
                SelectorIO.delete(oldFile.toPath());
            }
            currentSelector = newSelector;

            saveNewSelector(newSelector);
        }
    }

    public void saveNewSelector(AbstractRandomSelector<String, ? extends Variant<String>> newSelector) {
        selectors.put(newSelector.getName(), newSelector);

        try {
            File newFile = new File(VARIANTS_DIR + newSelector.getName() + '.' + FILE_EXTENSION);
            SelectorIO.write(newSelector, newFile.toPath());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public Set<String> getVariantsListNames() {
        return selectors.keySet();
    }

    public void makeCurrentSelectorRational() {
        if (currentSelector != null &&
            !(currentSelector instanceof RationalRandomSelector)) {
            currentSelector = RationalRandomSelector.of(currentSelector);
            selectors.put(currentSelector.getName(), currentSelector);
        }
    }

    public void makeCurrentSelectorNotRational() {
        if (currentSelector != null &&
            !(currentSelector instanceof RandomSelector)) {
            currentSelector = RandomSelector.of(currentSelector);
            selectors.put(currentSelector.getName(), currentSelector);
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

    private void setUpAllVariants() {
        File directory = new File(VARIANTS_DIR);

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    String fileNameWithExt = file.getName();

                    if (fileNameWithExt.endsWith('.' + FILE_EXTENSION)) {
                        String fileName = fileNameWithExt.substring(0,
                            fileNameWithExt.length() - FILE_EXTENSION.length() - 1);
                        try {
                            selectors.put(fileName, SelectorIO.read(Path.of(file.getPath())));
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
