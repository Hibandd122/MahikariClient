package mahikariui.core.commands;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import mahikariui.core.Constants;
import mahikariui.core.background.Background;
import mahikariui.core.background.BackgroundBuilder;
import mahikariui.core.config.Config;
import mahikariui.core.config.screen.ConfigScreen;

public class MahikariUICommand {
    private static final SuggestionProvider<ServerCommandSource> BACKGROUND_SUGGESTIONS = (context, builder) -> {
        String[] backgrounds;
        for (String bg : backgrounds = Background.getAvailableBackgrounds()) {
            builder.suggest(bg);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<ServerCommandSource> HELP_TOPIC_SUGGESTIONS = (context, builder) -> {
        String[] topics;
        for (String topic : topics = new String[]{"background", "config", "commands"}) {
            builder.suggest(topic);
        }
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> helpCmd = CommandManager.literal("help").executes(context -> {
            MahikariUICommand.showMainHelp((CommandContext<ServerCommandSource>)context);
            return 1;
        })
            .then(CommandManager.argument("topic", (ArgumentType)StringArgumentType.word()).suggests(HELP_TOPIC_SUGGESTIONS).executes(context -> {
                String topic = StringArgumentType.getString((CommandContext)context, "topic");
                MahikariUICommand.showTopicHelp((CommandContext<ServerCommandSource>)context, topic);
                return 1;
            }));

        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("mahikariui").executes(context -> {
            MahikariUICommand.showMainHelp((CommandContext<ServerCommandSource>)context);
            return 1;
        });
        command.then(CommandManager.literal("background").executes(context -> {
            MahikariUICommand.showBackgroundInfo((CommandContext<ServerCommandSource>)context);
            return 1;
        })
            .then(CommandManager.literal("set").then(CommandManager.argument("name", (ArgumentType)StringArgumentType.word()).suggests(BACKGROUND_SUGGESTIONS).executes(context -> {
                String bgName = StringArgumentType.getString((CommandContext)context, "name");
                MahikariUICommand.setBackground((CommandContext<ServerCommandSource>)context, bgName);
                return 1;
            })))
            .then(CommandManager.literal("list").executes(context -> {
                MahikariUICommand.listBackgrounds((CommandContext<ServerCommandSource>)context);
                return 1;
            }))
            .then(CommandManager.literal("info").executes(context -> {
                MahikariUICommand.showBackgroundInfo((CommandContext<ServerCommandSource>)context);
                return 1;
            }))
            .then(CommandManager.literal("fps").then(CommandManager.argument("fps", (ArgumentType)StringArgumentType.word()).executes(context -> {
                String fpsStr = StringArgumentType.getString((CommandContext)context, "fps");
                MahikariUICommand.setBackgroundFPS((CommandContext<ServerCommandSource>)context, fpsStr);
                return 1;
            })))
            .then(CommandManager.literal("config").executes(context -> {
                MahikariUICommand.openBackgroundConfig((CommandContext<ServerCommandSource>)context);
                return 1;
            }))
        );
        command.then(CommandManager.literal("config").executes(context -> {
            MahikariUICommand.openBackgroundConfig((CommandContext<ServerCommandSource>)context);
            return 1;
        })
            .then(CommandManager.literal("reload").executes(context -> {
                MahikariUICommand.reloadConfig((CommandContext<ServerCommandSource>)context);
                return 1;
            }))
            .then(CommandManager.literal("reset").executes(context -> {
                MahikariUICommand.resetConfig((CommandContext<ServerCommandSource>)context);
                return 1;
            }))
        );
        command.then(CommandManager.literal("reload").executes(context -> {
            MahikariUICommand.reloadConfig((CommandContext<ServerCommandSource>)context);
            return 1;
        }));
        command.then(helpCmd);
        dispatcher.register(command);
    }

    private static void showMainHelp(CommandContext<ServerCommandSource> context) {
        MahikariUICommand.sendHeader(context, "MahikariUI Commands");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui help [topic] \u00a77- Show detailed help");
        MahikariUICommand.sendMessage(context, "");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background \u00a77- Background management");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui config \u00a77- Configuration management");
        MahikariUICommand.sendMessage(context, "");
        MahikariUICommand.sendMessage(context, "\u00a77Use \u00a7f/mahikariui help <topic> \u00a77for detailed information");
    }

    private static void showTopicHelp(CommandContext<ServerCommandSource> context, String topic) {
        switch (topic.toLowerCase()) {
            case "background": {
                MahikariUICommand.showBackgroundHelp(context);
                break;
            }
            case "config": {
                MahikariUICommand.showConfigHelp(context);
                break;
            }
            case "commands": {
                MahikariUICommand.showMainHelp(context);
                break;
            }
            default: {
                MahikariUICommand.sendMessage(context, "\u00a7cUnknown help topic: " + topic, true);
                MahikariUICommand.sendMessage(context, "\u00a77Available topics: background, config, commands");
            }
        }
    }

    private static void showBackgroundHelp(CommandContext<ServerCommandSource> context) {
        MahikariUICommand.sendHeader(context, "Background Commands");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background \u00a77- Show current background info");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background list \u00a77- List available backgrounds");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background set <name> \u00a77- Change background");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background fps <value> \u00a77- Set animation FPS (1-60)");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background config \u00a77- Open background config GUI");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui background info \u00a77- Show detailed background info");
    }

    private static void showConfigHelp(CommandContext<ServerCommandSource> context) {
        MahikariUICommand.sendHeader(context, "Config Commands");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui config \u00a77- Open configuration GUI");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui config reload \u00a77- Reload configuration from disk");
        MahikariUICommand.sendMessage(context, "\u00a7b/mahikariui config reset \u00a77- Reset all settings to default");
    }

    private static void showBackgroundInfo(CommandContext<ServerCommandSource> context) {
        String currentBg = BackgroundBuilder.getBackgroundName();
        int frameCount = BackgroundBuilder.getFrameCount();
        int currentFrame = BackgroundBuilder.getCurrentFrameIndex();
        boolean isAnimated = BackgroundBuilder.isAnimated();
        int fps = Config.getInstance().getBackgroundFrame();
        MahikariUICommand.sendHeader(context, "Current Background");
        MahikariUICommand.sendMessage(context, "\u00a76Name: \u00a7b" + currentBg);
        MahikariUICommand.sendMessage(context, "\u00a76Type: \u00a7b" + (isAnimated ? "Animated" : "Static"));
        if (isAnimated) {
            MahikariUICommand.sendMessage(context, "\u00a76Frames: \u00a7b" + frameCount);
            MahikariUICommand.sendMessage(context, "\u00a76Current Frame: \u00a7b" + (currentFrame + 1) + "/" + frameCount);
            MahikariUICommand.sendMessage(context, "\u00a76FPS: \u00a7b" + fps);
        }
        MahikariUICommand.sendMessage(context, "\u00a76Path: \u00a77" + BackgroundBuilder.getBackgroundPath());
    }

    private static void listBackgrounds(CommandContext<ServerCommandSource> context) {
        String[] backgrounds = Background.getAvailableBackgrounds();
        String currentBg = BackgroundBuilder.getBackgroundName();
        MahikariUICommand.sendHeader(context, "Available Backgrounds");
        if (backgrounds.length == 0) {
            MahikariUICommand.sendMessage(context, "\u00a7cNo backgrounds found!");
            return;
        }
        for (String bg : backgrounds) {
            boolean isCurrent = bg.equalsIgnoreCase(currentBg);
            String marker = isCurrent ? " \u00a7a\u2713 \u00a77(current)" : "";
            MahikariUICommand.sendMessage(context, "\u00a7b" + bg + marker);
        }
        MahikariUICommand.sendMessage(context, "");
        MahikariUICommand.sendMessage(context, "\u00a77Use \u00a7f/mahikariui background set <name> \u00a77to change");
    }

    private static void setBackground(CommandContext<ServerCommandSource> context, String name) {
        try {
            Config.getInstance().setBackground(name);
            BackgroundBuilder.selectBackground(name);
            MahikariUICommand.sendMessage(context, "\u00a76Background set to: \u00a7b" + name);
        }
        catch (Exception e) {
            MahikariUICommand.sendMessage(context, "\u00a7cFailed to set background: " + e.getMessage(), true);
        }
    }

    private static void setBackgroundFPS(CommandContext<ServerCommandSource> context, String fpsStr) {
        try {
            int fps = Integer.parseInt(fpsStr);
            if (fps < 1 || fps > 60) {
                MahikariUICommand.sendMessage(context, "\u00a7cFPS must be between 1 and 60", true);
                return;
            }
            Config.getInstance().setBackgroundFrame(fps);
            MahikariUICommand.sendMessage(context, "\u00a76Background FPS set to: \u00a7b" + fps);
        }
        catch (NumberFormatException e) {
            MahikariUICommand.sendMessage(context, "\u00a7cInvalid FPS value: " + fpsStr, true);
        }
    }

    private static void openBackgroundConfig(CommandContext<ServerCommandSource> context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen((Screen)new ConfigScreen(mc.currentScreen));
        MahikariUICommand.sendMessage(context, "\u00a76Opening background configuration...");
    }

    private static void reloadConfig(CommandContext<ServerCommandSource> context) {
        try {
            Config.getInstance().load();
            BackgroundBuilder.selectBackground(Config.getInstance().getBackground());
            MahikariUICommand.sendMessage(context, "\u00a7aConfiguration reloaded successfully!");
        }
        catch (Exception e) {
            MahikariUICommand.sendMessage(context, "\u00a7cFailed to reload config: " + e.getMessage(), true);
        }
    }

    private static void resetConfig(CommandContext<ServerCommandSource> context) {
        try {
            Config.getInstance().setBackground("default");
            Config.getInstance().setBackgroundFrame(20);
            BackgroundBuilder.selectBackground("default");
            MahikariUICommand.sendMessage(context, "\u00a7aConfiguration reset to defaults!");
        }
        catch (Exception e) {
            MahikariUICommand.sendMessage(context, "\u00a7cFailed to reset config: " + e.getMessage(), true);
        }
    }

    private static void sendHeader(CommandContext<ServerCommandSource> context, String title) {
        MahikariUICommand.sendMessage(context, "\u00a76========== \u00a7b" + title + " \u00a76==========");
    }

    private static void sendMessage(CommandContext<ServerCommandSource> context, String message) {
        MahikariUICommand.sendMessage(context, message, false);
    }

    private static void sendMessage(CommandContext<ServerCommandSource> context, String message, boolean isError) {
        net.minecraft.text.MutableText text = net.minecraft.text.Text.literal((String)message);
        if (isError) {
            text = text.copy().fillStyle(Style.EMPTY.withColor(Formatting.RED));
        }
        ((ServerCommandSource)context.getSource()).sendMessage((net.minecraft.text.Text)text);
    }
}
