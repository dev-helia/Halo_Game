package controller;

import model.IModel;
import model.core.Room;
import model.elements.Fixture;
import model.elements.Item;
import model.obstacle.GameObstacle;
import model.obstacle.Monster;
import utils.fileutil.PathUtils;
import view.View;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * The type Game controller.
 */
public class GameController {
  // Refactored GameController: Decouple Player and WorldEngine with IModel
  private final IModel model;
  private final View view;
  private final Scanner scanner;

  private static final String NORTH_KEY = "N";
  private static final String SOUTH_KEY = "S";
  private static final String EAST_KEY = "E";
  private static final String WEST_KEY = "W";

  private static final String TAKE_KEY = "T";
  private static final String DROP_KEY = "D";
  private static final String USE_KEY = "U";

  private static final String INVENTORY_KEY = "I";
  private static final String LOOK_KEY = "L";
  private static final String EXAMINE_KEY = "X";
  private static final String ANSWER_KEY = "A";

  private static final String SAVE_KEY = "SAVE";
  private static final String RESTORE_KEY = "RESTORE";
  private static final String NEW_KEY = "NEW";
  private static final String QUIT_KEY = "Q";
  private static final String HELP_KEY = "HELP";

  static {
    try {
      Files.createDirectories(Paths.get(new File(PathUtils.getSavePath("")).getParent()));
    } catch (IOException ignored) { }
  }

  /**
   * Instantiates a new Game controller.
   *
   * @param model       the model
   * @param view        the view
   * @param inputSource the input source
   */
  public GameController(IModel model, View view, Readable inputSource) {
    this.model = model;
    this.view = view;
    this.scanner = new Scanner(inputSource);
  }

  /**
   * Start game.
   *
   * @throws IOException the io exception
   */
  public void startGame() throws IOException {
    view.displayMainMenu();
    view.showMessage("Type " + NEW_KEY + " start a new game or " + RESTORE_KEY + " to load saves:");

    while (true) {
      String input = scanner.nextLine().trim().toUpperCase();

      if (input.equals(NEW_KEY)) {
        String mapPath = selectMap();
        if (mapPath == null) {
          view.showMessage("Map selection failed. Exiting...");
          return;
        }
        model.generateWorld(mapPath);
        view.showMessage("Map loaded successfully!");
        view.showMessage("Enter your name:");
        String name = scanner.nextLine().trim();
        model.initializePlayer(name);
        break;
      } else if (input.equals(RESTORE_KEY)) {
        String savePath = selectSave();
        if (savePath == null) {
          view.showMessage("Save selection failed. Exiting...");
          return;
        }
        if (model.loadGame(savePath)) {
          view.showMessage("Game restored successfully!");
          break;
        } else {
          view.showMessage("Restore failed.");
        }
      } else {
        view.showMessage("Invalid input. Please enter 'NEW' or 'RESTORE'.");
      }
    }

    while (true) {
      view.showMessage("Health Status: " + model.getHealthStatus());

      GameObstacle obs = model.getCurrentRoom().getObstacle();
      if (obs instanceof Monster m && m.isActive() && m.canAttack()) {
        m.attack(model.getPlayer());
        view.showMessage(m.getAttackMessage());
        view.showMessage("Player takes -" + m.getDamage() + " damage!");
        if (model.getHealth() <= 0) {
          view.showMessage("You have fallen asleep... Game Over!");
          break;
        }
      }

      view.showMessage("Type a command ('HELP' for help, 'Q' to quit):");
      if (!scanner.hasNextLine()) break;
      String line = scanner.nextLine().trim();
      String[] parts = line.split(" ", 2);
      String cmd = parts[0].toUpperCase();
      String arg = parts.length > 1 ? parts[1] : null;

      if (cmd.equals(QUIT_KEY) && handleQuitCommand()) break;
      handleCommand(cmd, arg);
    }

    view.showMessage("Thanks for playing! Goodbye~");
    view.showMessage("Final Score: " + model.getScore());
    view.showMessage("Your Rank: " + model.getPlayerRank());
  }




  /**
   * Check the quit command input.
   *
   * @param cmd the user's command input
   * @return true if it is a quit command
   */
  private boolean isQuitCommand(String cmd) {
    return cmd.equals(QUIT_KEY);
  }

  /**
   * Handing user's input commands.
   *
   * @param cmd  current command.
   * @param arg the input string
   */
  private void handleCommand(String cmd, String arg) {
    switch (cmd) {
      case NORTH_KEY, SOUTH_KEY, EAST_KEY, WEST_KEY -> handleMove(cmd);
      case TAKE_KEY -> handleTake(arg);
      case DROP_KEY -> handleDrop(arg);
      case USE_KEY -> handleUse(arg);
      case INVENTORY_KEY -> handleInventory();
      case LOOK_KEY -> handleLook();
      case EXAMINE_KEY -> handleExamine(arg);
      case ANSWER_KEY -> handleAnswer(arg);
      case SAVE_KEY -> handleSave();
      case RESTORE_KEY -> handleRestore();
      case HELP_KEY -> handleHelp();
      default -> view.showMessage("Unknown command: " + cmd);
    }
  }

  /**
   * Handling move command.
   *
   * @param cmd the move command
   */
  private void handleMove(String cmd) {
    if (model.movePlayer(cmd)) {
      view.showMessage("You move " + directionToFull(cmd) + ".");
      view.renderGame(model.getPlayer(), model.getCurrentRoom() );
    } else {
      view.showMessage("You can't move that way.");
    }
  }

  /**
   * Handling take the item.
   *
   * @param arg the input string
   */
  private void handleTake(String arg) {
    if (arg == null) {
      view.showMessage("What do you want to take?");
      return;
    }
    boolean success = model.pickItem(arg);
    view.showMessage(success ? "You picked up " + arg + "." : "You can't take that.");
  }

  /**
   * Handling dropping the item.
   *
   * @param arg the input string
   */
  private void handleDrop(String arg) {
    if (arg == null) {
      view.showMessage("What do you want to drop?");
      return;
    }
    boolean success = model.dropItem(arg);
    view.showMessage(success ? "You dropped " + arg + "." : "You don't have that item.");
  }

  /**
   * Handling use the item.
   *
   * @param arg
   */
  private void handleUse(String arg) {
    if (arg == null) {
      view.showMessage("Use what?");
      return;
    }
    view.showMessage(model.useItem(arg));
  }

  /**
   * Handling check the inventory.
   */
  private void handleInventory() {
    List<Item> inventory = model.getInventory();
    if (inventory.isEmpty()) {
      view.showMessage("Your inventory is empty.");
    } else {
      view.showMessage("Your Inventory:");
      for (Item i : inventory) {
        view.showMessage(" - " + i.getName() + " (uses left: " + i.getUsesRemaining() + ")");
      }
    }
  }

  /**
   * Handling look around the room.
   */
  private void handleLook() {
    view.renderGame(model.getPlayer(), model.getCurrentRoom());
  }

  /**
   * Handling examine command.
   *
   * @param arg the input string
   */
  private void handleExamine(String arg) {
      if (arg == null) {
        view.showMessage("Examine what?");
        return;
      }
      Room room = model.getCurrentRoom();
      boolean found = false;
      Item item = room.getItem(arg);
      if (item != null) {
        view.showMessage(item.getDescription());
        found = true;
      }
      Fixture fixture = room.getFixture(arg);
      if (fixture != null) {
        view.showMessage(fixture.getDescription());
        found = true;
      }
      if (!found) view.showMessage("You see nothing interesting about that.");
  }

  /**
   * Handling answer the puzzle.
   *
   * @param arg input string
   */
  private void handleAnswer(String arg) {
    if (arg == null) {
      view.showMessage("Answer what?");
      return;
    }
    boolean correct = model.answerPuzzle(arg);
    view.showMessage(correct ? "Puzzle solved!" : "That didn't work.");
  }

  /**
   * Handling the quit command loop.
   *
   * @return true if the user wants to quit
   */
  private boolean handleQuitCommand() {
    view.showMessage("Do you want to save before quitting? (Y/N)");
    while (true) {
      String response = scanner.nextLine().trim().toUpperCase();
      if (response.equals("Y")) return handleSave();
      if (response.equals("N")) return true;
      view.showMessage("Please enter 'Y' or 'N'.");
    }
  }

  /**
   * Private method for save the game.
   */
  private boolean handleSave() {
    view.showMessage("Enter file name to save (without .json):");
    String fileName = scanner.nextLine().trim();
    String fullPath = PathUtils.getSavePath(fileName);
    File saveFile = new File(fullPath);
    if (saveFile.exists()) {
      view.showMessage("A save file with that name already exists. Overwrite? (Y/N)");
      if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) return false;
    }
    return model.saveGame(fullPath);
  }

  /**
   * Private method for restoring the game.
   */
  private void handleRestore() {
    String path = selectSave();
    if (path != null && model.loadGame(path)) view.showMessage("Game restored!");
    else view.showMessage("Restore failed.");
  }

  /**
   * Handling complete the move command.
   *
   * @param dir the move command
   * @return the full name of the direction (North, West, South, East)
   */
  private String directionToFull(String dir) {
    return switch (dir) {
      case "N" -> "North";
      case "S" -> "South";
      case "E" -> "East";
      case "W" -> "West";
      default -> dir;
    };
  }

  /**
   * Help command.
   */
  private void handleHelp() {
    view.showMessage("\nMovement Commands\n---------------------");
    view.showMessage("  - " + NORTH_KEY + " / " + SOUTH_KEY + " / " + EAST_KEY + " / " + WEST_KEY + " : Move North/South/East/West");
    view.showMessage("\nItem Actions\n---------------------");
    view.showMessage("  - " + TAKE_KEY + " <item>    : Take an item");
    view.showMessage("  - " + DROP_KEY + " <item>    : Drop an item");
    view.showMessage("  - " + USE_KEY + " <item>     : Use an item");
    view.showMessage("\nPuzzle & Info\n---------------------");
    view.showMessage("  - " + INVENTORY_KEY + "           : Check inventory");
    view.showMessage("  - " + LOOK_KEY + "               : Look around the room");
    view.showMessage("  - " + EXAMINE_KEY + " <name>     : Examine an item or fixture");
    view.showMessage("  - " + ANSWER_KEY + " <answer>    : Answer a puzzle");
    view.showMessage("\nSystem Commands\n---------------------");
    view.showMessage("  - " + SAVE_KEY + "               : Save your progress");
    view.showMessage("  - " + RESTORE_KEY + "            : Restore a saved game");
    view.showMessage("  - " + QUIT_KEY + "               : Quit the game");
    view.showMessage("  - " + HELP_KEY + "               : Show this help menu again");
  }


  // ======= Retain the logic for selecting the archive =======
  /**
   * Allows the user to select a game map from available .json files.
   *
   * @return the path to the selected map file, or null if selection fails
   */
  private String selectMap() {
    File folder = new File(PathUtils.getMapPath(""));
    return pickJsonFile(folder.getParent(), "Available maps:", "Choose a map number:");
  }

  /**
   * Allows the user to select a saved game state from available .json files.
   *
   * @return the path to the selected save file, or null if selection fails
   */
  private String selectSave() {
    File folder = new File(PathUtils.getSavePath(""));
    return pickJsonFile(folder.getParent(), "Available saves:", "Choose a save number:");
  }

  /**
   * Picks a .json file from the given folder path by prompting the user.
   *
   * @param folderPath   the directory path where .json files are stored
   * @param promptTitle  the title message shown before listing files
   * @param promptInput  the prompt asking user to choose a file number
   * @return the full path of the selected .json file, or null if no file is selected
   */
  private String pickJsonFile(String folderPath, String promptTitle, String promptInput) {
    File folder = new File(folderPath);
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

    if (files == null || files.length == 0) {
      view.showMessage("No .json files found in: " + folderPath);
      return null;
    }

    view.showMessage(promptTitle);
    for (int i = 0; i < files.length; i++) {
      view.showMessage((i + 1) + ". " + files[i].getName());
    }
    view.showMessage(promptInput);

    while (true) {
      String input = scanner.nextLine().trim();
      try {
        int choice = Integer.parseInt(input);
        if (choice >= 1 && choice <= files.length) {
          return files[choice - 1].getPath();
        } else {
          view.showMessage("Invalid number. Try again:");
        }
      } catch (NumberFormatException e) {
        view.showMessage("Please enter a number.");
      }
    }
  }

  //
//  /**
//   * Handling save the current game state.
//   */
//  private void saveGameState() {
//    view.showMessage("Enter file name to save (without .json):");
//    String fileName = scanner.nextLine().trim();
//    String fullPath = PathUtils.getSavePath(fileName);
//    File saveFile = new File(fullPath);
//    if (saveFile.exists()) {
//      view.showMessage("A save file with that name already exists. Overwrite? (Y/N)");
//      String response = scanner.nextLine().trim();
//      if (!response.equalsIgnoreCase("Y")) {
//        view.showMessage("Save canceled. Returning to game...");
//        return;
//      }
//    }
//    boolean success = world.saveState(fullPath, player);
//
//    view.showMessage(success ? "Game saved to " + fileName : " Save failed.");
//  }

}
