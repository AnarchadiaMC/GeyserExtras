package dev.letsgoaway.geyserextras.core.injectors.bedrock;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.preferences.bindings.Action;
import dev.letsgoaway.geyserextras.core.preferences.bindings.Remappable;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.bedrock.entity.player.BedrockInteractTranslator;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static dev.letsgoaway.geyserextras.core.GeyserExtras.GE;

@Translator(packet = InteractPacket.class)
public class BedrockInteractInjector extends BedrockInteractTranslator {

    // Cached reflection to avoid classloader conflicts with Geyser's Entity class
    private static final Method GET_GEYSER_ID;

    static {
        Method getGeyserId = null;
        try {
            Class<?> entityClass = Class.forName("org.geysermc.geyser.entity.type.Entity");
            try {
                getGeyserId = entityClass.getMethod("getGeyserId");
            } catch (NoSuchMethodException e) {
                // Try newer GeyserMC method name
                try {
                    getGeyserId = entityClass.getMethod("geyserId");
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (getGeyserId != null) {
                getGeyserId.setAccessible(true);
            }
        } catch (Exception ignored) {
        }
        GET_GEYSER_ID = getGeyserId;
    }

    @Override
    public void translate(GeyserSession session, InteractPacket packet) {
        ExtrasPlayer player = ExtrasPlayer.get(session);
        if (!packet.getAction().equals(InteractPacket.Action.OPEN_INVENTORY)) {
            super.translate(session, packet);
        } else {
            if ((player.getPreferences().isEnableDoubleClickShortcut() && GE.getConfig().isEnableGeyserExtrasMenu())
                    && !(player.getSession().isSneaking() && !player.getPreferences().isDefault(Remappable.SNEAK_INVENTORY))) {
                // Double click
                if (player.getPreferences().getDoubleClickMS() > System.currentTimeMillis() - player.getLastInventoryClickTime()) {
                    if (player.getDoubleClickShortcutFuture() != null && !player.getDoubleClickShortcutFuture().isCancelled() && !player.getDoubleClickShortcutFuture().isDone()) {
                        player.getDoubleClickShortcutFuture().cancel(false);
                        // open menu
                        Action.OPEN_GE_MENU.run(player);
                    } else {
                        player.setLastInventoryClickTime(System.currentTimeMillis());
                        player.setDoubleClickShortcutFuture(session.scheduleInEventLoop(() -> {
                            player.getPreferences().runAction(Remappable.OPEN_INVENTORY);
                        }, player.getPreferences().getDoubleClickMS() + 20, TimeUnit.MILLISECONDS));
                    }
                    return;
                } else {
                    player.setLastInventoryClickTime(System.currentTimeMillis());
                    player.setDoubleClickShortcutFuture(session.scheduleInEventLoop(() -> {
                        player.getPreferences().runAction(Remappable.OPEN_INVENTORY);
                    }, player.getPreferences().getDoubleClickMS() + 20, TimeUnit.MILLISECONDS));
                    return;
                }
            }
            Remappable bind = player.getSession().isSneaking() ? Remappable.SNEAK_INVENTORY : Remappable.OPEN_INVENTORY;
            if (player.getPreferences().isDefault(bind) || player.getPreferences().getAction(bind).equals(Action.OPEN_INVENTORY)) {
                super.translate(session, packet);
                return;
            }
            Entity entity;
            try {
                long geyserId = GET_GEYSER_ID != null ? (long) GET_GEYSER_ID.invoke(session.getPlayerEntity()) : session.getPlayerEntity().getEntityId();
                if (packet.getRuntimeEntityId() == geyserId) {
                    entity = session.getPlayerEntity();
                } else {
                    entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                }
            } catch (Exception e) {
                // Fallback to entityId if reflection fails
                if (packet.getRuntimeEntityId() == session.getPlayerEntity().getEntityId()) {
                    entity = session.getPlayerEntity();
                } else {
                    entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                }
            }
            if (entity == null) {
                return;
            }
            player.getPreferences().getAction(bind).run(player);
        }
    }
}
