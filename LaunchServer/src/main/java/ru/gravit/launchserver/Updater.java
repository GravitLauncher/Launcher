package ru.gravit.launchserver;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import ru.gravit.utils.helper.LogHelper;

public class Updater extends TimerTask {
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
	private static final long period = 1000*3600;
	private final Timer taskPool;
	private final GHRepository gravitLauncher;
	private GHRelease parenRel = null;

	public Updater(LaunchServer srv) {
		this.taskPool = new Timer("Updater thread", true);

		GHRepository gravitLauncherTmp = null;
		try {
			gravitLauncherTmp  = GitHub.connectAnonymously().getOrganization("GravitLauncher").getRepository("Launcher");
		} catch (IOException e) {
			LogHelper.error(e);
		}
		this.gravitLauncher = gravitLauncherTmp;

		run();
		taskPool.schedule(this, new Date(System.currentTimeMillis()+period), period);
	}

	@Override
	public void run() {
		try {
			GHRelease rel = gravitLauncher.getLatestRelease();
			if (rel.equals(parenRel)) return;
			LogHelper.warning("Latest release: %s", rel.getName());
			LogHelper.warning("It`s published at: " + DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(rel.getPublished_at().getTime()), ZoneId.systemDefault())));
		} catch (IOException e) {
			LogHelper.error(e);
		}
	}
	
}
