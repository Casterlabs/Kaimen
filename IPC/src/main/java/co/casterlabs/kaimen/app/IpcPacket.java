package co.casterlabs.kaimen.app;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.AllArgsConstructor;

abstract class IpcPacket {

    public abstract IpcPacketType getType();

    @JsonSerializationMethod("type")
    private JsonElement $serialize_type() {
        return new JsonString(this.getType().name());
    }

    @AllArgsConstructor
    public static enum IpcPacketType {
        PRINT(IpcPacketPrint.class),
        CLIENT_READY(IpcPacketClientReady.class),
        INVOKE(IpcPacketInvoke.class),
        INIT(IpcPacketInit.class),
        INVOCATION_RESULT(IpcPacketInvocationResult.class),
        ;

        private Class<? extends IpcPacket> packetClazz;

        @SuppressWarnings("unchecked")
        public static <T extends IpcPacket> T parsePacket(JsonObject json) throws JsonValidationException, JsonParseException {
            IpcPacketType type = Rson.DEFAULT.fromJson(json.get("type"), IpcPacketType.class);

            return (T) Rson.DEFAULT.fromJson(json, type.packetClazz);
        }

    }

}
