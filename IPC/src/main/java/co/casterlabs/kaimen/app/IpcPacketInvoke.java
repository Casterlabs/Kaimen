package co.casterlabs.kaimen.app;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonClass(exposeAll = true)
@EqualsAndHashCode(callSuper = false)
class IpcPacketInvoke extends IpcPacket {
    private String invocationId;
    private String objectId;
    private String method;
    private JsonArray args;

    @Override
    public IpcPacketType getType() {
        return IpcPacketType.INVOKE;
    }

}
