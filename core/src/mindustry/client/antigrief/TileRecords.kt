package mindustry.client.antigrief

import arc.*
import arc.math.*
import arc.struct.Queue
import arc.util.*
import mindustry.*
import mindustry.ai.types.*
import mindustry.client.*
import mindustry.client.ui.PanelFragment
import mindustry.client.utils.*
import mindustry.content.*
import mindustry.core.ActionsHistory
import mindustry.game.*
import mindustry.gen.*
import mindustry.world.*
import mindustry.world.blocks.*
import mindustry.world.blocks.storage.StorageBlock
import java.time.*
import kotlin.math.*

object TileRecords {
    private var records: Array<Array<TileRecord>> = arrayOf(arrayOf())
    var reslogs: Array<TileRecord> = arrayOf()
    var history: ArrayList<String> = arrayListOf()
    var joinTime: Instant = Instant.EPOCH

    fun initialize() {
        Events.on(EventType.WorldLoadEvent::class.java) {
            val startTime = Time.globalTime / 60.0 - Vars.state.tick / 60.0
            var sameMap = abs(ClientVars.lastServerStartTime - startTime) < 10 // if start time of map is within 10s of the previous start time
            sameMap = sameMap && records.isNotEmpty() && Vars.state.map.name() == ClientVars.lastServerName &&
                    Vars.world.width() == records.size && Vars.world.height() == records[0].size

            ClientVars.lastServerStartTime = startTime
            ClientVars.lastServerName = Vars.state.map.name()
            if (!ClientVars.syncing && !sameMap) {
                if(!PanelFragment.forcesavelogs) {
                    records = Array(Vars.world.width()) { x -> Array(Vars.world.height()) { y -> TileRecord(x, y) } }
                    joinTime = Instant.now()
                    ActionsHistory.clearactionhistory()
                }
            }
        }

        Events.on(EventType.BlockBuildBeginEventBefore::class.java) {
            if (it.newBlock == null || it.newBlock == Blocks.air) {
                if(it.unit.isPlayer) addLogH(TileBreakLog(it.tile, it.unit.toInteractor(), it.tile.block()))
                it.tile.getLinkedTiles { tile ->
                    addLog(tile, TileBreakLog(tile, it.unit.toInteractor(), tile.block()))
                }
            } else { // FINISHME: slightly very inefficient?
                if(it.unit.isPlayer) addLogH(TilePlacedLog(it.tile, it.unit.toInteractor(), it.newBlock, -1, null, it.tile == it.tile))
                it.tile.getLinkedTilesAs(it.newBlock) { tile ->
                    val log = TilePlacedLog(tile, it.unit.toInteractor(),
                        it.newBlock, -1, null, tile == it.tile)
                    addLog(tile, log)
                    Core.app.post { // When BlockBuildBeginEvent is fired. Or the building is just rotated.
                        log.updateLog(tile.build?.rotation, tile.build?.config())
                    }
                }
            }
        }

        Events.on(EventType.BlockBuildEndEvent::class.java) {
            if (it.breaking) return@on
            it.tile.getLinkedTiles { tile ->
                val sequence = this[tile]?.sequences ?: return@getLinkedTiles
                (sequence.last().logs.lastOrNull() as? TilePlacedLog)?.configuration = it.tile.build?.config() ?: return@getLinkedTiles // FINISHME: Build is nullable for some reason (see https://discord.com/channels/965438060508631050/965438061003550722/1039950910295658600)
            }
        }

        Events.on(EventType.ConfigEventBefore::class.java) {
            if (it.player != null) Seer.blockConfig(it.player, it.tile.tile, it.value)
            addLogH(ConfigureTileLog(it.tile.tile, it.player.toInteractor(), it.tile.tile.block(), it.tile.rotation, it.value))
            it.tile.tile.getLinkedTiles { tile ->
                addLog(tile, ConfigureTileLog(tile, it.player.toInteractor(), tile.block(), it.tile.rotation, it.value))
            }
        }

        Events.on(EventType.BuildPayloadPickup::class.java) {
            it.tile.getLinkedTiles { tile ->
                addLog(tile, BlockPayloadPickupLog(tile, it.unit.toInteractor(), it.building.block))
            }
        }

        Events.on(EventType.BuildPayloadDrop::class.java) {
            it.tile.getLinkedTilesAs(it.building.block) { tile ->
                addLog(tile, BlockPayloadDropLog(tile, it.unit.toInteractor(), it.building.block, it.building.rotation, it.building.config(), isOrigin(tile)))
            }
        }

        Events.on(EventType.BlockDestroyEvent::class.java) {
            if (it.tile.team() != Vars.player.team()) return@on // Couldn't care less about enemies, especially in flood
            it.tile.getLinkedTiles { tile ->
                addLog(tile, TileDestroyedLog(tile,
                    if (tile.build is ConstructBlock.ConstructBuild) (tile.build as ConstructBlock.ConstructBuild).current ?:
                    (tile.build as ConstructBlock.ConstructBuild).previous
                    else tile.block() ?: Blocks.air))
            }
        }

        Events.on(EventType.UnitDeadEvent::class.java) {
            if(it.unit == null || it.unit.team() != Vars.player.team() || it.unit.tileOn() == null) return@on
            val controller = it.unit.controller()
            if(controller !is LogicAI && controller !is Player) return@on

            val threshold = it.unit.type.hitSize * it.unit.type.hitSize + 0.01f
            for (point in TileLog.linkedArea(it.unit.tileOn(), Mathf.ceil(it.unit.type.hitSize / Vars.tilesize))) {
                if (point in Vars.world && it.unit.within(Vars.world[point], threshold)) {
                    val tile = Vars.world[point]
                    addLog(tile, UnitDestroyedLog(tile, it.unit.toInteractor(), it.unit, controller is Player))
                }
            }
        }

        Events.on(EventType.BuildRotateEvent::class.java) {
            val player = it.unit?.player ?: return@on
            val direction = rotationDirection(it.previous, it.build.rotation)
            if(it.unit.player != null) addLogH(RotateTileLog(it.build.tile, it.unit.player.toInteractor(), it.build.block, it.build.rotation, direction))
            it.build.tile.getLinkedTiles { tile ->
                addLog(tile, RotateTileLog(tile, player.toInteractor(), it.build.block, it.build.rotation, direction))
            }
        }
        Events.on(EventType.BuildingCommandEvent::class.java) {
            //if(it.player != null) addLogH(CommandTileLog(it.building.tile, it.player.toInteractor(), it.building.block, it.position))
            it.building.tile.getLinkedTiles { tile ->
                addLog(tile, CommandTileLog(tile, it.player.toInteractor(), it.building.block, it.position))
            }
        }

        Events.on(EventType.DepositEvent::class.java) {
            if(!Core.settings.getBool("itemslog")) return@on
            //it.tile.tile.getLinkedTilesAs(it.tile.tile.build.block) { tile ->addLog(tile, DepositTileLog(tile, it.player.toInteractor(), it.item, it.amount, it.tile.tile.build))}
        }

        Events.on(EventType.WithdrawEvent::class.java) {
            if(!Core.settings.getBool("itemslog")) return@on
            //it.tile.tile.getLinkedTilesAs(it.tile.tile.build.block) { tile ->addLog(tile, WithdrawTileLog(tile, it.player.toInteractor(), it.item, it.amount, it.tile.tile.build))}
        }
    }

    operator fun get(x: Int, y: Int): TileRecord? = records.getOrNull(x)?.getOrNull(y)

    operator fun get(tile: Tile): TileRecord? = this[tile.x.toInt(), tile.y.toInt()]

    private fun addLog(tile: Tile, log: TileLog) {
        val logs = this[tile] ?: return
        logs.add(log, tile)
    }

    private fun addLogH(log: TileLog) {
        addHistoryLog(log)
    }
    private fun addHistoryLog(log: TileLog){
        //if(log.toShortString().contains(Core.bundle.get("client.destroyed"))){return}
        if(history.size>7){
            history.removeAt(0)
        }
        //var i = 0;
        var done = false;
        if(history.size>0)
            for (i in 0..(history.size-1)){
                if(history[i].startsWith(log.toShortString())){
                    var nya = history[i].substring(log.toShortString().length+2);
                    var neko = 0;
                    try{
                        neko = Integer.parseInt(nya)+1
                    }
                    catch (e: NumberFormatException){}
                    history[i] = log.toShortString()+" x" + neko
                    //player.sendMessage("nya^ " + nya + "  neko^ " + neko)
                    done=true;
                }
            }
        if(!done) {
            history.add(log.toShortString() + " x1");
        }
    }
    fun show(tile: Tile) {
        dialog("Logs") {
            cont.add(TileRecords[tile]?.toElement())
            addCloseButton()
        }.show()
    }

    fun isOrigin(tile: Tile): Boolean {
//        return tile.build?.pos() == tile.pos()
        return tile.build?.tile == tile // Ahem. Why did this break everything
    }
}
