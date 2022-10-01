package co.casterlabs.kaimen.ipc.packet;

public class KaimenIpcPingPacket extends KaimenIpcPacket {

    @Override
    public KaimenIpcPacketType getType() {
        return KaimenIpcPacketType.PING;
    }

}
