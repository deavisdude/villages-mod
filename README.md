# villages-mod
A minecraft mod to improve villages. Inspired by [Millenaire](https://github.com/MoonCutter2B/Millenaire)

## Blueprint System

This mod includes a blueprint system that allows you to store building blueprints in JSON format and use them to generate villages.

### Adding Custom Blueprints

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
