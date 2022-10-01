package co.casterlabs.kaimen.ipc.packet;

import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;

public abstract class KaimenIpcPacket {

    public abstract KaimenIpcPacketType getType();

    @JsonSerializationMethod("type")
    private JsonElement $serialize_type() {
        return new JsonString(this.getType().name());
    }

    public static enum KaimenIpcPacketType {
        PRINT,
        PING,
        ;
    }

}
