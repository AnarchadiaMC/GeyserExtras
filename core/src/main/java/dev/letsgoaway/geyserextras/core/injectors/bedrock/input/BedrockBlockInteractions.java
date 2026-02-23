package dev.letsgoaway.geyserextras.core.injectors.bedrock.input;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.preferences.bindings.Remappable;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;

public class BedrockBlockInteractions {
    public static void translate(GeyserSession session, List<PlayerBlockActionData> playerActions) {
        // Send book update before any player action
        session.getBookEditCache().checkForSend();

        for (PlayerBlockActionData blockActionData : playerActions) {
            handle(session, blockActionData);
        }
    }

    private static void handle(GeyserSession session, PlayerBlockActionData blockActionData) {
        PlayerActionType action = blockActionData.getAction();
        ExtrasPlayer player = ExtrasPlayer.get(session);

        if (action == PlayerActionType.DROP_ITEM) {
            if (session.isSneaking()) {
                player.getPreferences().runAction(Remappable.SNEAK_DROP);
                session.getPlayerInventoryHolder().updateInventory();
            }
        }
    }
}
