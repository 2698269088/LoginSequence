package top.mcocet.loginSequence;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class LogFilter implements Filter {
    private Result onMatch = Result.NEUTRAL;
    private Result onMismatch = Result.NEUTRAL;

    public Result checkMessage(String message) {
        if (message != null && (
                message.contains("/l ") ||
                message.contains("/login ") ||
                message.contains("/log ") ||
                message.contains("/reg ") ||
                message.contains("/register ") ||
                message.contains("/unregister ") ||
                message.contains("/unreg ") ||
                message.contains("/changepasswd ") ||
                message.contains("/cp "))) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public LifeCycle.State getState() {
        return LifeCycle.State.STARTED;
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public Result filter(LogEvent event) {
        return this.checkMessage(event.getMessage().getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object... params) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable t) {
        return this.checkMessage(message != null ? message.toString() : null);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable t) {
        return this.checkMessage(message != null ? message.getFormattedMessage() : null);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return this.checkMessage(message);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return this.checkMessage(message);
    }

    @Override
    public Result getOnMatch() {
        return onMatch;
    }

    @Override
    public Result getOnMismatch() {
        return onMismatch;
    }
}