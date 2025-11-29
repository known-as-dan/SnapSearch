package tailor.snapsearch.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import tailor.snapsearch.client.mixins.ChatScreenAccessor;


public class SearchScreen extends Screen {

    private TextFieldWidget searchField;
    private boolean isBuy = true; // toggle buy/sell
    private final List<String> suggestions = new ArrayList<>();
    private int suggestion_pointer = 1;


    protected SearchScreen() {
        super(Text.literal("SnapSearch"));
    }

    protected void init() {
        int width = this.width / 2 - 100;
        int height = this.height / 3;

        searchField = new TextFieldWidget(this.textRenderer, width, height, 200, 20, Text.literal("Search item..."));
        searchField.setCentered(true);
        this.addSelectableChild(searchField);
        this.setFocused(searchField);

        searchField.setChangedListener((text) -> {
            if (userTyped) {
                updateSuggestions(text);
                userTyped = false;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int transparent_black = 0x7F000000;
        int less_transparent_black = 0x7F000000;
        int white = 0xFFFFFFFF;
        int green = 0xFF00FF00;
        int red = 0xFFFF0000;
        int marker_yellow = 0x99F5F504;

        context.fill(0, 0, this.width, this.height, transparent_black);
        context.fill(searchField.getX() - 2, searchField.getY() - 2, searchField.getX() + searchField.getWidth() + 2, searchField.getY() + searchField.getHeight() + 2, isBuy ? green : red);
        searchField.render(context, mouseX, mouseY, delta);

        // Render suggestions below the text field
        int suggestionY = searchField.getY() + searchField.getHeight() + 8; // 8 px padding
        int suggestionSize = 12;

        if (!suggestions.isEmpty()) {
            int maxVisible = (int) ((context.getScaledWindowHeight() * 0.8 - (searchField.getY() + searchField.getHeight() + 8)) / suggestionSize);
            int page = (suggestion_pointer - 1) / maxVisible;
            int listPointer = suggestion_pointer - page * maxVisible;
            int visible = Math.clamp(suggestions.size() - (long) page * maxVisible, 1, maxVisible);

            context.fill(
                    searchField.getX(),
                    suggestionY - 3,
                    searchField.getX() + searchField.getWidth(),
                    suggestionY + visible * suggestionSize,
                    less_transparent_black
            );

            for (int i = 0; i < maxVisible; i++) {
                if (i >= visible) { break; }

                String suggestion = suggestions.get(page * maxVisible + i);
                if (i == listPointer - 1) {
                    context.fill(
                            searchField.getX(),
                            suggestionY - 2,
                            searchField.getX() + searchField.getWidth(),
                            suggestionY + 10,
                            marker_yellow
                    );
                }
                context.drawCenteredTextWithShadow(this.textRenderer, prettifyItemName(suggestion), this.width / 2, suggestionY, white);

                suggestionY += 12;
            }
        }

        // Show buy/sell mode
        context.drawCenteredTextWithShadow(this.textRenderer, isBuy ? "BUY" : "SELL", this.width / 2, searchField.getY() - 14, isBuy ? green : red);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        int keyCode = key.getKeycode();
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            executeCommand(searchField.getText());
            this.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) {
            isBuy = !isBuy; // toggle buy/sell
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_DOWN) {
            if (suggestion_pointer < suggestions.size()) {
                suggestion_pointer++;
            } else {
                suggestion_pointer = 1;
            }
            if (suggestion_pointer <= suggestions.size() && suggestion_pointer >= 1) {
                searchField.setText(suggestions.get(suggestion_pointer - 1));
            }

            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            if (suggestion_pointer >= 2) {
                suggestion_pointer--;
                searchField.setText(suggestions.get(suggestion_pointer - 1));
            }
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            userTyped = true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        return super.keyPressed(key);
    }

    private void executeCommand(String itemName) {
        if (!itemName.isEmpty() && this.client != null) {
            String command = "/finditem " + (isBuy ? "buy" : "sell") + " " + itemName.toUpperCase();

            this.client.openChatScreen(ChatHud.ChatMethod.MESSAGE);
            if (this.client.currentScreen instanceof ChatScreen chatScreen) {
                // Access chat field
                TextFieldWidget input = ((ChatScreenAccessor) chatScreen).snapsearch$getInput();

                // Set the command in the input
                input.setText(command);

                // Send it, add to history
                chatScreen.sendMessage(input.getText(), true);

                input.setText("");
            }
        }
    }

    private void updateSuggestions(String text) {
        suggestions.clear();
        suggestion_pointer = 0;

        if (text.isEmpty()) { return; }

        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item); // minecraft:diamond
            String name = id.getPath().toUpperCase();    // just the path

            // Optional: skip unwanted items
            if (name.contains("DEBUG") || name.contains("COMMAND") || name.contains("BARRIER") || name.equalsIgnoreCase("AIR")) continue;

            if (name.contains(text.toUpperCase().replace(" ", "_"))) {
                suggestions.add(name);
            }
        }
    }

    boolean userTyped = false;
    @Override
    public boolean charTyped(CharInput input) {
        userTyped = true;
        return super.charTyped(input);
    }

    public static String prettifyItemName(String name) {
        String[] words = name.split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }
        return String.join(" ", words);
    }
}