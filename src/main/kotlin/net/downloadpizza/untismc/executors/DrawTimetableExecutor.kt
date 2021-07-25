package net.downloadpizza.untismc.executors

import net.downloadpizza.untiskt.FieldData
import net.downloadpizza.untiskt.LessonTime
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.logging.Logger
import java.time.LocalDate
import net.downloadpizza.untiskt.Period
import net.downloadpizza.untiskt.WebUntis
import org.bukkit.block.BlockFace
import org.bukkit.block.Sign
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import java.lang.Exception
import java.time.ZoneId
import java.time.format.DateTimeFormatter


const val WIDTH = 6

val GRID_BLOCK = Material.POLISHED_ANDESITE
val DEFAULT_BG = Material.LIGHT_GRAY_WOOL

val TIMES = arrayOf(
    LessonTime(800, 850),   // 1
    LessonTime(850, 940),   // 2
    LessonTime(955, 1045),  // 3
    LessonTime(1045, 1135), // 4
    LessonTime(1145, 1235), // 5
    LessonTime(1235, 1325), // 6
    LessonTime(1325, 1415), // 7
    LessonTime(1425, 1515), // 8
    LessonTime(1515, 1605), // 9
    LessonTime(1615, 1705), // 10
    LessonTime(1710, 1755), // 11
    LessonTime(1755, 1840), // 12
    LessonTime(1850, 1935)  // 13
)

class DrawTimetableExecutor(private val untis: WebUntis, private val world: World, private val logger: Logger) :
    CommandExecutor {

    private var current = LocalDate.now(ZoneId.of("GMT+1"))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        val x = run {
            val arg = args[0]
            if (arg == "~") {
                (sender as Player).location.x
            } else {
                arg.toDouble()
            }
        }
        val y = run {
            val arg = args[1]
            if (arg == "~") {
                (sender as Player).location.y
            } else {
                arg.toDouble()
            }
        }
        val z = run {
            val arg = args[2]
            if (arg == "~") {
                (sender as Player).location.z
            } else {
                arg.toDouble()
            }
        }

        val date = getFirstMonday(run {
            val arg = args[3]
            if (arg.startsWith('[')) {
                when (arg) {
                    "[next]" -> {
                        current = current.plusDays(7)
                    }
                    "[last]" -> {
                        current = current.minusDays(7)
                    }
                    "[reset]" -> {
                        current = LocalDate.now(ZoneId.of("GMT+1"))
                    }
                }
                current.format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
            } else {
                arg.also {
                    current = LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE)
                }.toInt()
            }
        })
        println(date)

        val orientation = when (args.getOrNull(4)?.toLowerCase()?.firstOrNull()) {
            'n' -> BlockFace.NORTH
            'e' -> BlockFace.EAST
            's' -> BlockFace.SOUTH
            'w' -> BlockFace.WEST
            else -> BlockFace.NORTH
        }

        val timetable: Map<Int, List<Period>> =
            untis.use {
                getTimetable(date, date + 4)
                    .filter { it.lessonTime.startTime in TIMES.map(LessonTime::startTime) }
                    .filter { it.lessonTime.endTime in TIMES.map(LessonTime::endTime) }
                    .filterNot { it.code == "cancelled" }
                    .filterNot { it.roomName.isEmpty() || it.className.isEmpty() || it.subjectName.isEmpty() }
                    .filterNot { it.subjectName[0].name.lastOrNull() == 'y' }
                    .sortedBy(Period::lessonTime)
                    .sortedBy(Period::date)
                    .groupBy { it.date - date }
            }

        val height = TIMES.size

        drawGrid(x.toInt(), y.toInt(), z.toInt(), height, orientation, date)


        for (i in 0 until 5) {
            val dayPeriods = timetable[i] ?: continue
            for (period in dayPeriods) {
                val startIndex = TIMES.map(LessonTime::startTime).indexOf(period.lessonTime.startTime)
                if (startIndex == -1) {
                    throw Exception("Should never happpen")
                }

                val endIndex = TIMES.map(LessonTime::endTime).indexOf(period.lessonTime.endTime)
                if (endIndex == -1) {
                    throw Exception("Should never happpen")
                }


                for (absolute in (startIndex..endIndex)) {
                    drawPeriod(
                        (x + (orientation.modX * (i + 1))).toInt(),
                        (y + height - absolute - 1).toInt(),
                        (z + (orientation.modZ * (i + 1))).toInt(),
                        period,
                        orientation
                    )
                }
            }
        }


        return true
    }

    private fun drawGrid(x: Int, y: Int, z: Int, height: Int, orientation: BlockFace = BlockFace.NORTH, date: Int) {
        val signFace = orientation.cw()


        for (i in 0..height) {
            val block = world.getBlockAt(x, y + i, z)
            block.type = GRID_BLOCK


            if (i in 1 until height) {
                val fgBlock = world.getBlockAt(x + signFace.modX, y + i, z + signFace.modZ)
                fgBlock.type = Material.BIRCH_WALL_SIGN

                val blockData = fgBlock.blockData as Directional
                blockData.facing = signFace
                fgBlock.blockData = blockData

                val line0 = (TIMES.size - i).toString()
                val line1 = "${TIMES[TIMES.size - i - 1].startTime} - ${TIMES[TIMES.size - i - 1].endTime}"

                with(world.getBlockAt(x + signFace.modX, y + i, z + signFace.modZ).state as Sign) {
                    setLine(0, line0)
                    setLine(1, line1)
                    update()
                }
            }


            val block2 = world.getBlockAt(x + (orientation.modX * WIDTH), y + i, z + (orientation.modZ * WIDTH))
            block2.type = GRID_BLOCK
        }
        for (i in 1 until WIDTH) {
            val block = world.getBlockAt(x + (orientation.modX * i), y, z + (orientation.modZ * i))
            block.type = GRID_BLOCK

            val block2 = world.getBlockAt(x + (orientation.modX * i), y + height, z + (orientation.modZ * i))
            block2.type = GRID_BLOCK

            if (i in 1..5) {
                val fgBlock = world.getBlockAt(
                    x + (orientation.modX * i) + (signFace.modX),
                    y + height,
                    z + (orientation.modZ * i) + (signFace.modZ)
                )
                fgBlock.type = Material.BIRCH_WALL_SIGN

                val blockData = fgBlock.blockData as Directional
                blockData.facing = signFace
                fgBlock.blockData = blockData
                with(
                    world.getBlockAt(
                        x + (orientation.modX * i) + (signFace.modX),
                        y + height,
                        z + (orientation.modZ * i) + (signFace.modZ)
                    ).state as Sign
                ) {
                    val dayStr = when (i) {
                        1 -> "Monday"
                        2 -> "Tuesday"
                        3 -> "Wednesday"
                        4 -> "Thurday"
                        5 -> "Friday"
                        else -> "Noneday"
                    }
                    setLine(0, dayStr)

                    val dateStr = date.toString()
                    val dayDate = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE).plusDays((i - 1).toLong())

                    val day = dayDate.dayOfMonth
                    val month = dayDate.monthValue
                    val year = dayDate.year
                    setLine(1, "$day.$month.$year")

                    update()
                }
            }
        }

        for (y_o in 1 until height) {
            for (v_o in 1 until WIDTH) {
                val localX = x + (orientation.modX * v_o)
                val localY = y + y_o
                val localZ = z + (orientation.modZ * v_o)
                val block = world.getBlockAt(localX, localY, localZ)
                block.type = DEFAULT_BG
                val fBlock = world.getBlockAt(localX + (signFace.modX), localY, localZ + (signFace.modZ))
                fBlock.type = Material.AIR
            }
        }
    }

    private fun drawPeriod(x: Int, y: Int, z: Int, period: Period, orientation: BlockFace) {
        val signFace = orientation.cw()
        val background =
            if ((period.code != "") || period.teacherName.isEmpty() || period.teacherName.any { it.name == "---" } || period.hasSub()) {
                Material.PURPLE_WOOL
            } else {
                Material.ORANGE_WOOL
            }
        val bgBlock = world.getBlockAt(x, y, z)
        bgBlock.type = background

        val fgBlock = world.getBlockAt(x + signFace.modX, y, z + signFace.modZ)
        fgBlock.type = Material.BIRCH_WALL_SIGN

        val blockData = fgBlock.blockData as Directional
        blockData.facing = signFace
        fgBlock.blockData = blockData

        val substitutionString: (FieldData?) -> String = {
            if (it == null) {
                ""
            } else {
                if (it.isSub()) {
                    "${it.name} (${it.orgname})"
                } else {
                    it.name
                }
            }
        }

        with(world.getBlockAt(x + signFace.modX, y, z + signFace.modZ).state as Sign) {
            setLine(0, substitutionString(period.subjectName[0]))
            setLine(1, substitutionString(period.roomName[0]))
            setLine(2, substitutionString(period.teacherName[0]))
            setLine(3, substitutionString(period.teacherName.getOrNull(1)))

            update()
        }
    }
}

fun getFirstMonday(date: Int): Int {
    val dateStr = date.toString()
    require(dateStr.length == 8) { "Date should be at 8 chars long" }
    val year = dateStr.substring(0 until 4).toInt()
    val month = dateStr.substring(4 until 6).toInt()
    val day = dateStr.substring(6 until 8).toInt()

    val ld = LocalDate.of(year, month, day)
    return date - ld.dayOfWeek.ordinal
}

fun BlockFace.cw() = when (this) {
    BlockFace.NORTH -> BlockFace.EAST
    BlockFace.EAST -> BlockFace.SOUTH
    BlockFace.SOUTH -> BlockFace.WEST
    BlockFace.WEST -> BlockFace.NORTH
    else -> throw IllegalArgumentException("Blockface should only be cardinal direction")
}

fun FieldData.isSub() = this.orgname != null

fun Array<FieldData>.anySub() = this.any(FieldData::isSub)

fun Period.hasSub() = (roomName.anySub() || teacherName.anySub() || subjectName.anySub())