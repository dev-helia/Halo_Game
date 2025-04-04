package controller;

import model.core.WorldEngine;
import org.junit.Before;
import org.junit.Test;
import view.View;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The type Game controller test.
 */
public class GameControllerTest {
  private WorldEngine engine;
  private View view;

  /**
   * Sets .
   */
  @Before
  public void setup() {
    engine = new WorldEngine();
    view = new MockView();
  }

  // Helper to make Readable from input string
  private Readable simulateInput(String input) {
    return new InputStreamReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
    );
  }

  // Helper to get printed messages
  private List<String> getOutput() {
    return ((MockView) view).getMessages();
  }

  /**
   * Test start new game look and quit no save.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStartNewGame_LookAndQuit_NoSave() throws Exception {
    String input = "NEW\n1\nXiao Hong\nL\nQ\nN\n";
    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    assertNotNull(controller.getPlayer());
    assertEquals("Xiao Hong", controller.getPlayer().getName());

    List<String> output = getOutput();
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Health Status")));
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Rendered")));
  }

  /**
   * Test start new game move east quit with save.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStartNewGame_MoveEast_QuitWithSave() throws Exception {
    String input = String.join("\n",
            "NEW",          // Start a new game
            "1",            // Choose Align_Quest_Game_Elements.json
            "Xiao Hong",    // Player name
            "E",            // Move East
            "Q",            // Quit game
            "Y",            // Want to save
            "testSave",        // Save name
            "Y"             // Confirm overwrite!
    );

    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    assertNotNull(controller.getPlayer());
    assertEquals("Xiao Hong", controller.getPlayer().getName());

    MockView mockView = (MockView) view;
    List<String> output = mockView.getMessages();

    assertTrue(
            "Expected save confirmation message",
            output.stream().anyMatch(msg -> msg.contains("Game saved to "))
    );
  }


  /**
   * Test start and use help command.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStartAndUseHelpCommand() throws Exception {
    String input = "NEW\n1\nLia\nHELP\nQ\nN\n";
    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    assertNotNull(controller.getPlayer());
    assertEquals("Lia", controller.getPlayer().getName());

    List<String> output = getOutput();
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Movement Commands")));
  }

  /**
   * Test inventory empty.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInventoryEmpty() throws Exception {
    String input = "NEW\n1\nAda\nI\nQ\nN\n";
    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    assertNotNull(controller.getPlayer());
    assertEquals("Ada", controller.getPlayer().getName());

    List<String> output = getOutput();
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Your inventory is empty")));
  }

  /**
   * Test unknown command handled.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnknownCommandHandled() throws Exception {
    String input = "NEW\n1\nLuna\nWHAT\nQ\nN\n";
    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    List<String> output = getOutput();
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Unknown command")));
  }

  /**
   * Test puzzle answer fail and success.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPuzzleAnswerFailAndSuccess() throws Exception {
    String input = String.join("\n",
            "NEW",          // Start new game
            "5",            // Map: Simple_Hallway
            "Mimi",         // Player name
            "N",            // Go to room 2 (LOCK puzzle triggers)
            "T Key",     // Take Key (now in correct room)
            "A Key",   // Solve LOCK
            "N",            // Go to room 3
            "T Lamp",    // Take Lamp
            "N",            // Go to room 4 (DARKNESS triggers)
            "A Lamp",  // Solve DARKNESS
            "Q", "N"        // Quit without saving
    );


    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    List<String> output = getOutput();
    output.forEach(System.out::println);

    assertTrue(output.stream().anyMatch(msg -> msg.contains("That didn't work")));
    assertTrue(output.stream().anyMatch(msg -> msg.contains("Puzzle solved")));
  }

  /**
   * Test examine unknown item.
   *
   * @throws Exception the exception
   */
  @Test
  public void testExamineUnknownItem() throws Exception {
    String input = "NEW\n1\nLily\nX unknown\nQ\nN\n";
    GameController controller = new GameController(engine, view, simulateInput(input));
    controller.startGame();

    List<String> output = getOutput();
    assertTrue(output.stream().anyMatch(msg -> msg.contains("nothing interesting")));
  }

}
