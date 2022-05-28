package co.casterlabs.kaimen.app;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonClass(exposeAll = true)
@EqualsAndHashCode(callSuper = false)
class IpcPacketInvocationResult extends IpcPacket {
    private String invocationId;
    private ResultData result;
    private String error;

    public boolean isError() {
        return this.error != null;
    }

    @Override
    public IpcPacketType getType() {
        return IpcPacketType.INVOCATION_RESULT;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @JsonClass(exposeAll = true)
    @EqualsAndHashCode(callSuper = false)
    static class ResultData {
        private boolean isRegularResult;
        private JsonElement content;

    }

}
