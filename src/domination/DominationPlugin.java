package domination;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import arc.Events;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.mod.*;
import arc.util.Align;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;

public class DominationPlugin extends Plugin {

    private static final int CORE_PER_SECOND = 1;
    private static final int POINT_TO_WIN = 300_000;

    private static final ConcurrentHashMap<Team, Integer> teamPoints = new ConcurrentHashMap<>();

    @Override
    public void init() {
        Timer.schedule(() -> {
            if (Vars.state.isPlaying()) {
                addPoint();
                updatePointPanel();
            }

        }, 0, 1);

        Events.on(EventType.GameOverEvent.class, e -> {
            resetPoints();
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            resetPoints();
        });
    }

    private void addPoint() {
        for (Team team : Team.all) {
            if (team == Team.derelict) {
                continue;
            }

            for (var core : team.cores()) {
                int points = core.block.size * CORE_PER_SECOND;
                int newPoint = teamPoints.getOrDefault(team, 0) + points;
                teamPoints.put(team, newPoint);

                Call.label("+" + points, 0.5f, core.x + Mathf.range(4f), core.y + Mathf.range(4f));

                if (newPoint >= POINT_TO_WIN) {
                    Events.fire(new EventType.GameOverEvent(team));
                }
            }
        }
    }

    private void updatePointPanel() {
        StringBuilder content = new StringBuilder("Point to win: " + POINT_TO_WIN + "\n");

        for (var entry : teamPoints.entrySet().stream()
                .sorted(Comparator.<Entry<Team, Integer>>comparingInt(a -> a.getValue()).reversed()).toList()) {
            var team = entry.getKey();
            var point = entry.getValue();
            var percent = (float) point / POINT_TO_WIN * 100;
            percent = Math.round(percent * 100.0f) / 100.0f;

            content.append(team.coloredName()).append("[] ").append(point).append(" (").append(percent).append("%)\n");
        }

        Call.infoPopup(content.toString(), 1.05f, Align.right | Align.center, 0, 0, 0, 0);
    }

    private void resetPoints() {
        teamPoints.clear();
    }

    public void registerServerCommands(CommandHandler handler) {
        handler.register("point", "[team] [amount]", "Add point to team", (agrs) -> {
            try {
                for (Team team : Team.all) {
                    if (team.name != agrs[0]) {
                        continue;
                    }

                    int amount = Integer.parseInt(agrs[1]);
                    int newPoint = teamPoints.getOrDefault(team, 0) + amount;
                    teamPoints.put(team, newPoint);

                    Log.info("Added @ points to team @", amount, team.name);

                    updatePointPanel();
                }

            } catch (Exception e) {
                Log.err(e);
            }
        });
    }
}
