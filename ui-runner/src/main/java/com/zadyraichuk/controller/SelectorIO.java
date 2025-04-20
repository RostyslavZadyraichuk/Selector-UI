package com.zadyraichuk.controller;

import com.zadyraichuk.selector.AbstractRandomSelector;
import com.zadyraichuk.selector.RandomSelector;
import com.zadyraichuk.selector.RationalRandomSelector;
import com.zadyraichuk.variant.Variant;
import java.io.*;
import java.nio.file.Path;

public class SelectorIO {

    public static AbstractRandomSelector<String, ? extends Variant<String>> read(Path path)
            throws ClassNotFoundException, IOException, NullPointerException {
        File file = path.toFile();

        try (InputStream is = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            if (is.available() > 0) {
                Object read = ois.readObject();
                Class<?> clazz = read.getClass();

                if (clazz.equals(RandomSelector.class))
                    return (RandomSelector) read;
                else if (clazz.equals(RationalRandomSelector.class))
                    return (RationalRandomSelector) read;
                else
                    throw new ClassNotFoundException("Undefined selector class: " + clazz.getName());
            } else {
                throw new NullPointerException("No object inside selector file");
            }
        }
    }

    public static void write(AbstractRandomSelector<String, ? extends Variant<String>> selector, Path path)
            throws IOException {
        File file = path.toFile();

        try (OutputStream os = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(os)) {
            selector.getVariantsList().initVariantPercents();
            oos.writeObject(selector);
        }
    }

    public static void delete(Path path) {
        File file = path.toFile();
        file.delete();
    }

////    //TODO create some templates + constructor

}
