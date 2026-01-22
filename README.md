# Terraforming Mars

Terraforming Mars is a desktop strategy game built in JavaFX, inspired by the idea of turning the Red Planet into a habitable world. The project combines core Java programming, game logic, and an interactive user interface designed with Scene Builder.

The game includes a complete engine that handles all core mechanics, from resource management to turn-based gameplay. Players can choose between hot-seat multiplayer and online matches with real-time synchronization. Online players can also communicate via a Java RMI-based chat service.

Key highlights:

- 12 unique starting corporations and over 240 project cards

- Save/load functionality with automatic XML replay generation

- Host/client networking with synchronized game state

- Auto-generated code documentation using Java Reflection API

It’s built with Java 17+, using Apache Maven for building and dependency management, while XML and serialization handle data storage.



## Configuration Setup

Before running the project, you need to set up the external configuration file that the JNDI `ConfigurationReader` expects: 

### Step 1: Create configuration directory

- Open File Explorer and navigate to your `C:\` drive  
- Create a new folder named `conf`  

### Step 2: Create `application.properties`

- Inside `C:\conf\`, create a new file named `application.properties`  
- Open it with any text editor and paste the following:

```properties
rmi.port=1099
server.port=1234
hostname=localhost
```

## Running the Game

To run the game, make sure you have JDK 17 (or newer) and Maven installed. Then simply clone the repository and launch it with:

```bash
git clone https://github.com/LukaCulina/Terraforming-Mars
cd Terraforming-Mars
mvn clean javafx:run
```
