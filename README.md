# Muliplayer Game using Scala REST API - Akka HTTP
Implemented a RESTful Service using Akka HTTP in Scala

### Author: Vasu Garg
### Email: vasug20@gmail.com

## Introduction

This project contains a Scala REST application that uses the Akka HTTP's Actor Model to design the server side implementation of the application and Client side which runs the Game automatically.

Video Link: 
The video explains deployment of the REST application in AWS as a mincroservice using AWS ECR and ECS.

## Prerequisites

- JDK 11
- Akka 2.6.20
- Scala 2.13.x
- SBT (Scala Build Tool)

### Running the test cases

Test Files can be found under the directory src/test

````
sbt clean compile test
````

### Running the project

1) Clone this repository

```
git clone https://github.com/vasugarg/GraphPTGame
```
2) cd to the Project
```
cd GraphPTGame
```

3) Open the project in intelliJ
```
https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html#open-project
```
4) Open application.conf under src/main/resources/application.conf and make sure the relevant folders are created in the project. Below is the explanation of the configuration settings that needs to be set for the program to run.

##### Configuration Settings (application.conf)

- **`originalGraphFilePath`**
  - **Purpose:** Sets the filepath for the original graph from which we need to take out the valuable nodes.

- **`perturbedGraphFilePath`**
  - **Purpose:** Specifies the filepath for the perturbed graph on which we will perform the random walks.

- **Paths and Directories:**
  - Note that certain settings specify directory paths relative to the project's root directory.
  - For example, if `originalGraphFilePath` is set to `"inputs"`, it refers to the `inputs` directory within the project's root directory.
  - These relative paths are resolved based on the project's classpath, providing flexibility and independence from specific absolute file paths.
  - The `inputs` folder already contains sample graph generated by setting the number of states as 300 which can be used to run the program.
  - In order to run the program on different graphs, you can clone NetGameSime and call Main class to generate new graphs and making the relevant changes to application.conf post that.

5) Running project via SBT Run.
```
sbt run
```
6) (Optional) The application can be run as a docker container
```
```

## Project Structure

The project comprises the following key packages:

- **Server**: Server package comprises of the GameServer and GameApp scala files which contains the code for the server-side implementation and routes. 
- **Client**: The client package contains a program which automatically plays the game following the constraints and methods defined by the server application.
- **Actor**: The Akka actors - PoliceMan and Thief are defined under this package
- **Model**: Model contains the deserialised data i.e. the graph on which the game is played

## Akka Implementation

- **Actor System Initialization**: The project initializes an Akka Actor System as the foundational runtime for creating and managing actors. This system serves as the entry point for the game, where the Policeman and Thief actors operate.

- **Actor Creation**: Two primary actors are created to represent the Policeman and the Thief. These actors encapsulate the state and behavior relevant to each role in the game, such as their current position on the game board and the logic to calculate allowed moves.

- **Message Passing**: The actors communicate asynchronously through message passing, which dictates the flow of the game. Messages such as Move, GetPosition, and GetAllowedMoves are defined to facilitate this interaction, and the actors respond to these messages with actions or further messages.

- **Game Logic**: Actors make decisions based on the game logic implemented within their message-handling behavior. For example, the Move message will check if the move is valid, and if not, the actor can respond with an error or a game-over status.


## Conclusion

This project demonstrates the use of Akka HTTP in designing a REST API and deploying it as a microservice using Scala and AWS.

**Note:** This README provides an overview of the project. For detailed documentation and instructions, refer to the project's youtube video link and src files.
