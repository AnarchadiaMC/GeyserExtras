package dev.letsgoaway.geyserextras.core.parity.java.menus.serverlinks;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.utils.IsAvailable;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.ServerLink;

import java.lang.reflect.Method;
import java.util.List;

public class ServerLinksData {
    @Setter
    @Getter
    public List<ServerLink> serverLinks = List.of();

    private final ExtrasPlayer player;

    public ServerLinksData(ExtrasPlayer player) {
        this.player = player;
    }

    // Cached reflection to avoid classloader conflicts with Component type
    private static final Method CONVERT_MESSAGE;

    static {
        Method convertMessage = null;
        try {
            for (Method m : MessageTranslator.class.getDeclaredMethods()) {
                if (m.getName().equals("convertMessage") && m.getParameterCount() == 1) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params[0].getSimpleName().equals("Component")) {
                        convertMessage = m;
                        convertMessage.setAccessible(true);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        CONVERT_MESSAGE = convertMessage;
    }

    public static String getLinkText(ServerLink link, ExtrasPlayer player) {
        if (link.knownType() != null) {
            switch (link.knownType()) {
                // life is unfair
                case BUG_REPORT -> {
                    return player.translate("known_server_link.report_bug");
                }
                default -> {
                    return player.translate("known_server_link." + link.knownType().name().toLowerCase());
                }
            }
        } else {
            if (IsAvailable.adventure() && CONVERT_MESSAGE != null) {
                try {
                    return (String) CONVERT_MESSAGE.invoke(null, link.unknownType());
                } catch (Exception e) {
                    return "error";
                }
            } else {
                return "adventure_not_found";
            }
        }
    }
}
