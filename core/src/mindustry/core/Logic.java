package mindustry.core;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.annotations.Annotations.*;
import mindustry.client.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.ui.fragments.ChatFragment;
import mindustry.ui.fragments.ResLogDialog;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.content.Blocks;

import java.time.LocalTime;
import java.util.*;

import static mindustry.Vars.*;

/**
 * Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 * <p>
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic implements ApplicationListener{

    public Logic(){
        Events.on(BlockDestroyEvent.class, event -> {
            //core broke alarm
            if(Core.settings.getBool("coredeathalarm")){
                if(state.rules.coreCapture && Core.settings.getBool("coredeathalarmrecap")) {} else{
                    if(event.tile.build instanceof CoreBuild core){
                        if(core.team == player.team()){
                            if(Core.settings.getBool("unitatchat") && !state.rules.coreCapture){
                                if(state.rules.pvp) { Call.sendChatMessage("/t [#fa]" + "Our core at " + Mathf.ceil(core.x/8) + ", " + Mathf.ceil(core.y/8) + " death...");} else { Call.sendChatMessage("[#fa]" + "Our core at " + Mathf.ceil(core.x/8) + ", " + Mathf.ceil(core.y/8) + " death...");}
                            }else{
                                String alarm = "[#fa]" + "Our core at " + Mathf.ceil(core.x/8) + ", " + Mathf.ceil(core.y/8) + " death...";
                                ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(alarm, null, null, "", alarm);
                                NetClient.findCoords(msg);
                            }
                        } else {
                            String alarm ="[#" + core.team.color + "]" + core.team.name + " core at []" + Mathf.ceil(core.x/8) + ", " + Mathf.ceil(core.y/8) + " death.";
                            ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(alarm, null, null, "", alarm);
                            NetClient.findCoords(msg);
                        }
                    }
                }
            }

            //skip if rule is off
            if(!state.rules.ghostBlocks) return;

            //blocks that get broken are appended to the team's broken block queue
            Tile tile = event.tile;
            //skip null entities or un-rebuildables, for obvious reasons
            if(tile.build == null || !tile.block().rebuildable) return;

            tile.build.addPlan(true);
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                TeamData data = event.team.data();
                Iterator<BlockPlan> it = data.plans.iterator();
                var bounds = event.tile.block().bounds(event.tile.x, event.tile.y, Tmp.r1);
                while(it.hasNext()){
                    BlockPlan b = it.next();
                    Block block = content.block(b.block);
                    if(bounds.overlaps(block.bounds(b.x, b.y, Tmp.r2))){
                        b.removed = true;
                        it.remove();
                    }
                }

                if(event.team == state.rules.defaultTeam){
                    state.stats.placedBlockCount.increment(event.tile.block());
                }
            }
        });

        //when loading a 'damaged' sector, propagate the damage
        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                state.rules.coreIncinerates = true;

                //TODO why is this even a thing?
                state.rules.canGameOver = true;

                //fresh map has no sector info
                if(!e.isMap){
                    SectorInfo info = state.rules.sector.info;
                    info.write();

                    //only simulate waves if the planet allows it
                    if(state.rules.sector.planet.allowWaveSimulation){
                        //how much wave time has passed
                        int wavesPassed = info.wavesPassed;

                        //wave has passed, remove all enemies, they are assumed to be dead
                        if(wavesPassed > 0){
                            Groups.unit.each(u -> {
                                if(u.team == state.rules.waveTeam){
                                    u.remove();
                                }
                            });
                        }

                        //simulate passing of waves
                        if(wavesPassed > 0){
                            //simulate wave counter moving forward
                            state.wave += wavesPassed;
                            state.wavetime = state.rules.waveSpacing;

                            SectorDamage.applyCalculatedDamage();
                        }
                    }

                    state.getSector().planet.applyRules(state.rules);

                    //reset values
                    info.damage = 0f;
                    info.wavesPassed = 0;
                    info.hasCore = true;
                    info.secondsPassed = 0;

                    state.rules.sector.saveInfo();
                }
            }
        });

        Events.on(PlayEvent.class, e -> {
            //reset weather on play
            var randomWeather = state.rules.weather.copy().shuffle();
            float sum = 0f;
            for(var weather : randomWeather){
                weather.cooldown = sum + Mathf.random(weather.maxFrequency);
                sum += weather.cooldown;
            }
            //tick resets on new save play
            state.tick = 0f;
        });

        Events.on(WorldLoadEvent.class, e -> {
            //enable infinite ammo for wave team by default
            state.rules.waveTeam.rules().infiniteAmmo = true;

            if(state.isCampaign()){
                //enable building AI on campaign unless the preset disables it

                state.rules.coreIncinerates = true;
                state.rules.waveTeam.rules().infiniteResources = true;
                state.rules.waveTeam.rules().buildSpeedMultiplier *= state.getPlanet().enemyBuildSpeedMultiplier;

                //fill enemy cores by default? TODO decide
                for(var core : state.rules.waveTeam.cores()){
                    for(Item item : content.items()){
                        core.items.set(item, core.block.itemCapacity);
                    }
                }

                //set up hidden items
                state.rules.hiddenBuildItems.clear();
                state.rules.hiddenBuildItems.addAll(state.rules.sector.planet.hiddenItems);
            }

            //save settings
            Core.settings.manualSave();
        });

        //sync research
        Events.on(UnlockEvent.class, e -> {
            if(net.server()){
                Call.researched(e.content);
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(!net.client() && e.sector == state.getSector() && e.sector.isBeingPlayed()){
                state.rules.waveTeam.data().destroyToDerelict();
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBuild core && core.team.isAI() && state.rules.coreDestroyClear){
                Core.app.post(() -> {
                    core.team.data().timeDestroy(core.x, core.y, state.rules.enemyCoreBuildRadius);
                });
            }
        });

        //listen to core changes; if all cores have been destroyed, set to derelict.
        Events.on(CoreChangeEvent.class, e -> Core.app.post(() -> {
            if(state.rules.cleanupDeadTeams && state.rules.pvp && !e.core.isAdded() && e.core.team != Team.derelict && e.core.team.cores().isEmpty()){
                e.core.team.data().destroyToDerelict();
            }
        }));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.team == state.rules.defaultTeam){
                if(e.breaking){
                    state.stats.buildingsDeconstructed++;
                }else{
                    state.stats.buildingsBuilt++;
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.team() == state.rules.defaultTeam){
                state.stats.buildingsDestroyed ++;
            }
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.team() != state.rules.defaultTeam){
                state.stats.enemyUnitsDestroyed ++;
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(e.unit.team == state.rules.defaultTeam){
                state.stats.unitsCreated++;
            }
        });

        Events.on(BlockBuildBeginEventBefore.class, it -> { // Recording the construction of blocks
            if(Core.settings.getBool("blocksplayersplan")) {
                if (it.newBlock == null || it.newBlock == Blocks.air) {
                    if (it.tile.build == null || !it.tile.block().rebuildable) return;
                    ActionsHistory.blocksplayersplans.addFirst(new ActionsHistory.BlockPlayerPlan(it.tile.x, it.tile.y, (short) it.tile.build.rotation, it.tile.build.block.id,it.tile.build.config(), it.unit.getControllerName(), it.breaking));
                    //state.teams.get(player.team()).blocksplayersplans.addFirst(new BlockPlayerPlan(it.tile.x, it.tile.y, (short) it.tile.build.rotation, it.tile.build.block.id,it.tile.build.config(), it.unit.getControllerName(), it.breaking));
                } else {
                    if (it.tile == null) return;
                    //state.teams.get(player.team()).blocksplayersplans.addFirst(new BlockPlayerPlan(it.tile.x, it.tile.y, (short) 0,  it.newBlock.id, null, it.unit.getControllerName(), it.breaking));
                    ActionsHistory.blocksplayersplans.addFirst(new ActionsHistory.BlockPlayerPlan(it.tile.x, it.tile.y, (short) 0,  it.newBlock.id, null, it.unit.getControllerName(), it.breaking));
                }
            }
        });

        Events.on(BuildRotateEvent.class, it -> { // Recording the confgigs of blocks
            if(Core.settings.getBool("blocksplayersplan")) {
                if (it.build == null || it.unit.controller() == null) return;
                ActionsHistory.blockconfplayersplans.addFirst(new ActionsHistory.BlockConfigPlayerPlan((int)it.build.x/8, (int)it.build.y/8, it.build.block.id, it.unit.getControllerName()));
                //state.teams.get(player.team()).blockconfplayersplans.addFirst(new BlockConfigPlayerPlan((int)it.build.x/8, (int)it.build.y/8, it.build.block.id, it.player.name));
            }
        });
        Events.on(ConfigEvent.class, it -> { // Recording the confgigs of blocks
            if(Core.settings.getBool("blocksplayersplan")) {
                if (it.tile.block == null || it.player == null) return;
                //state.teams.get(player.team()).blockconfplayersplans.addFirst(new BlockConfigPlayerPlan((int)it.tile.x/8, (int)it.tile.y/8, it.tile.block.id, it.player.name));
                ActionsHistory.blockconfplayersplans.addFirst(new ActionsHistory.BlockConfigPlayerPlan((int)it.tile.x/8, (int)it.tile.y/8, it.tile.block.id, it.player.name));
            }
        });

        Events.on(DepositEvent.class, it -> { // Recording items deposit
            if(Core.settings.getBool("itemslog")) {
                if (it.tile.block == null || it.player == null) return;
                LocalTime tempct = LocalTime.now();
                ActionsHistory.playeritemsplans.addFirst(new ActionsHistory.ItemPlayerPlan(it.player, it.tile.tile, it.item, false, tempct));
            }
        });
        Events.on(WithdrawEvent.class, it -> { // Recording items withdraw
            if(Core.settings.getBool("itemslog")) {
                if (it.tile.block == null || it.player == null) return;
                LocalTime tempct = LocalTime.now();
                ActionsHistory.playeritemsplans.addFirst(new ActionsHistory.ItemPlayerPlan(it.player, it.tile.tile, it.item, true, tempct));
            }
        });

        Events.on(UnitDeadEvent.class, it -> { // Alarm of death units
            if(Core.settings.getBool("unitdeathalarm")) {
                if(Core.settings.getInt("unitdeathalarmhp") == 0) return;
                Unit u = it.unit;
                if(u.maxHealth >= Core.settings.getInt("unitdeathalarmhp")){
                    String alarm = "[#" + u.team.color + "] " + u.type + ": " + Mathf.ceil(u.lastX/8) + "," + Mathf.ceil(u.lastY/8);
                    if(!(u.getControllerName() == null)){
                        alarm = alarm + " controlled by: " + u.getControllerName();
                    }
                    ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(alarm, null, null, "", alarm);
                    NetClient.findCoords(msg);
                }
            }
        });


        Events.on(PlayerUnitDeathEvent.class, it -> { // Alarm of death units with players
            if(Core.settings.getBool("playerunitdeathalarm")) {
                Unit u = it.unit;
                if(!(u.getControllerName() == null)){
                    if(Core.settings.getInt("playerunitdeathalarmhp") >= 0){
                        LocalTime tempct = LocalTime.now();
                        ActionsHistory.deathunitsplan.addFirst(new ActionsHistory.UnitsKilledByPlayers(u.getPlayer(), u, u.lastX, u.lastY, tempct));
                        if(u.maxHealth >= Core.settings.getInt("playerunitdeathalarmhp")){
                            if(Core.settings.getInt("playerunitdeathalarmhp") == 0) return;
                            String alarm = "[#" + u.team.color + "] " + u.type + ": " + Mathf.ceil(u.lastX / 8) + "," + Mathf.ceil(u.lastY / 8) + " controlled by: " + u.getControllerName();
                            ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(alarm, null, null, "", alarm);
                            NetClient.findCoords(msg);
                        }
                    }
                } else if(!(u.lastCommanded == null)){
                    if(Core.settings.getInt("playerunitdeathalarmhp") >= 0){
                        LocalTime tempct = LocalTime.now();
                        ActionsHistory.deathunitscontrolplan.addFirst(new ActionsHistory.UnitsKilledByControllPlayers(u.lastCommanded, u, u.lastX, u.lastY, tempct));
                        if(u.maxHealth >= Core.settings.getInt("playerunitdeathalarmhp")){
                            if(Core.settings.getInt("playerunitdeathalarmhp") == 0) return;
                            String alarm = "[#" + u.team.color + "] " + u.type + ": " + Mathf.ceil(u.lastX/8) + "," + Mathf.ceil(u.lastY/8)+ " last command by: " + u.lastCommanded();
                            ChatFragment.ChatMessage msg = ui.chatfrag.addMessage(alarm, null, null, "", alarm);
                            NetClient.findCoords(msg);
                        }
                    }

                }
            }
        });
    }

    /** Adds starting items, resets wave time, and sets state to playing. */
    public void play(){
        state.set(State.playing);
        //grace period of 2x wave time before game starts
        state.wavetime = state.rules.initialWaveSpacing <= 0 ? state.rules.waveSpacing * 2 : state.rules.initialWaveSpacing;
        Events.fire(new PlayEvent());

        //add starting items
        if(!state.isCampaign() || !state.rules.sector.planet.allowLaunchLoadout || (state.rules.sector.preset != null && state.rules.sector.preset.addStartingItems)){
            for(TeamData team : state.teams.getActive()){
                if(team.hasCore()){
                    CoreBuild entity = team.core();
                    entity.items.clear();

                    for(ItemStack stack : state.rules.loadout){
                        //make sure to cap storage
                        entity.items.add(stack.item, Math.min(stack.amount, entity.storageCapacity - entity.items.get(stack.item)));
                    }
                }
            }
        }

        //heal all cores on game start
        for(TeamData team : state.teams.getActive()){
            for(var entity : team.cores){
                entity.heal();
            }
        }
    }

    public void reset(){
        State prev = state.getState();
        //recreate gamestate - sets state to menu
        state = new GameState();
        //fire change event, since it was technically changed
        Events.fire(new StateChangeEvent(prev, State.menu));

        Groups.clear();
        Time.clear();
        Events.fire(new ResetEvent());

        //save settings on reset
        Core.settings.manualSave();
    }

    public void skipWave(){
        runWave();
    }

    public void runWave(){
        spawner.spawnEnemies();
        state.wave++;
        state.wavetime = state.rules.waveSpacing;

        Events.fire(new WaveEvent());
    }

    private void checkGameState(){
        //campaign maps do not have a 'win' state!
        if(state.isCampaign()){
            //gameover only when cores are dead
            if(state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }

            //check if there are no enemy spawns
            if(state.rules.waves && spawner.countSpawns() + state.teams.cores(state.rules.waveTeam).size <= 0){
                //if yes, waves get disabled
                state.rules.waves = false;
            }

            //if there's a "win" wave and no enemies are present, win automatically
            if(state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning()) ||
                (state.rules.attackMode && state.rules.waveTeam.cores().isEmpty())){

                if(state.rules.sector.preset != null && state.rules.sector.preset.attackAfterWaves && !state.rules.attackMode){
                    //activate attack mode to destroy cores after waves are done.
                    state.rules.attackMode = true;
                    state.rules.waves = false;
                    Call.setRules(state.rules);
                }else{
                    Call.sectorCapture();
                }
            }
        }else{
            if(!state.rules.attackMode && state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }else if(state.rules.attackMode){
                //count # of teams alive
                int countAlive = state.teams.getActive().count(t -> t.hasCore() && t.team != Team.derelict);

                if((countAlive <= 1 || (!state.rules.pvp && state.rules.defaultTeam.core() == null)) && !state.gameOver){
                    //find team that won
                    TeamData left = state.teams.getActive().find(t -> t.hasCore() && t.team != Team.derelict);
                    Events.fire(new GameOverEvent(left == null ? Team.derelict : left.team));
                    state.gameOver = true;
                }
            }else if(!state.gameOver && state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning())){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.defaultTeam));
            }
        }
    }

    protected void updateWeather(){
        state.rules.weather.removeAll(w -> w.weather == null);

        for(WeatherEntry entry : state.rules.weather){
            //update cooldown
            entry.cooldown -= Time.delta;

            //create new event when not active
            if((entry.cooldown < 0 || entry.always) && !entry.weather.isActive()){
                float duration = entry.always ? Float.POSITIVE_INFINITY : Mathf.random(entry.minDuration, entry.maxDuration);
                entry.cooldown = duration + Mathf.random(entry.minFrequency, entry.maxFrequency);
                Tmp.v1.setToRandomDirection();
                Call.createWeather(entry.weather, entry.intensity, duration, Tmp.v1.x, Tmp.v1.y);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void sectorCapture(){
        //the sector has been conquered - waves get disabled
        state.rules.waves = false;

        if(state.rules.sector == null){
            //disable attack mode
            state.rules.attackMode = false;
            return;
        }

        boolean initial = !state.rules.sector.info.wasCaptured;

        state.rules.sector.info.wasCaptured = true;

        //fire capture event
        Events.fire(new SectorCaptureEvent(state.rules.sector, initial));

        //disable attack mode
        state.rules.attackMode = false;

        //map is over, no more world processor objective stuff
        state.rules.disableWorldProcessors = true;
        state.rules.objectives.clear();

        //save, just in case
        if(!headless && !net.client()){
            control.saves.saveSector(state.rules.sector);
        }
    }

    @Remote(called = Loc.both)
    public static void updateGameOver(Team winner){
        Events.fire(new GameOverEventClient(winner));
        state.gameOver = true;
        if(!headless){
            state.won = player.team() == winner;
        }
    }

    @Remote(called = Loc.both)
    public static void gameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        state.won = player.team() == winner;
        Time.run(60f * 3f, () -> ui.restart.show(winner));
        netClient.setQuiet();
//        if (net.client()) Events.fire(new GameOverEvent(winner));
    }

    //called when the remote server researches something
    @Remote
    public static void researched(Content content){
        if(!(content instanceof UnlockableContent u)) return;

        boolean was = u.unlockedNow();
        state.rules.researched.add(u.name);

        if(!was){
            Events.fire(new UnlockEvent(u));
        }
    }

    @Override
    public void dispose(){
        //save the settings before quitting
        netServer.admins.forceSave();
        Core.settings.manualSave();
    }

    @Override
    public void update(){
        Events.fire(Trigger.update);
        universe.updateGlobal();

        if(Core.settings.modified() && !state.isPlaying()){
            netServer.admins.forceSave();
            Core.settings.forceSave();
        }

        boolean runStateCheck = !net.client() && !world.isInvalidMap() && !state.isEditor() && state.rules.canGameOver;

        if(state.isGame()){
            if(!net.client()){
                state.enemies = Groups.unit.count(u -> u.team() == state.rules.waveTeam && u.isEnemy());
            }

            if(!state.isPaused()){
                float delta = Core.graphics.getDeltaTime();
                state.tick += Float.isNaN(delta) || Float.isInfinite(delta) ? 0f : delta * 60f;
                state.updateId ++;
                state.teams.updateTeamStats();
                //MapPreviewLoader.checkPreviews();

                if(state.rules.fog){
                    fogControl.update();
                }

                if(state.isCampaign()){
                    state.rules.sector.info.update();
                }

                if(state.isCampaign()){
                    universe.update();
                }
                Time.update();

                logicVars.update();

                //weather is serverside
                if(!net.client() && !state.isEditor()){
                    updateWeather();

                    for(TeamData data : state.teams.getActive()){
                        //does not work on PvP so built-in attack maps can have it on by default without issues
                        if(data.team.rules().buildAi && !state.rules.pvp){
                            if(data.buildAi == null) data.buildAi = new BaseBuilderAI(data);
                            data.buildAi.update();
                        }

                        if(data.team.rules().rtsAi){
                            if(data.rtsAi == null) data.rtsAi = new RtsAI(data);
                            data.rtsAi.update();
                        }
                    }
                }

                if(!state.isEditor()){
                    state.rules.objectives.update();
                    if(state.rules.objectives.checkChanged() && net.server()){
                        Call.setObjectives(state.rules.objectives);
                    }
                }

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!isWaitingWave()){
                        state.wavetime = Math.max(state.wavetime - Time.delta, 0);
                    }
                }

                if(!net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                //apply weather attributes
                state.envAttrs.clear();
                state.envAttrs.add(state.rules.attributes);
                Groups.weather.each(w -> state.envAttrs.add(w.weather.attrs, w.opacity));

                Groups.update();
                Client.INSTANCE.update();
            }

            if(runStateCheck){
                checkGameState();
            }
        }else if(netServer.isWaitingForPlayers() && runStateCheck){
            checkGameState();
        }
    }

    /** @return whether the wave timer is paused due to enemies */
    public boolean isWaitingWave(){
        return (state.rules.waitEnemies || (state.wave >= state.rules.winWave && state.rules.winWave > 0)) && state.enemies > 0;
    }
}
