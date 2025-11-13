package rocamocha.reactivemusic.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import rocamocha.reactivemusic.ReactiveMusicDebug.TextBuilder;
import rocamocha.reactivemusic.api.ReactiveMusicAPI;
import rocamocha.reactivemusic.api.audio.GainSupplier;
import rocamocha.reactivemusic.api.audio.ReactivePlayer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.util.Formatting;

public class PlayerCommandHandlers {

    public static int playerList(CommandContext<FabricClientCommandSource> ctx) {
        TextBuilder playerList = new TextBuilder();

        playerList.header("AUDIO PLAYERS");
        for (ReactivePlayer player : ReactiveMusicAPI.audioManager().getAll()) {
            playerList.line(player.id(), Formatting.AQUA);
        }
        
        ctx.getSource().sendFeedback(playerList.build());
        return 1;
    }

    public static int playerInfo(CommandContext<FabricClientCommandSource> ctx) {


        String id = StringArgumentType.getString(ctx, "namespace") + ":" + StringArgumentType.getString(ctx, "path");
        ReactivePlayer player = ReactiveMusicAPI.audioManager().get(id);
        TextBuilder playerInfo = new TextBuilder();

        playerInfo.header("AUDIO PLAYER INFO")

        .line("id", player.id(), Formatting.AQUA)
        .line("isPlaying", player.isPlaying() ? "YES" : "NO", player.isPlaying() ? Formatting.GREEN : Formatting.GRAY)
        .line("isPaused", player.isPaused() ? "YES" : "NO", player.isPaused() ? Formatting.YELLOW : Formatting.GRAY)
        .line("position", Integer.toString(player.getPosition()) + " frames", Formatting.AQUA)
        .line("stopOnFadeOut", player.stopOnFadeOut() ? "YES" : "NO", player.stopOnFadeOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("resetOnFadeOut", player.resetOnFadeOut() ? "YES" : "NO", player.resetOnFadeOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("gainSuppliers", "", Formatting.WHITE);

        player.getGainSuppliers().forEach((supplierId, gainSupplier) -> {
            playerInfo.line(" --> " + supplierId, Float.toString(gainSupplier.supplyComputedPercent()), gainSupplier.supplyComputedPercent() > 0 ? Formatting.LIGHT_PURPLE : Formatting.GRAY);
        });
        
        ctx.getSource().sendFeedback(playerInfo.build());
        return 1;
    }

    public static int gainSupplierInfo(CommandContext<FabricClientCommandSource> ctx) {

        String id = StringArgumentType.getString(ctx, "namespace") + ":" + StringArgumentType.getString(ctx, "path");
        String gainSupplierId = StringArgumentType.getString(ctx, "gainSupplierId");
        TextBuilder supplierInfo = new TextBuilder();
        ReactivePlayer player = ReactiveMusicAPI.audioManager().get(id);
        GainSupplier gainSupplier = player.getGainSuppliers().get(gainSupplierId);

        supplierInfo.header("GAIN SUPPLIER")
        .line("player", id, Formatting.WHITE)
        .line("id", gainSupplierId, Formatting.AQUA)
        .newline()
        .line("computedPercent", Float.toString(gainSupplier.supplyComputedPercent()), Formatting.LIGHT_PURPLE)
        .line("fadeStart", Float.toString(gainSupplier.getFadeStart()), Formatting.AQUA)
        .line("fadeTarget", Float.toString(gainSupplier.getFadeTarget()), Formatting.AQUA)
        .line("fadeDuration", Integer.toString(gainSupplier.getFadeDuration()), Formatting.BLUE)
        .line("isFadingOut", gainSupplier.isFadingOut() ? "YES" : "NO", gainSupplier.isFadingOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("isFadingIn", gainSupplier.isFadingIn() ? "YES" : "NO", gainSupplier.isFadingIn() ? Formatting.GREEN : Formatting.GRAY);

        ctx.getSource().sendFeedback(supplierInfo.build());
        return 1;
    }
    
    public static int pause(CommandContext<FabricClientCommandSource> ctx) {
        ReactivePlayer musicPlayer = ReactiveMusicAPI.audioManager().get("reactive:music");
        ReactivePlayer overlayPlayer = ReactiveMusicAPI.audioManager().get("reactive:overlay");
        
        TextBuilder feedback = new TextBuilder();
        feedback.header("PAUSING PLAYBACK");
        
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
            feedback.line("Music player paused at frame: " + musicPlayer.getPosition(), Formatting.GREEN);
        }
        
        if (overlayPlayer != null && overlayPlayer.isPlaying()) {
            overlayPlayer.pause();
            feedback.line("Overlay player paused at frame: " + overlayPlayer.getPosition(), Formatting.GREEN);
        }
        
        ctx.getSource().sendFeedback(feedback.build());
        return 1;
    }
    
    public static int resume(CommandContext<FabricClientCommandSource> ctx) {
        ReactivePlayer musicPlayer = ReactiveMusicAPI.audioManager().get("reactive:music");
        ReactivePlayer overlayPlayer = ReactiveMusicAPI.audioManager().get("reactive:overlay");
        
        TextBuilder feedback = new TextBuilder();
        feedback.header("RESUMING PLAYBACK");
        
        if (musicPlayer != null && musicPlayer.isPaused()) {
            musicPlayer.resume();
            feedback.line("Music player resumed from frame: " + musicPlayer.getPosition(), Formatting.GREEN);
        }
        
        if (overlayPlayer != null && overlayPlayer.isPaused()) {
            overlayPlayer.resume();
            feedback.line("Overlay player resumed from frame: " + overlayPlayer.getPosition(), Formatting.GREEN);
        }
        
        ctx.getSource().sendFeedback(feedback.build());
        return 1;
    }
    
    public static int rewind(CommandContext<FabricClientCommandSource> ctx) {
        int frames = IntegerArgumentType.getInteger(ctx, "frames");
        ReactivePlayer musicPlayer = ReactiveMusicAPI.audioManager().get("reactive:music");
        ReactivePlayer overlayPlayer = ReactiveMusicAPI.audioManager().get("reactive:overlay");
        
        TextBuilder feedback = new TextBuilder();
        feedback.header("REWIND");
        
        int rewound = 0;
        
        // Rewind music player
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            if (musicPlayer instanceof rocamocha.reactivemusic.impl.audio.RMPlayer) {
                rocamocha.reactivemusic.impl.audio.RMPlayer rmPlayer = 
                    (rocamocha.reactivemusic.impl.audio.RMPlayer) musicPlayer;
                rmPlayer.rewind(frames);
                feedback.line("Music player: Rewinding " + frames + " frames", Formatting.GREEN);
                rewound++;
            }
        }
        
        // Rewind overlay player
        if (overlayPlayer != null && overlayPlayer.isPlaying()) {
            if (overlayPlayer instanceof rocamocha.reactivemusic.impl.audio.RMPlayer) {
                rocamocha.reactivemusic.impl.audio.RMPlayer rmPlayer = 
                    (rocamocha.reactivemusic.impl.audio.RMPlayer) overlayPlayer;
                rmPlayer.rewind(frames);
                feedback.line("Overlay player: Rewinding " + frames + " frames", Formatting.GREEN);
                rewound++;
            }
        }
        
        if (rewound == 0) {
            feedback.line("No players currently playing", Formatting.GRAY);
        }
        
        ctx.getSource().sendFeedback(feedback.build());
        return 1;
    }
    
    public static int fastForward(CommandContext<FabricClientCommandSource> ctx) {
        int frames = IntegerArgumentType.getInteger(ctx, "frames");
        ReactivePlayer musicPlayer = ReactiveMusicAPI.audioManager().get("reactive:music");
        ReactivePlayer overlayPlayer = ReactiveMusicAPI.audioManager().get("reactive:overlay");
        
        TextBuilder feedback = new TextBuilder();
        feedback.header("FAST-FORWARD");
        
        int skipped = 0;
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.skip(frames);
            feedback.line("Music player: Skipping " + frames + " frames forward", Formatting.GREEN);
            skipped++;
        }
        
        if (overlayPlayer != null && overlayPlayer.isPlaying()) {
            overlayPlayer.skip(frames);
            feedback.line("Overlay player: Skipping " + frames + " frames forward", Formatting.GREEN);
            skipped++;
        }
        
        if (skipped == 0) {
            feedback.line("No players currently playing", Formatting.GRAY);
        }
        
        ctx.getSource().sendFeedback(feedback.build());
        return 1;
    }
    
}
