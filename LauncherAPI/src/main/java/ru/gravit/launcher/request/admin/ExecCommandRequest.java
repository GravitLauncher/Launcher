package ru.gravit.launcher.request.admin;

import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.LogHelper;

public class ExecCommandRequest extends Request<Boolean> {
    public LogHelper.Output loutput;
    public String cmd;

    public ExecCommandRequest(LogHelper.Output output, String cmd) {
        this.loutput = output;
        this.cmd = cmd;
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.EXECCOMMAND.getNumber();
    }

    @Override
    protected Boolean requestDo(HInput input, HOutput output) throws Exception {
        readError(input);
        output.writeString(cmd, SerializeLimits.MAX_COMMAND);
        boolean isContinue = true;
        while (isContinue) {
            isContinue = input.readBoolean();
            if (isContinue) {
                String log = input.readString(SerializeLimits.MAX_COMMAND);
                if (loutput != null) loutput.println(log);
            }
        }
        readError(input);
        return true;
    }
}
