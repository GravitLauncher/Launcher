package ru.gravit.launchserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import ru.gravit.launcher.Launcher;
import ru.gravit.utils.Version;
import ru.gravit.utils.Version.Type;
import ru.gravit.utils.helper.LogHelper;

public class Updater extends TimerTask {
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
	private static final long period = 1000*3600;
	private static final Version VERSION = Launcher.getVersion();
	private final Timer taskPool;
	private final GHRepository gravitLauncher;

	public Updater(LaunchServer srv) {
		this.taskPool = new Timer("Updater thread", true);

		GHRepository gravitLauncherTmp = null;
		try {
			gravitLauncherTmp  = GitHub.connectAnonymously().getOrganization("GravitLauncher").getRepository("Launcher");
		} catch (Throwable e) {
			LogHelper.error(e);
		}
		this.gravitLauncher = gravitLauncherTmp;

		run();
		if (srv.config.updatesNotify) taskPool.schedule(this, new Date(System.currentTimeMillis()+period), period);
	}

	@Override
	public void run() {
		try {
			GHRelease rel = gravitLauncher.getLatestRelease();
			Version relV = parseVer(rel.getTagName());
			if (VERSION.major >= relV.major || VERSION.minor >= relV.minor
					|| VERSION.patch >= relV.patch || VERSION.build >= relV.build) return;
			if (relV.release.equals(Type.STABLE) || relV.release.equals(Type.LTS)) {
				LogHelper.warning("New %s release: %s", relV.getReleaseStatus(), relV.getVersionString());
				LogHelper.warning("You can download it: " + rel.getHtmlUrl().toString());
				LogHelper.warning("It`s published at: " + DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(rel.getPublished_at().getTime()), ZoneId.systemDefault())));
			} else {
				LogHelper.debug("New %s release: %s", relV.getReleaseStatus(), relV.getVersionString());
				LogHelper.debug("You can download it: " + rel.getHtmlUrl());
				LogHelper.debug("It`s published at: " + DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(rel.getPublished_at().getTime()), ZoneId.systemDefault())));
			}
		} catch (Throwable e) {
			LogHelper.error(e);
		}
	}
	
	private static final Pattern startingVerPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
	private static final Pattern pointPatternStriper = Pattern.compile("\\.");
	
	private static Version parseVer(String relS) {
		Matcher verMatcher = startingVerPattern.matcher(relS);
		if (!verMatcher.find()) return null;
		String[] ver = pointPatternStriper.split(relS.substring(verMatcher.start(), verMatcher.end()));
		if (ver.length < 3) return null;
		return new Version(Integer.parseInt(ver[0]), Integer.parseInt(ver[1]), 
				Integer.parseInt(ver[2]), ver.length > 3 ? Integer.parseInt(ver[3]) : 0, findRelType(relS.substring(verMatcher.end()+1)));
	}

	private static Type findRelType(String substring) {
		if (substring.length() < 3 || substring.isEmpty()) return Type.UNKNOWN;
		String tS = substring;
		if (tS.startsWith("-")) tS = tS.substring(1);
		tS = tS.toUpperCase(Locale.ENGLISH);
		Type t = Type.UNKNOWN;
		try {
			t = Type.valueOf(tS);
		} catch (Throwable ign) { // ignore it
		}
		return t;
	}
	
	public static void main(String[] args) {
		System.out.println(parseVer("v3.4.5.6-stable").release);
	}
}
