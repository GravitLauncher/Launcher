package pro.gravit.launcher.client.params;

import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.utils.helper.IOHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketParamsWriter implements ParamsWriter {
    private final SocketAddress address;

    public SocketParamsWriter(SocketAddress address) {
        this.address = address;
    }

    @Override
    public void write(ClientLauncherProcess.ClientParams params) throws IOException {
        try(ServerSocket socket = new ServerSocket()) {
            socket.bind(address);
            Socket client = socket.accept();
            try(DataOutputStream stream = new DataOutputStream(client.getOutputStream())) {
                params.write(stream);
            }
        }
    }

    @Override
    public ClientLauncherProcess.ClientParams read() throws IOException {
        try(Socket socket = IOHelper.newSocket()) {
            socket.connect(address, 30*1000);
            try(DataInputStream stream = new DataInputStream(socket.getInputStream())) {
                return ClientLauncherProcess.ClientParams.read(stream);
            }
        }
    }
}
