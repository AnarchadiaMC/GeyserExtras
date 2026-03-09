package dev.letsgoaway.geyserextras.core.injectors.java;

import dev.letsgoaway.geyserextras.core.utils.ReflectionAPI;
import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.injectors.GeyserHandler;
import dev.letsgoaway.geyserextras.core.utils.IsAvailable;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;

import java.lang.reflect.Method;

// NOTE: If Geyser ever implements this, make sure to extend their translator!
@Translator(packet = ClientboundTabListPacket.class)
public class JavaTabListInjector extends PacketTranslator<ClientboundTabListPacket> {

    // Cached reflection handles to avoid classloader constraint violations.
    // GeyserExtras and Geyser both shade net.kyori.adventure under the same relocated
    // package but are loaded by different classloaders, making their Component classes
    // incompatible at link time. Using reflection bypasses the JVM linkage check.
    private static final Method GET_HEADER;
    private static final Method GET_FOOTER;
    private static final Method CONVERT_MESSAGE;

    static {
        Method getHeader = null;
        Method getFooter = null;
        Method convertMessage = null;
        try {
            getHeader = ClientboundTabListPacket.class.getMethod("getHeader");
            getFooter = ClientboundTabListPacket.class.getMethod("getFooter");
            // Find the convertMessage(GeyserSession, Component) overload via reflection
            // to avoid referencing the Component type directly in our bytecode.
            for (Method m : MessageTranslator.class.getDeclaredMethods()) {
                if (m.getName().equals("convertMessage") && m.getParameterCount() == 2) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params[0] == GeyserSession.class
                            && params[1].getSimpleName().equals("Component")) {
                        convertMessage = m;
                        convertMessage.setAccessible(true);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        GET_HEADER = getHeader;
        GET_FOOTER = getFooter;
        CONVERT_MESSAGE = convertMessage;
    }

    @Override
    public void translate(GeyserSession session, ClientboundTabListPacket packet) {
        if (!IsAvailable.adventure() || GET_HEADER == null || GET_FOOTER == null || CONVERT_MESSAGE == null) {
            return;
        }
        try {
            Object header = GET_HEADER.invoke(packet);
            Object footer = GET_FOOTER.invoke(packet);
            String headerStr = (String) CONVERT_MESSAGE.invoke(null, session, header);
            String footerStr = (String) CONVERT_MESSAGE.invoke(null, session, footer);
            ExtrasPlayer player = ExtrasPlayer.get(session);
            player.getTabListData().setHeader(headerStr);
            player.getTabListData().setFooter(footerStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
