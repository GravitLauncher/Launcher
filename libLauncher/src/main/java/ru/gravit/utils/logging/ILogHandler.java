package ru.gravit.utils.logging;

public interface ILogHandler {
	boolean canExc(Throwable t);
	String exc(Throwable t);
	String process(String str);
}
