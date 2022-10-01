package co.casterlabs.kaimen.ipc.packet;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonClass(exposeAll = true)
public class KaimenIpcPrintPacket extends KaimenIpcPacket {
    private String bytes;
    private PrintChannel channel;

    @Override
    public KaimenIpcPacketType getType() {
        return KaimenIpcPacketType.PRINT;
    }

    public static enum PrintChannel {
        STDOUT,
        STDERR;

    }

}
