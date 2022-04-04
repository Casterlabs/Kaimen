package co.casterlabs.kaimen.app;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonElement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonClass(exposeAll = true)
@EqualsAndHashCode(callSuper = false)
class IpcPacketInvocationResult extends IpcPacket {
    private String invocationId;
    private JsonElement result;
    private String error;

    public boolean isError() {
        return this.error != null;
    }

    @Override
    public IpcPacketType getType() {
        return IpcPacketType.INVOCATION_RESULT;
    }

}
