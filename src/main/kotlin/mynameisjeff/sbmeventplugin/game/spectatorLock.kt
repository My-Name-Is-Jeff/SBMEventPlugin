package mynameisjeff.sbmeventplugin.game

import com.booksaw.betterTeams.Team
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import mynameisjeff.sbmeventplugin.isPlaying
import mynameisjeff.sbmeventplugin.isVanished
import net.axay.kspigot.chat.sendText
import net.axay.kspigot.event.listen
import net.axay.kspigot.extensions.bukkit.toComponent
import net.axay.kspigot.runnables.taskRunLater
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent


fun loadSpectatorLock() {
    listen<PlayerJoinEvent> { e ->
        if (e.player.isVanished) return@listen
        if (e.player.gameMode == GameMode.SPECTATOR) {
            val myTeam = Team.getTeam(e.player) ?: return@listen
            if (myTeam.onlineMemebers.none { it != e.player && it.isPlaying }) {
                e.player.kick("§cYou joined as spectator and your team has no more alive players.".toComponent())
            } else {
                val nearestTeammate = myTeam.onlineMemebers.filter { it != e.player && it.isPlaying }.minByOrNull { it.location.distance(e.player.location) }
                if (nearestTeammate != null) {
                    e.player.sendText {
                        text("§aYou are now spectating ")
                        component(nearestTeammate.teamDisplayName())
                        text("§a.")
                    }
                }
            }
        }
    }
    listen<PlayerStartSpectatingEntityEvent> { e ->
        if (e.player.isVanished) return@listen
        if (e.newSpectatorTarget !is Player) {
            e.player.sendText {
                text("§cYou can only spectate players.")
            }
            e.isCancelled = true
            return@listen
        }
        val myTeam = Team.getTeam(e.player) ?: return@listen
        if (myTeam.onlineMemebers.any { it != e.player && it.isPlaying }) {
            if (!myTeam.members.contains(e.newSpectatorTarget as Player)) {
                e.player.sendText {
                    text("§cYou still have alive teammates, so you can't start spectating")
                    component(e.newSpectatorTarget.teamDisplayName())
                    text("§c.")
                }
                e.isCancelled = true
            }
        } else {
            e.player.kick("§cYou started spectating and your team has no more alive players.".toComponent())
        }
    }
    listen<PlayerStopSpectatingEntityEvent> { e ->
        if (e.player.isVanished || e.player.gameMode != GameMode.SPECTATOR) return@listen
        if (e.spectatorTarget !is Player) return@listen
        val myTeam = Team.getTeam(e.player) ?: return@listen
        if (myTeam.onlineMemebers.any { it != e.player && it.isPlaying }) {
            e.player.sendText {
                text("§cYou still have alive teammates, so you can't stop spectating.")
            }
            if (!(e.spectatorTarget as Player).isOnline) {
                val nearestTeammate = myTeam.onlineMemebers.filter { it != e.player && it.isPlaying }.minByOrNull { it.location.distance(e.player.location) }
                if (nearestTeammate != null) {
                    e.player.sendText {
                        text("§aThe player you were spectating disconnected, so you are now spectating ")
                        component(nearestTeammate.teamDisplayName())
                        text("§a.")
                    }
                } else {
                    e.player.kick("§cThe player you were spectating disconnected and your team has no more alive players.".toComponent())
                }
            }
            e.isCancelled = true
        } else {
            e.player.kick("§cYou stopped spectating and your team has no more alive players.".toComponent())
        }
    }
    listen<PlayerGameModeChangeEvent> { e ->
        if (e.cause == PlayerGameModeChangeEvent.Cause.HARDCORE_DEATH) {
            val myTeam = Team.getTeam(e.player) ?: return@listen
            val nearestTeammate = myTeam.onlineMemebers.filter { it != e.player && it.isPlaying }.minByOrNull { it.location.distance(e.player.location) }
            if (nearestTeammate != null) {
                taskRunLater(1L) {
                    e.player.spectatorTarget = nearestTeammate
                    e.player.sendText {
                        text("§cYou died!")
                        newLine()
                        text("§aYou still have alive teammates, so you are now spectating ")
                        component(nearestTeammate.teamDisplayName())
                        text("§a.")
                    }
                }
            } else {
                taskRunLater(1L) {
                    e.player.kick("§cYou died and your team has no more alive players.".toComponent())
                }
            }
        }
    }
}