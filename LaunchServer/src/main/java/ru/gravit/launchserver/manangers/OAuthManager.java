package ru.gravit.launchserver.manangers;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.NeedGarbageCollection;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class OAuthManager implements NeedGarbageCollection {

    @Override
    public void garbageCollection() {
            for(Entry e: LaunchServer.server.cacheHandler.stageArea)
            {
                e.destroy();
            }
        LogHelper.subInfo("OAuthCache purged");
    }

    public static class Entry{

        public void setter(ChannelHandlerContext ctx, Client client){
            this.init = true;
            this.ctx = ctx;
            this.client = client;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    destroy();
                }
            }, 300000L);
        }

        public void setter(AuthRequestEvent authRequestEvent){
            this.authRequestEvent = authRequestEvent;
        }

        public Entry(){
            this.init =  false;
            this.ctx = null;
            this.client = null;
            this.authRequestEvent = null;
            new Timer().cancel();
        }

        private boolean init;

        private ChannelHandlerContext ctx;

        private Client client;

        private AuthRequestEvent authRequestEvent;

        public void destroy(){
            this.init =  false;
            this.ctx = null;
            this.client = null;
            this.authRequestEvent = null;
            new Timer().cancel();
        }

        public boolean isInit() {
            return init;
        }

        public ChannelHandlerContext getCtx() {
            return ctx;
        }

        public Client getClient() {
            return client;
        }

        public String getIP(){
            if(isInit())
                return IOHelper.getIP(getCtx().channel().remoteAddress());
            else
                return null;
        }
    }

    private Entry[] stageArea;

    public static Entry[] getStageArea(){
        return LaunchServer.server.cacheHandler.stageArea;
    }

    public static Integer getCacheLength(){
        int i = 0;
        for(Entry e : LaunchServer.server.cacheHandler.stageArea)
            i = e.isInit() ? 1 : 0;
        return i;
    }

    public OAuthManager(){
        stageArea = new Entry[]{
                new Entry(), new Entry(), new Entry(), new Entry(), new Entry()
        };
    }

    public static void stretchCache(ChannelHandlerContext ctx, Client client){
        try {
           Entry e = getUnused();
           e.setter(ctx, client);
           LogHelper.subDebug("New Entry IP: " + e.getIP());
        } catch (OAuthException e) {
            e.printStackTrace();
        }
    }

    public static void stretchCache(String IP, AuthRequestEvent authRequestEvent){
            for(Entry e: LaunchServer.server.cacheHandler.stageArea)
            {
                if(e.getIP().equals(IP))
                    e.setter(authRequestEvent);
            }
            try {
                throw new OAuthException("Not found");
            } catch (OAuthException e) {
                e.printStackTrace();
            }
    }
    public static void stretchCache(ChannelHandlerContext ctx, AuthRequestEvent authRequestEvent){
            for(Entry e: LaunchServer.server.cacheHandler.stageArea)
            {
                if(e.getIP().equals(IOHelper.getIP(ctx.channel().remoteAddress())))
                    e.setter(authRequestEvent);
            }
            try {
                throw new OAuthException("Not found");
            } catch (OAuthException e) {
                e.printStackTrace();
            }
    }

    public static Entry getUnused() throws OAuthException {
        for(Entry e: LaunchServer.server.cacheHandler.stageArea)
        {
            if(e.isInit())
                continue;
            return e;
        }

        throw new OAuthException("OAuth Overloaded");
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
