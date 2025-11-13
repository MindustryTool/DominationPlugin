package domination;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import arc.Events;
import arc.scene.Group;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.mod.*;
import arc.util.Align;
import arc.util.Timer;

public class DominationPlugin extends Plugin {

    private static final int CORE_PER_SECOND = 1;
    private static final int POINT_TO_WIN = 300_000;

    private static final ConcurrentHashMap<Team, Integer> teamPoints = new ConcurrentHashMap<>();

    @Override
    public void init() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            Timer.schedule(() -> {
                if (Vars.state.isPlaying()) {
                    addPoint();
                    updatePointPanel();
                }

            }, 0, 1);
        });

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
                teamPoints.put(team, teamPoints.getOrDefault(team, 0) + points);
            }
        }
    }

    private void updatePointPanel() {
        StringBuilder content = new StringBuilder("Point to win: " + POINT_TO_WIN + "\n");

        for (var entry : teamPoints.entrySet().stream()
                .sorted(Comparator.<Entry<Team, Integer>>comparingInt(a -> a.getValue()).reversed()).toList()) {
            content.append(entry.getKey().name).append(": ").append(entry.getValue()).append("\n");
        }

        Call.infoPopupReliable(content.toString(), 1.05f, Align.right | Align.center, 0, 0, 0, 0);
    }

    private void resetPoints() {
        teamPoints.clear();
    }
}
