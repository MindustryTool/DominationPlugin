package towerdefense;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.ai.types.FlyingAI;
import mindustry.ai.types.GroundAI;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.PlayEvent;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.UnitSpawnEvent;
import mindustry.game.EventType.WaveEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.mod.*;
import mindustry.net.Administration.ActionType;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.ShockMine;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.UnitBlock;
import mindustry.world.meta.BlockFlag;

public class DominationPlugin extends Plugin {

    @Override
    public void init() {
    }
}
