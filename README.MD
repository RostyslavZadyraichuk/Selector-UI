# **Selector Util UI Application**

JavaFX selector application with dynamic probabilities changing ability.
This is a tool for selecting random element from the list.
It provides intuitive interface for user to manage selectors.
Selectors can be used to randomize elements in different situations.
Every selector can have different probabilities for each element depending on element's "weight".

### Prerequisites

Application requires **[Java](https://www.oracle.com/java/technologies/javase/) v11+** to run.
Before installation download **JDK v11+** and set up *JAVA_HOME* system environment variable.

### Launch

1. In CMD run following command that launch application: `mvn -pl ui-runner -am javafx:run` (Maven Wrapper is used).
2. Also, it's available to pack application with maven-shade-plugin: `mvn -pl ui-runner -am shade:shade`.
As result, you'll get fat archive with all dependencies. You can run it with: `java -jar <name>.jar`.

All user related files are stored by path: *%USER_PATH%/.my_utils/selector/*

### Functionality

### License

~~MIT~~
