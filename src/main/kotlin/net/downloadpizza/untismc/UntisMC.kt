package net.downloadpizza.untismc

import net.downloadpizza.untiskt.WebUntis
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin

import net.downloadpizza.untismc.executors.DrawTimetableExecutor
import org.bukkit.Bukkit
import org.bukkit.World

class UntisMC : JavaPlugin() {

    private lateinit var untis: WebUntis
    private lateinit var world: World

    private val user = "secret"
    private val password = "secret"

    // (aus der URL nehmen, bsp https://blabla.webuntis.com/WebUntis/?school=DieSchuleHalt#/basic/login)
    private val server = "https://blabla.webuntis.com"
    private val school = "DieSchuleHalt"
    override fun onEnable() {

        untis = WebUntis(user, password,  server, school)

        world = Bukkit.getWorld("world") ?: Bukkit.getWorld("test")!!

        "drawtimetable" executes DrawTimetableExecutor(untis, world, logger)

        logger.info("Started UntisMC")
    }

    private infix fun String.executes(exec: CommandExecutor) = getCommand(this)?.setExecutor(exec)
}
