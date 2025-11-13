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
import mindustry.gen.Groups;
import mindustry.mod.*;
import mindustry.world.blocks.storage.CoreBlock;
import arc.util.Align;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;

public class DominationPlugin extends Plugin {

    private static final int CORE_PER_SECOND = 1;
    private static final int POINT_TO_WIN = 200_000;

    private static final ConcurrentHashMap<Team, Integer> teamPoints = new ConcurrentHashMap<>();

    private static int secondPassed = 0;

    private static final class Session {
        private final Team team;

        public Session(Team team) {
            this.team = team;
        }
    }

    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void init() {
        Timer.schedule(() -> {
            if (Vars.state.isPlaying()) {
                addPoint();
                updatePointPanel();
                checkSession();
                secondPassed++;
            }

        }, 0, 1);

        Events.on(EventType.GameOverEvent.class, e -> {
            reset();
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            reset();
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            handlePlayerJoin(event);
        });

        Events.on(EventType.BuildingBulletDestroyEvent.class, event -> {
            handleBlockDestroy(event);
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            sessions.clear();
        });
    }

    private void handleBlockDestroy(EventType.BuildingBulletDestroyEvent event) {
        var building = event.build;
        var bullet = event.bullet;

        if (building != null && building.team() != Team.malis && building.block != null
                && building.block instanceof CoreBlock) {
            int points = building.block.size * CORE_PER_SECOND * 100;
            int newPoint = teamPoints.getOrDefault(building.team(), 0) - points;

            teamPoints.put(building.team(), Math.max(0, newPoint));

            Call.infoMessage("Team " + building.team().name + " lost " + points + " points");

            teamPoints.put(bullet.team(), teamPoints.getOrDefault(bullet.team(), 0) + points);

            Call.infoMessage("Team " + bullet.team().name + " gained " + points + " points");
        }
    }

    private void handlePlayerJoin(EventType.PlayerJoin event) {
        if (sessions.containsKey(event.player.uuid())) {
            var session = sessions.get(event.player.uuid());

            event.player.team(session.team);
            return;
        }

        if (event.player.team() == Team.malis) {
            int leastPlayer = Integer.MAX_VALUE;
            Team leastPlayerTeam = null;

            for (var team : Vars.state.teams.getActive().map(t -> t.team)) {
                if (team == null || team == Team.malis) {
                    continue;
                }

                int players = 0;
                for (var player : Groups.player) {
                    if (player.team() == team) {
                        players++;
                    }
                }

                if (players < leastPlayer) {
                    leastPlayer = players;
                    leastPlayerTeam = team;
                }
            }

            if (leastPlayerTeam != null) {
                event.player.team(leastPlayerTeam);
            }
        }

        sessions.put(event.player.uuid(), new Session(event.player.team()));
    }

    private void addPoint() {
        for (Team team : Vars.state.teams.getActive().map(t -> t.team)) {
            if (team == Team.malis || team == Team.derelict) {
                continue;
            }

            for (var core : team.cores()) {
                int points = core.block.size * CORE_PER_SECOND;
                int newPoint = teamPoints.getOrDefault(team, 0) + points;
                teamPoints.put(team, newPoint);

                Call.label("+" + points, 0.5f, core.x + Mathf.range(8f), core.y + Mathf.range(8f));

                if (newPoint >= POINT_TO_WIN) {
                    Events.fire(new EventType.GameOverEvent(team));
                }
            }
        }
    }

    private void updatePointPanel() {
        StringBuilder content = new StringBuilder("Point to win: " + POINT_TO_WIN + "\n");
        int minutes = secondPassed / 60;
        int hours = minutes / 60;
        content.append("Time passed: ");

        if (hours > 0) {
            content.append(hours).append("h ");
        }
        if (minutes > 0) {
            content.append(minutes % 60).append("m ");
        }
        content.append(secondPassed % 60).append("s\n");

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

    private void checkSession() {
        for (var player : Groups.player) {
            if (!sessions.containsKey(player.uuid())) {
                sessions.put(player.uuid(), new Session(player.team()));
            }
        }
    }

    private void reset() {
        teamPoints.clear();
        sessions.clear();
        secondPassed = 0;
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
