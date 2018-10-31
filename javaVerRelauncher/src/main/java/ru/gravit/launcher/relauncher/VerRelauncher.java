package ru.gravit.launcher.relauncher;

import java.lang.reflect.Method;

import javax.swing.JOptionPane;

public final class VerRelauncher {
	private static final void checkCompat() {
		if (JavaVersionInfo.MAJOR_VERSION < Helper.getMinVer()) {
			runGraph(Helper.getErrMessage());
			throw new AssertionError(Helper.getErrMessage());
		}
	}

	public static void main(final String[] args) {
		verifySystemProperties();
		try {
			checkCompat();
			final Class<?> main = Class.forName(Helper.getMainClass(), true, ClassLoader.getSystemClassLoader());
			Helper.verifySystemProperties(main, true);
			final Method mainMethod = main.getMethod("main", String[].class);
			mainMethod.setAccessible(true);
			mainMethod.invoke(null, new Object[] { args });
		} catch (final Throwable t) {
			if (t instanceof AssertionError)
				throw (AssertionError) t;
			if (t instanceof InternalError)
				throw (InternalError) t;
			throw new InternalError(t);
		}
	}

	private static void runGraph(final String errMessage) {
		try {
			Class.forName("javax.swing.JOptionPane", true, ClassLoader.getSystemClassLoader());
			JOptionPane.showMessageDialog(null, errMessage);
		} catch (final Throwable t) { }
	}

	private static void verifySystemProperties() {
		Helper.verifySystemProperties(Helper.class, true);
		Helper.verifySystemProperties(VerRelauncher.class, true);
		Helper.verifySystemProperties(JavaVersionInfo.class, true);
	}
}
