Terraforming Mars is a desktop strategy game built in JavaFX, inspired by the idea of turning the Red Planet into a habitable world. The project combines core Java programming, game logic, and an interactive user interface designed with Scene Builder.

The game features a complete engine that manages all core mechanics, from resource handling to turn-based actions. You can easily save and load your progress using serialization, and every session is automatically recorded in an XML replay file, so you can review your game history or continue later.

The project also includes auto-generated documentation using Java’s Reflection API, ensuring the internal structure of the code stays transparent and easy to maintain.

It’s built with Java 17+, using Apache Maven for building and dependency management, while XML and serialization handle data storage.

To run the game, make sure you have JDK 17 (or newer) and Maven installed. Then simply clone the repository and launch it with:

git clone [YOUR_REPOSITORY_URL]
cd [YOUR_PROJECT_FOLDER]
mvn clean javafx:run
