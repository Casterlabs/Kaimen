package co.casterlabs.kaimen.app;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonClass(exposeAll = true)
@EqualsAndHashCode(callSuper = false)
class IpcPacketInit extends IpcPacket {
    private String targetClass;

    @Override
    public IpcPacketType getType() {
        return IpcPacketType.INIT;
    }

}
