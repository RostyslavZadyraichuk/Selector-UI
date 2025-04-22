# **Selector Util UI Application**

JavaFX selector application with dynamic probabilities changing ability.
This is a tool for selecting random element from the list.
It provides intuitive interface for user to manage selectors.
Selectors can be used to randomize elements in different situations.
Every selector can have different probabilities for each element depending on element's "weight".

### Features
- **Random Selector Management:** 
  - The application provides the ability to manage random selectors via an intuitive UI.
  - Users can create, update, and delete selectors with ease.
  - Each selector can be configured with different probabilities for its elements.

- **Dynamic Probability Adjustment:**
  - Users can dynamically adjust the probability weights of elements within a selector.
  - The UI allows for real-time updates to reflect these changes.

- **Rational and Non-Rational Selectors:**
  - The application supports both Rational and Non-Rational selectors.
  - Users can switch between these types depending on their requirements.

- **Selector Persistence:**
  - Selectors can be saved to and loaded from JSON files.
  - This ensures that users can retain their configurations between sessions.

- **Color Coding Support:**
  - Elements within the selectors are visually distinguished using color coding.
  - The application allows for random and ordered color palettes to be generated for selectors.

- **JavaFX-based User Interface:**
  - The application is built using JavaFX, providing a rich and responsive user interface.
  - It includes features like drag-and-drop, animations, and custom styling.

### Prerequisites

Application requires **[Java](https://www.oracle.com/java/technologies/javase/) v11+** to run.
Before installation download **JDK v11+** and set up *JAVA_HOME* system environment variable.
**Maven** is not required to be installed on your system, because Maven Wrapper is used in this project.

### Getting Started & Installation

1. Clone repository:
```bash
git clone https://github.com/Zadyraichuk/SelectorUtil.git
```

2. Go to the project root directory:
```bash
cd selector-util-api
```

3. In CMD run following command that launch application:
```bash
mvn -pl ui-runner -am javafx:run
```

Also, it's available to pack application: `mvn package`.
As result, you'll get fat archive with all dependencies in `/ui-runner/target/shade` directory. 
You can run it with `java -jar <name>.jar` command.

All user related files are stored by path: *%USER_PATH%/.my_utils/selector/*.
