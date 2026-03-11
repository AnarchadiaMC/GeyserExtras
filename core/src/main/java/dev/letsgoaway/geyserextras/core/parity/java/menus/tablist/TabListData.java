package dev.letsgoaway.geyserextras.core.parity.java.menus.tablist;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.utils.IdUtils;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.UUID;

public class TabListData {

    private final ExtrasPlayer player;
    @Getter
    private final LinkedHashMap<UUID, PlayerListEntry> players;
    @Setter
    @Getter
    private String header = " ";
    @Setter
    @Getter
    private String footer = " ";

    public TabListData(ExtrasPlayer player) {
        this.player = player;
        players = new LinkedHashMap<>();
    }

    // Cached reflection to avoid classloader conflicts with Component type
    private static final Method GET_DISPLAY_NAME;
    private static final Method CONVERT_MESSAGE;

    static {
        Method getDisplayName = null;
        Method convertMessage = null;
        try {
            // Try standard @Data accessor first (older mcprotocollib)
            try {
                getDisplayName = PlayerListEntry.class.getMethod("getDisplayName");
            } catch (NoSuchMethodException e) {
                // Try fluent @Accessors(fluent = true) accessor (newer mcprotocollib)
                getDisplayName = PlayerListEntry.class.getMethod("displayName");
            }
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
        GET_DISPLAY_NAME = getDisplayName;
        CONVERT_MESSAGE = convertMessage;
    }

    public static String getPlayerListName(PlayerListEntry entry) {
        try {
            Object displayName = GET_DISPLAY_NAME.invoke(entry);
            if (displayName == null && entry.getProfile() != null) {
                return ChatColor.DARK_GRAY + entry.getProfile().getName();
            }
            return (String) CONVERT_MESSAGE.invoke(null, displayName);
        } catch (Exception e) {
            // Fallback to profile name if reflection fails
            if (entry.getProfile() != null) {
                return ChatColor.DARK_GRAY + entry.getProfile().getName();
            }
            return "";
        }
    }

    public static String getPlayerListHead(PlayerListEntry entry) {
        return "https://starlightskins.lunareclipse.studio/render/pixel/" + getPlayerListID(entry.getProfileId()) + "/face?cameraWidth=8";
    }

    private static String getPlayerListID(UUID profileID) {
        long XUID = IdUtils.getBedrockXUID(profileID);
        if (XUID != -1) {
            // StarlightStudios needs a dot infront of it to recognize it as a XUID.
            return "." + XUID;
        }
        // If its not a Bedrock Player, then we can just return the UUID as a string.
        return profileID.toString();
    }

    // Ping icons are supplied by GeyserExtrasPack
    // Matches how Minecraft Java Edition chooses the icon to use
    public static String getPingIcon(PlayerListEntry entry) {
        String pingIcon = "\uF834";
        int ping = entry.getLatency();
        if (ping < 0) {
            pingIcon = "\uF835";
        } else if (ping < 150) {
            pingIcon = "\uF830";
        } else if (ping < 300) {
            pingIcon = "\uF831";
        } else if (ping < 600) {
            pingIcon = "\uF832";
        } else if (ping < 1000) {
            pingIcon = "\uF833";
        }
        return pingIcon;
    }
}
