package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.net.InetSocketAddress;

public class ChangeServerRequest extends Request<ChangeServerRequest.Result> {
    @Override
    public Integer getLegacyType() {
        return RequestType.CHANGESERVER.getNumber();
    }

    public boolean change(Result result) {
        if (!result.needChange) return false;
        Launcher.getConfig().address = InetSocketAddress.createUnresolved(result.address, result.port);
        return true;
    }

    @Override
    protected Result requestDo(HInput input, HOutput output) throws Exception {
        readError(input);
        Result result = new Result();
        result.needChange = input.readBoolean();
        if (result.needChange) {
            result.address = input.readString(255);
            result.port = input.readInt();
        }
        if (result.needChange) change(result);
        return result;
    }

    public class Result {
        public boolean needChange;
        public String address;
        public int port;
    }
}
