package co.casterlabs.kaimen.ipc;

import java.util.concurrent.TimeUnit;

import co.casterlabs.kaimen.ipc.packet.KaimenIpcPacket;
import co.casterlabs.kaimen.ipc.packet.KaimenIpcPingPacket;

public class KaimenIPC {
    public static final KaimenIpcPacket PING_PACKET = new KaimenIpcPingPacket();
    public static final long PING_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    public static final long PING_TIMEOUT = PING_INTERVAL * 2;

}
