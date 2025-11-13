package domination;

import java.util.concurrent.ConcurrentHashMap;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.mod.*;
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

            for (var core : team.cores()){
                int points = core.block.size * CORE_PER_SECOND;
                teamPoints.put(team, teamPoints.getOrDefault(team, 0) + points);
            }
        }
    }

    private void updatePointPanel(){
        Call.infoPopupReliable(null, CORE_PER_SECOND, CORE_PER_SECOND, CORE_PER_SECOND, CORE_PER_SECOND, POINT_TO_WIN, CORE_PER_SECOND);
    }

    private void resetPoints(){
        teamPoints.clear();
    }
}
