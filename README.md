# Minecraft 1.21.2 NeoForge Mod Workspace

A complete, modern mod development workspace for **Minecraft 1.21.2** powered by **NeoForge** and **ModDevGradle**.

---

## 🛠️ Project Stack & Versions

- **Minecraft Version**: `1.21.1`
- **NeoForge Version**: `21.1.248`
- **ModDevGradle Plugin**: `net.neoforged.moddev:2.0.78`
- **Gradle Version**: `8.11.1` (Wrapper included)
- **Java Requirement**: **Java 21**
- **Resource Pack Format**: `34`

---

## 🚀 Getting Started

### 1. IDE Setup

#### IntelliJ IDEA (Recommended)
1. Open IntelliJ IDEA.
2. Select **Open** and choose this folder (`minecraft mod`).
3. IntelliJ will automatically detect the Gradle project and import dependencies.
4. If prompted to configure the JDK, ensure **Java 21** is selected.

#### Visual Studio Code
1. Install the **Extension Pack for Java** and **Gradle for Java** extensions.
2. Open this folder in VS Code.
3. Allow the Java language server to import the Gradle project.

---

## 💻 Common Gradle Commands

Run these commands using PowerShell or the terminal in the project root:

| Command | Description |
| :--- | :--- |
| `.\gradlew runClient` | Launches the Minecraft client with your mod loaded |
| `.\gradlew runServer` | Launches a dedicated server with your mod |
| `.\gradlew runData` | Runs data generators (recipes, loot tables, tags, etc.) |
| `.\gradlew build` | Builds your mod `.jar` file located in `build/libs/` |
| `.\gradlew generateModMetadata` | Generates the `neoforge.mods.toml` from `gradle.properties` |

---

## 📁 Project Structure

```text
├── .gitignore
├── README.md
├── build.gradle                               # ModDevGradle build script
├── gradle.properties                          # Mod metadata and version configuration
├── settings.gradle                            # Plugin repositories and toolchains
├── gradlew & gradlew.bat                      # Gradle wrapper scripts
├── gradle/wrapper/                            # Gradle wrapper binaries and properties
├── src/
│   ├── main/
│   │   ├── java/com/example/examplemod/
│   │   │   ├── ExampleMod.java                # Main entry point (@Mod)
│   │   │   └── Config.java                    # Mod configuration spec
│   │   ├── resources/
│   │   │   ├── pack.mcmeta                    # Resource pack description
│   │   │   └── assets/examplemod/
│   │   │       ├── blockstates/               # Blockstate definitions
│   │   │       ├── lang/en_us.json            # English translations
│   │   │       ├── models/block/              # Block model definitions
│   │   │       ├── models/item/               # Item model definitions
│   │   │       └── textures/                  # PNG textures for blocks and items
│   │   └── templates/
│   │       └── META-INF/neoforge.mods.toml    # Mod metadata template
```

## 📁 Dynamic External Content System

This mod is designed from the ground up to be **data-driven**. All items, blocks, textures, names, and descriptions can be created or customized externally without modifying Java code!

### Directory Structure in `run/config/examplemod/` (or `.minecraft/config/examplemod/`):
```text
config/examplemod/
├── items/
│   └── example_item.json          # Item definition (name, food, rarity, tooltips)
├── blocks/
│   └── example_block.json         # Block definition (name, hardness, sound, light, WAILA, 6 faces)
├── fluids/
│   └── acid.json                  # Fluid definition (flow physics, effects, transparency, bucket)
└── textures/
    ├── item/
    │   ├── example_item.png       # Item textures & bucket textures (.png or .gif)
    │   └── acid_bucket.png
    ├── block/
    │   └── example_block.png      # Block face textures (.png or .gif)
    └── fluid/
        ├── acid_still.png         # Still fluid textures (.png or .gif)
        └── acid_flow.png          # Flowing fluid textures (.png or .gif)
```

Whenever you add a new `.json` file and matching `.png` or animated `.gif` texture to these folders, the mod will automatically:
1. Register the item/block/fluid into Minecraft.
2. Auto-generate blockstates, item models, and fluid bucket models.
3. Auto-convert `.gif` files into native animated sprite sheets with smooth frame interpolation.
4. Add items, blocks, and fluid buckets to the creative tab.
5. Add descriptions to **JEI**.
6. Add HUD tooltip overlays to **Jade (WAILA)**.

---

## 🔌 Modpack Library Integrations

This mod includes built-in, soft-dependency integrations for top modpack utilities:

- **JEI (Just Enough Items)**:
  - Plugin: [`ExampleModJeiPlugin.java`](src/main/java/com/example/examplemod/compat/jei/ExampleModJeiPlugin.java)
  - Registers custom item and block descriptions into JEI's information tab.
- **Jade (Modern WAILA / HWYLA)**:
  - Plugin: [`ExampleModJadePlugin.java`](src/main/java/com/example/examplemod/compat/jade/ExampleModJadePlugin.java) & [`ExampleBlockComponentProvider.java`](src/main/java/com/example/examplemod/compat/jade/ExampleBlockComponentProvider.java)
  - Displays custom HUD overlay tooltips when looking at mod blocks.
- **Pre-configured Maven Repositories** in `build.gradle`:
  - `ModMaven`, `Modrinth Maven`, `CurseMaven`, `Shedaniel Maven` (REI / Cloth Config), and `TerraformersMC` (EMI).

---

## 🎨 Customizing Your Mod

### 1. Renaming the Mod ID / Package
1. In `gradle.properties`, change `mod_id`, `mod_name`, `mod_authors`, `mod_group_id`, etc.
2. In `ExampleMod.java`, update the `MODID` constant and package declaration to match your new mod ID and group ID.
3. In `src/main/resources/assets/`, rename the `examplemod` subfolder to your new `mod_id`.

### 2. Adding New Items
Use the `ITEMS` deferred register in `ExampleMod.java` (or in a dedicated registry class):
```java
public static final DeferredItem<Item> MY_ITEM = ITEMS.registerItem("my_item", Item::new, new Item.Properties());
```

### 3. Adding New Blocks
Use the `BLOCKS` deferred register:
```java
public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.registerSimpleBlock("my_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
public static final DeferredItem<BlockItem> MY_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("my_block", MY_BLOCK);
```
