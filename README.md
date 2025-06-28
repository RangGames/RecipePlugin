# RecipePlugin

## Introduction

RecipePlugin is a Minecraft Bukkit/Spigot plugin that allows server administrators to create and manage custom crafting recipes. It provides a flexible system for defining recipes, ingredients, and cooking processes, complete with a graphical user interface (GUI) for ease of use. The plugin also features data persistence through a database, making it suitable for multi-server environments.

## Features

* **Custom Recipe Creation**: Create unique recipes that are not available in vanilla Minecraft.
* **GUI-Based Recipe Management**: An intuitive GUI allows for the easy addition, editing, and removal of recipes.
* **Cooking System with Progress Bar**: Recipes can have a specific cooking time, and players will see a boss bar that displays the remaining time.
* **Permission-Based Recipes**: Control which players have access to which recipes using LuckPerms integration.
* **Player Data Storage**: The plugin saves player-specific data, including their cauldron contents, a virtual inventory, and cooking equipment.
* **Database Integration**: Player and recipe data can be saved to a MySQL database, ensuring data is synchronized across multiple servers.
* **API for Developers**: Provides an API for other plugins to interact with its features.

## Commands

The main command for this plugin is `/요리`.

* `/요리`: Opens the recipe book, showing all available recipes.
* `/요리 추가 <recipe_name> <cooking_time>`: Adds a new recipe.
* `/요리 편집 <recipe_name> <cooking_time>`: Edits an existing recipe.
* `/요리 제거 <recipe_name>`: Deletes a recipe.
* `/요리 재료`: Opens the player's cauldron inventory.
* `/요리 가방`: Opens the player's virtual cooking bag.
* `/요리 장비`: Opens the cooking equipment GUI.
* `/요리 취소`: Cancels the current cooking process.

## Permissions

* `cook.add`: Allows a player to add new recipes.
* `cook.edit`: Allows a player to edit existing recipes.
* `cook.delete`: Allows a player to delete recipes.
* `cook.item`: Allows a player to use the `/요리 장비` command.
* `cook.chest`: Allows a player to use the `/요리 재료` command.
* `recipe.<recipe_name>`: Grants a player permission to use a specific recipe.

## Configuration

The plugin's configuration is split into two main files:

### `config.yml`

```yaml
database:
  host: "localhost:3306"
  name: "recipe"
  user: "root"
  password: ""
server-name: "server1"
auto-save:
  interval: 300
  enabled: true
debug:
  enabled: false
  log-level: "INFO"
````

  * **database**: Configuration for the MySQL database connection.
  * **server-name**: A unique name for the server, used for data synchronization.
  * **auto-save**: Settings for automatically saving plugin data.
  * **debug**: Options for enabling debug logging.

### `Option/config.yml`

This file allows for the customization of in-game items and GUI titles.

  * **item**: Defines the appearance of items used in the GUI, such as "next page" and "previous page" buttons.
  * **gui\_title**: Customizes the titles of the recipe book and other GUIs.
  * **bossBar**: Customizes the text displayed in the cooking progress boss bar.

## Dependencies

  * **LuckPerms**: Required for permission management.
  * **Paper API**: Built against the Paper API.

## Installation

1.  Download the latest version of the plugin.
2.  Place the `.jar` file in your server's `plugins` directory.
3.  Install the required dependencies (LuckPerms).
4.  Start your server.
5.  Configure the plugin by editing the files in the `plugins/RecipePlugin` directory.

## For Developers

### API

The plugin provides an API for developers to integrate their own plugins with RecipePlugin. The main API class is `gaya.pe.kr.core.API.RecipeAPI`. You can use this to access and manipulate recipe and player data.

### Building from Source

To build the plugin from the source code, you will need:

  * Java Development Kit (JDK) 21 or higher.
  * Apache Maven.

Clone the repository and run the following command in the root directory:

```bash
mvn clean package
```

This will create the plugin's `.jar` file in the `target` directory.
