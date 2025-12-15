package backend

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import frontend.buildPage

enum class Status {
    TODO,          // Сделать
    IN_PROGRESS,   // В работе
    REVIEW,        // Показать заказчику
    DONE           // Готово
}

// Модель задачи (как в твоём коде)
data class Task(
    val id: Int,
    val title: String,
    val body: String,
    val status: Status,
    val dueDate: String,
    val participants: String,
    val hashtags: String
)

// Доска: ключ + имя + список задач
data class Board(
    val key: String,
    var name: String,
    var tasks: MutableList<Task>
)

// Файл долгосрочного хранения
private val DATA_FILE = File("kanban_state.json")

private val stateLock = Any()
private val boards = LinkedHashMap<String, Board>()

private fun initialDefaultTasks(): MutableList<Task> = mutableListOf(
    Task(
        id = 1,
        title = "Сделать макет главной страницы",
        body = "Нарисовать макет и согласовать с заказчиком.",
        status = Status.TODO,
        dueDate = "2025-10-20",
        participants = "Никита",
        hashtags = "#дизайн #главная"
    ),
    Task(
        id = 2,
        title = "Сверстать канбан-доску",
        body = "Сделать 4 колонки и кликабельные карточки.",
        status = Status.IN_PROGRESS,
        dueDate = "2025-10-22",
        participants = "Никита, Иван",
        hashtags = "#frontend #kanban"
    ),
    Task(
        id = 3,
        title = "Подготовить демо для заказчика",
        body = "Показать рабочий прототип преподавателю.",
        status = Status.REVIEW,
        dueDate = "2025-10-23",
        participants = "Никита",
        hashtags = "#демо"
    ),
    Task(
        id = 4,
        title = "Написать отчёт по практике",
        body = "Скриншоты, описание архитектуры, выводы.",
        status = Status.DONE,
        dueDate = "2025-10-25",
        participants = "Никита",
        hashtags = "#отчёт"
    )
)

fun main() {
    loadStateOrInit()

    // поднимаем простой HTTP-сервер на 8080
    val server = HttpServer.create(InetSocketAddress(8080), 0)

    // GET /
    server.createContext("/") { exchange ->
        val snapshot = synchronized(stateLock) {
            val def = boards.getOrPut("default") {
                Board("default", "Моя канбан-доска", initialDefaultTasks())
            }
            def.tasks.toList()
        }

        val response = buildPage(snapshot)

        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        val bytes = response.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { os -> os.write(bytes) }
    }

    // GET /state  -> { boards:[ {key,name,tasks:[...]} ] }
    server.createContext("/state") { exchange ->
        addCors(exchange)

        val method = exchange.requestMethod.uppercase()
        if (method == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return@createContext
        }
        if (method != "GET") {
            respondJson(exchange, 405, """{"ok":false,"error":"method_not_allowed"}""")
            return@createContext
        }

        val json = synchronized(stateLock) { stateToJson() }
        respondJson(exchange, 200, json)
    }

    // POST /edit — обновляет (перезаписывает) задачи конкретной доски и сохраняет в файл
    server.createContext("/edit") { exchange ->
        addCors(exchange)

        val method = exchange.requestMethod.uppercase()
        if (method == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return@createContext
        }
        if (method != "POST") {
            respondJson(exchange, 405, """{"ok":false,"error":"method_not_allowed"}""")
            return@createContext
        }

        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)

        try {
            val req = parseEditRequest(body)

            synchronized(stateLock) {
                val boardKey = req.boardKey.ifBlank { "default" }
                val boardName = req.boardName.ifBlank { if (boardKey == "default") "Моя канбан-доска" else boardKey }

                val board = boards.getOrPut(boardKey) {
                    Board(boardKey, boardName, mutableListOf())
                }
                // обновляем имя, если пришло
                if (req.boardName.isNotBlank()) {
                    board.name = req.boardName
                }

                board.tasks = req.tasks.toMutableList()

                saveStateUnsafe()
            }

            respondJson(exchange, 200, """{"ok":true}""")
        } catch (e: Exception) {
            val safe = (e.message ?: "bad_request").replace("\"", "'")
            respondJson(exchange, 400, """{"ok":false,"error":"$safe"}""")
        }
    }

    server.executor = null
    server.start()
    println("Канбан-доска запущена: http://localhost:8080")
}

/* ======================== PERSISTENCE ======================== */

private fun loadStateOrInit() {
    synchronized(stateLock) {
        boards.clear()

        val loaded = runCatching { loadStateFromFile(DATA_FILE) }.getOrNull()
        if (loaded != null && loaded.isNotEmpty()) {
            boards.putAll(loaded)
        } else {
            boards["default"] = Board("default", "Моя канбан-доска", initialDefaultTasks())
            // сразу сохраним, чтобы файл появился
            saveStateUnsafe()
        }
    }
}

private fun saveStateUnsafe() {
    val json = stateToJson()
    val tmp = File(DATA_FILE.absolutePath + ".tmp")
    Files.writeString(tmp.toPath(), json, StandardCharsets.UTF_8)
    try {
        Files.move(
            tmp.toPath(),
            DATA_FILE.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    } catch (_: Exception) {
        // если ATOMIC_MOVE не поддержан
        Files.move(tmp.toPath(), DATA_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun stateToJson(): String {
    val sb = StringBuilder()
    sb.append("{\"boards\":[")
    var firstBoard = true
    for ((_, b) in boards) {
        if (!firstBoard) sb.append(",")
        firstBoard = false
        sb.append("{")
        sb.append("\"key\":\"").append(jsonEscape(b.key)).append("\",")
        sb.append("\"name\":\"").append(jsonEscape(b.name)).append("\",")
        sb.append("\"tasks\":[")
        var firstTask = true
        for (t in b.tasks) {
            if (!firstTask) sb.append(",")
            firstTask = false
            sb.append(taskToJson(t))
        }
        sb.append("]}")
    }
    sb.append("]}")
    return sb.toString()
}

private fun taskToJson(t: Task): String {
    return buildString {
        append("{")
        append("\"id\":").append(t.id).append(",")
        append("\"title\":\"").append(jsonEscape(t.title)).append("\",")
        append("\"body\":\"").append(jsonEscape(t.body)).append("\",")
        append("\"status\":\"").append(t.status.name).append("\",")
        append("\"dueDate\":\"").append(jsonEscape(t.dueDate)).append("\",")
        append("\"participants\":\"").append(jsonEscape(t.participants)).append("\",")
        append("\"hashtags\":\"").append(jsonEscape(t.hashtags)).append("\"")
        append("}")
    }
}

private fun jsonEscape(s: String): String {
    val out = StringBuilder(s.length + 8)
    for (ch in s) {
        when (ch) {
            '\\' -> out.append("\\\\")
            '"'  -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> out.append(ch)
        }
    }
    return out.toString()
}

/* ======================== /edit parsing ======================== */

private data class EditRequestParsed(
    val boardKey: String,
    val boardName: String,
    val tasks: List<Task>
)

private fun parseEditRequest(jsonText: String): EditRequestParsed {
    val rootAny = JsonParser(jsonText).parse()
    val root = rootAny as? Map<*, *> ?: throw IllegalArgumentException("root_not_object")

    val boardKey = root["boardKey"]?.toString() ?: "default"
    val boardName = root["boardName"]?.toString() ?: ""

    val tasksAny = root["tasks"]
    val arr = (tasksAny as? List<*>) ?: emptyList<Any?>()

    val tasks = arr.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null

        val idAny = m["id"]
        val id = when (idAny) {
            is Number -> idAny.toInt()
            is String -> idAny.toIntOrNull()
            else -> null
        } ?: return@mapNotNull null

        val title = m["title"]?.toString() ?: ""
        val body = m["body"]?.toString() ?: ""

        val statusStr = (m["status"]?.toString() ?: "TODO").trim()
        val status = runCatching { Status.valueOf(statusStr) }.getOrElse { Status.TODO }

        val dueDate = m["dueDate"]?.toString() ?: ""
        val participants = m["participants"]?.toString() ?: ""
        val hashtags = m["hashtags"]?.toString() ?: ""

        Task(
            id = id,
            title = title,
            body = body,
            status = status,
            dueDate = dueDate,
            participants = participants,
            hashtags = hashtags
        )
    }

    return EditRequestParsed(boardKey, boardName, tasks)
}

/* ======================== /state load ======================== */

private fun loadStateFromFile(file: File): Map<String, Board> {
    if (!file.exists()) return emptyMap()

    val text = Files.readString(file.toPath(), StandardCharsets.UTF_8)
    val rootAny = JsonParser(text).parse()
    val root = rootAny as? Map<*, *> ?: return emptyMap()

    val boardsArr = root["boards"] as? List<*> ?: return emptyMap()

    val out = LinkedHashMap<String, Board>()
    for (bAny in boardsArr) {
        val bObj = bAny as? Map<*, *> ?: continue
        val keyRaw = bObj["key"]?.toString() ?: continue
        val key = if (keyRaw.isBlank()) continue else keyRaw
        val name = bObj["name"]?.toString() ?: key
        val tasksArr = bObj["tasks"] as? List<*> ?: emptyList<Any?>()

        val parsedTasks = tasksArr.mapNotNull { tAny ->
            val tm = tAny as? Map<*, *> ?: return@mapNotNull null
            val idAny = tm["id"]
            val id = when (idAny) {
                is Number -> idAny.toInt()
                is String -> idAny.toIntOrNull()
                else -> null
            } ?: return@mapNotNull null

            val title = tm["title"]?.toString() ?: ""
            val body = tm["body"]?.toString() ?: ""
            val statusStr = (tm["status"]?.toString() ?: "TODO").trim()
            val status = runCatching { Status.valueOf(statusStr) }.getOrElse { Status.TODO }
            val dueDate = tm["dueDate"]?.toString() ?: ""
            val participants = tm["participants"]?.toString() ?: ""
            val hashtags = tm["hashtags"]?.toString() ?: ""

            Task(id, title, body, status, dueDate, participants, hashtags)
        }.toMutableList()

        out[key] = Board(key, name, parsedTasks)
    }

    return out
}

/* ======================== HTTP helpers ======================== */

private fun addCors(exchange: com.sun.net.httpserver.HttpExchange) {
    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
    exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
}

private fun respondJson(exchange: com.sun.net.httpserver.HttpExchange, code: Int, json: String) {
    val bytes = json.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}

/* ======================== Minimal JSON parser (no deps) ======================== */

private class JsonParser(private val s: String) {
    private var i = 0

    fun parse(): Any? {
        skipWs()
        val v = parseValue()
        skipWs()
        if (i != s.length) error("trailing_chars")
        return v
    }

    private fun parseValue(): Any? {
        skipWs()
        if (i >= s.length) error("unexpected_eof")

        return when (val c = s[i]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            '-', in '0'..'9' -> parseNumber()
            't' -> { expect("true"); true }
            'f' -> { expect("false"); false }
            'n' -> { expect("null"); null }
            else -> error("bad_value_char:$c")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        consume('{')
        skipWs()
        if (peek('}')) {
            consume('}')
            return emptyMap()
        }

        val map = LinkedHashMap<String, Any?>()
        while (true) {
            skipWs()
            val key = parseString()
            skipWs()
            consume(':')
            val value = parseValue()
            map[key] = value
            skipWs()
            when {
                peek(',') -> { consume(','); continue }
                peek('}') -> { consume('}'); break }
                else -> error("object_expected_comma_or_brace")
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        consume('[')
        skipWs()
        if (peek(']')) {
            consume(']')
            return emptyList()
        }

        val list = ArrayList<Any?>()
        while (true) {
            val v = parseValue()
            list.add(v)
            skipWs()
            when {
                peek(',') -> { consume(','); continue }
                peek(']') -> { consume(']'); break }
                else -> error("array_expected_comma_or_bracket")
            }
        }
        return list
    }

    private fun parseString(): String {
        consume('"')
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (i >= s.length) error("bad_escape_eof")
                    val e = s[i++]
                    when (e) {
                        '"', '\\', '/' -> sb.append(e)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (i + 4 > s.length) error("bad_unicode_escape")
                            val hex = s.substring(i, i + 4)
                            i += 4
                            val code = hex.toIntOrNull(16) ?: error("bad_unicode_hex")
                            sb.append(code.toChar())
                        }
                        else -> error("bad_escape_char:$e")
                    }
                }
                else -> sb.append(c)
            }
        }
        error("string_not_closed")
    }

    private fun parseNumber(): Number {
        val start = i
        if (peek('-')) i++
        if (i >= s.length) error("bad_number")

        while (i < s.length && s[i] in '0'..'9') i++

        var isFloat = false
        if (i < s.length && s[i] == '.') {
            isFloat = true
            i++
            while (i < s.length && s[i] in '0'..'9') i++
        }

        if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
            isFloat = true
            i++
            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
            while (i < s.length && s[i] in '0'..'9') i++
        }

        val numStr = s.substring(start, i)
        return if (isFloat) {
            numStr.toDoubleOrNull() ?: error("bad_number_format")
        } else {
            numStr.toLongOrNull() ?: error("bad_number_format")
        }
    }

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun expect(word: String) {
        if (s.regionMatches(i, word, 0, word.length, ignoreCase = false)) {
            i += word.length
        } else {
            error("expected_$word")
        }
    }

    private fun consume(ch: Char) {
        if (i >= s.length || s[i] != ch) error("expected_$ch")
        i++
    }

    private fun peek(ch: Char): Boolean = i < s.length && s[i] == ch

    private fun error(msg: String): Nothing = throw IllegalArgumentException(msg)
}
