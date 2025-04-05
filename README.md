# Villages Mod

A Minecraft mod that completely overhauls villages with custom buildings and improved villager AI. Inspired by [Millenaire](https://www.curseforge.com/minecraft/mc-mods/millenaire).

## Table of Contents
- [Setup](#setup)
- [Running the Project](#running-the-project)
- [Features](#features)
- [Blueprint System](#blueprint-system)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/villages-mod.git
   cd villages-mod
   ```

2. Build the project:
   ```
   ./gradlew build
   ```

## Running the Project

### Development Environment

To run the client in a development environment:

**Windows**:
```
.\gradlew runClient
```

**macOS/Linux**:
```
./gradlew runClient
```

To run the server:
```
./gradlew runServer
```

### Building for Distribution

To build a distributable JAR file:
```
./gradlew build
```

The compiled mod JAR will be available in `build/libs/`.

## Features

### Village Finder

The mod includes a village finder feature that allows players to locate nearby villages:

- Press the `V` key to find villages near your current position
- The mod will display a list of villages sorted by distance, including:
  - Distance to the village
  - Direction to the village
  - Coordinates of the village

The village finder has been optimized for performance:
- Client-side throttling prevents excessive server requests
- Position-aware caching only sends new requests when moving a significant distance
- Server-side caching for village locations reduces computational load
- Asynchronous processing to prevent server lag

### Blueprint System

This mod includes a blueprint system that allows you to store building blueprints in JSON format and use them to generate villages.

#### Adding Custom Blueprints

To add a custom blueprint, follow these steps:

1. Create a JSON file for your blueprint in the `src/main/resources/blueprints` directory.
2. The JSON file should have the following structure:

```json
{
  "name": "example_blueprint",
  "width": 5,
  "height": 5,
  "length": 5,
  "blockData": [
    {
      "x": 0,
      "y": 0,
      "z": 0,
      "block": "minecraft:stone"
    },
    {
      "x": 1,
      "y": 0,
      "z": 0,
      "block": "minecraft:stone"
    }
  ]
}
```

3. Save the JSON file and restart the game. The new blueprint will be loaded and used to generate villages.

#### Blueprint Format Details

- `name`: Unique identifier for the blueprint
- `width`, `height`, `length`: Dimensions of the blueprint
- `blockData`: Array of block positions and types
  - `x`, `y`, `z`: Block coordinates relative to the blueprint origin
  - `block`: Block ID (must be a valid Minecraft block ID)
  - `properties` (optional): Block properties as key-value pairs

#### Generating Blueprints from Existing Structures

You can use the in-game tools to convert existing structures to blueprints:

1. Craft a Blueprint Tool (`WOODEN_AXE`)
2. Select the first corner of your structure
3. Select the opposite corner
4. Name your blueprint and save

## Project Structure

```
villages-mod/
├── src/
│   ├── main/
│   │   ├── java/        # Java source code
│   │   ├── resources/   # Resources like textures, models, and blueprints
│   │   └── ...
│   └── test/            # Test files
├── build.gradle         # Gradle build configuration
├── gradle/              # Gradle wrapper
└── README.md            # This file
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
