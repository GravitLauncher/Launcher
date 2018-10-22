package ru.gravit.launcher.relauncher;

import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Locale;
import java.util.jar.Manifest;

public final class Helper {
	public static enum OS {
		LINUX("linux"), MACOSX("macosx"), MUSTDIE("mustdie"), OTHER("other");
		public static OS byName(final String name) {
			if (name.startsWith("Windows"))
				return MUSTDIE;
			if (name.startsWith("Linux"))
				return LINUX;
			if (name.startsWith("Mac OS X"))
				return MACOSX;
			return OTHER;
		}

		public final String name;

		private OS(final String name) {
			this.name = name;
		}
	}

	private static final String DEFERR = "Invalid java version.";

	public static final ClassLoader LOADER = ClassLoader.getSystemClassLoader();
	public static final OS os = OS.byName(ManagementFactory.getOperatingSystemMXBean().getName());
	public static Manifest mf = null;
	
	public static final String getErrMessage() {
		try {
			return getErrMessage(getManifest());
		} catch (final Throwable t) {
			return DEFERR;
		}
	}

	public static final String getErrMessage(final Manifest mf) {
		String mess = DEFERR;
		try {
			mess = mf.getMainAttributes().getValue("ErrorMessage-String");
		} catch (final Throwable t) {
		}
		return mess;
	}

	public static final String getMainClass() {
		try {
			return getMainClass(getManifest());
		} catch (final Throwable t) {
			return null;
		}
	}

	public static final String getMainClass(final Manifest mf) {
		String main = null;
		try {
			main = mf.getMainAttributes().getValue("MainRun-Class").trim();
		} catch (final Throwable t) {
		}
		return main;
	}

	public static final Manifest getManifest() {
		if (mf != null) return mf;
		try {
			InputStream in = VerRelauncher.class.getResourceAsStream("/META-INF/MANIFEST.MF");
			Manifest mf = new Manifest(in);
			in.close();
			Helper.mf = mf;
			return mf;
		} catch (final Throwable t) {
			return null;
		}
	}

	public static final int getMinVer() {
		try {
			return getMinVer(getManifest());
		} catch (final Throwable t) {
			return JavaVersionInfo.JAVA_6;
		}
	}

	public static final int getMinVer(final Manifest mf) {
		int ver = JavaVersionInfo.JAVA_6;
		try {
			ver = Integer.parseInt(mf.getMainAttributes().getValue("MinVesion-Integer").trim());
		} catch (final Throwable t) {
		}
		return ver;
	}

	public static final OS getOs() {
		return os;
	}

	public static final boolean isGraphic() {
		try {
			return isGraphic(getManifest());
		} catch (final Throwable t) {
			return false;
		}
	}

	public static final boolean isGraphic(final Manifest mf) {
		boolean graph = false;
		try {
			graph = "TRUE".equalsIgnoreCase(mf.getMainAttributes().getValue("Graphic-Enabled").trim());
		} catch (final Throwable t) {
		}
		return graph;
	}

	public static void verifySystemProperties(final Class<?> mainClass, final boolean requireSystem) {
		Locale.setDefault(Locale.US);
		// Verify ClassLoader
		if (requireSystem && !mainClass.getClassLoader().equals(LOADER))
			throw new SecurityException("ClassLoader should be system");
	}
}
