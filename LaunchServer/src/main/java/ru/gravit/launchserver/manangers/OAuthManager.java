package ru.gravit.launchserver.manangers;

import com.vk.api.sdk.actions.OAuth;
import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.NeedGarbageCollection;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class OAuthManager implements NeedGarbageCollection {

    @Override
    public void garbageCollection() {
        for(int i=0; i < 5; i++ )
        {
            LaunchServer.server.cacheHandler.stageArea[i].destroy.run();
        }
        LogHelper.subInfo("OAuthCache purged");
    }

    public Entry[] stageArea;

    public OAuthManager(){
        if(stageArea == null) {
            stageArea = newEntryArray();
        }
    }

    public static class Entry{

        public void setEntry(Client client, ChannelHandlerContext ctx){
            if(client != null && ctx != null) {
                this.init =  true;
                this.client = client;
                this.ctx = ctx;
                LogHelper.subInfo("New Entry with IP " + IP());
                this.mTimer = new Timer();
                this.mTimer.schedule(destroy, 300000L);
            }
        }

        public void Entry(){
            this.init = false;
            this.mTimer = null;
            this.client = null;
            this.ctx = null;
        }

        private boolean init = false;

        private Client client = null;

        private ChannelHandlerContext ctx = null;

        private Timer mTimer = null;

        public String IP(){
           return IOHelper.getIP(getCtx().channel().remoteAddress());
        }

        private TimerTask destroy = new TimerTask() {
            @Override
            public void run() {
                if(init == false)
                    return;
                LogHelper.info("cache purged, IP: " + IP());
                init = false;
                mTimer = null;
                client = null;
                ctx = null;
            }
        };

        public boolean isInit() {
            return init;
        }

        public Client getClient() {
            return client;
        }

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

    }
    public static int stageAreaLength(){
        int i = 0;
        for(int e=0; e < 5; e++ )
        {
            i += LaunchServer.server.cacheHandler.stageArea[e].isInit() ? 1 : 0;
        }
        return i;
    }

    public static void stretchCache(Client client, ChannelHandlerContext ctx){
        getUnused().setEntry(client, ctx);
    }

    public static Entry getUnused(){
        for(int i=0; i < 5; i++ )
        {
            if(!LaunchServer.server.cacheHandler.stageArea[i].isInit())
                return LaunchServer.server.cacheHandler.stageArea[i];
        }
        try {
            throw new OAuthException("OAuth Overloaded");
        } catch (OAuthException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Entry[] newEntryArray(){
        return new Entry[]{new Entry(), new Entry(), new Entry(), new Entry(), new Entry()};
    }

    public static final class OAuthException extends IOException {

        public OAuthException(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }


}
