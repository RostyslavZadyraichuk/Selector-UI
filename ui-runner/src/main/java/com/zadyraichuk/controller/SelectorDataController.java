package com.zadyraichuk.controller;

import com.zadyraichuk.App;
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

    private static final String VARIANTS_DIR = "/variants/";

    private static SelectorDataController instance;

    private final Map<String, AbstractRandomSelector<String, ? extends Variant<String>>> selectors;

    private AbstractRandomSelector<String, ? extends Variant<String>> currentSelector;

    private SelectorDataController() {
        selectors = new HashMap<>();
        setUpAllVariants();
        if (selectors.isEmpty()) {
            loadSelectorTemplate();
        } else {
            if (App.PROPERTIES == null) {
                System.out.println("Not found properties file. Used first available selector.");
                currentSelector = selectors.values().stream().findFirst().orElse(null);
            } else {
                String lastUsedSelector = App.PROPERTIES.getProperty("last.used.variants");
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
            loadSelectorTemplate();
        } else {
            //todo save in file via Selector IO thread and change
            currentSelector = selector;
            App.PROPERTIES.setProperty("last.used.variants", currentSelector.getName());
        }
    }

    public AbstractRandomSelector<String, ? extends Variant<String>> getCurrentSelector() {
        return currentSelector;
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

    private void setUpAllVariants() {
        File directory = new File(VARIANTS_DIR);

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    String fileNameWithExt = file.getName();

                    if (fileNameWithExt.endsWith(".selector")) {
                        String fileName = fileNameWithExt.substring(0,
                                fileNameWithExt.length() - 9);
                        try {
                            selectors.put(fileName, SelectorIO.read(Path.of(file.getPath())));
                        } catch (ClassNotFoundException | IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private void loadSelectorTemplate() {
        try {
            File templateFile = new File(VARIANTS_DIR + "templates/Template.selector");
            AbstractRandomSelector<String, ?> template = SelectorIO.read(templateFile.toPath());
            selectors.put("Template", template);
            currentSelector = template;
        } catch (ClassNotFoundException | IOException | NullPointerException e) {
            System.out.println("Cannot read template");
        }
    }
}
