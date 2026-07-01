package net.xtte.betterlobby.teleport;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.xtte.betterlobby.model.LobbyLocation;
import org.bukkit.Location;

/** 代表一位玩家正在進行中的傳送倒數 */
public class TeleportSession {

    private final LobbyLocation target;
    private final Location startLocation;
    private int secondsLeft;
    private ScheduledTask task;

    public TeleportSession(LobbyLocation target, Location startLocation, int secondsLeft) {
        this.target = target;
        this.startLocation = startLocation;
        this.secondsLeft = secondsLeft;
    }

    public LobbyLocation getTarget() {
        return target;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public void decrement() {
        secondsLeft--;
    }

    public void setTask(ScheduledTask task) {
        this.task = task;
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
        }
    }
}
