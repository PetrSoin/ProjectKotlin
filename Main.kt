package backend

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import frontend.buildPage

enum class Status {
    TODO,          // Сделать
    IN_PROGRESS,   // В работе
    REVIEW,        // Показать заказчику
    DONE           // Готово
}

// Модель задачи
data class Task(
    val id: Int,
    val title: String,
    val body: String,
    val status: Status,
    val dueDate: String,
    val participants: String,
    val hashtags: String
)

// Начальный список задач (теперь var, чтобы /edit мог перезаписать)
@Volatile
var tasks: List<Task> = listOf(
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

private val tasksLock = Any() // на всякий случай, чтобы запись/чтение не конфликтовали

fun main() {
    // поднимаем простой HTTP-сервер на 8080
    val server = HttpServer.create(InetSocketAddress(8080), 0)

    // GET /
    server.createContext("/") { exchange ->
        // "фронтенд" генерирует HTML-страницу по списку задач
        val snapshot = synchronized(tasksLock) { tasks }
        val response = buildPage(snapshot)

        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        val bytes = response.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())

        exchange.responseBody.use { os ->
            os.write(bytes)
        }
    }

    // POST /edit — принимает JSON и полностью перезаписывает список tasks
    server.createContext("/edit") { exchange ->
        // небольшой CORS-хелп (не мешает даже при same-origin)
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "POST, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")

        val method = exchange.requestMethod.uppercase()

        if (method == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return@createContext
        }

        if (method != "POST") {
            val msg = """{"ok":false,"error":"method_not_allowed"}"""
            val bytes = msg.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(405, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            return@createContext
        }

        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)

        try {
            val newTasks = parseTasksFromEditPayload(body)

            synchronized(tasksLock) {
                tasks = newTasks
            }

            val msg = """{"ok":true,"count":${newTasks.size}}"""
            val bytes = msg.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } catch (e: Exception) {
            val safe = (e.message ?: "bad_request").replace("\"", "'")
            val msg = """{"ok":false,"error":"$safe"}"""
            val bytes = msg.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(400, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    server.executor = null
    server.start()
    println("Канбан-доска запущена: http://localhost:8080")
}

/**
 * Ожидаемый payload от фронта:
 * {
 *   "boardKey": "...",
 *   "boardName": "...",
 *   "reason": "...",
 *   "updatedAt": "...",
 *   "tasks": [
 *     { "id": 1, "title": "...", "body": "...", "status": "TODO", "dueDate": "...", "participants": "...", "hashtags": "...", "order": 0 }
 *   ]
 * }
 */
private fun parseTasksFromEditPayload(jsonText: String): List<Task> {
    val root = JsonParser(jsonText).parse()

    val obj = root as? Map<*, *> ?: throw IllegalArgumentException("root_not_object")
    val tasksAny = obj["tasks"] ?: throw IllegalArgumentException("tasks_missing")
    val arr = tasksAny as? List<*> ?: throw IllegalArgumentException("tasks_not_array")

    val parsed = arr.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null

        val idAny = m["id"]
        val id = when (idAny) {
            is Number -> idAny.toInt()
            is String -> idAny.toIntOrNull()
            else -> null
        } ?: throw IllegalArgumentException("task_id_missing")

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

    // сохраняем порядок из payload: сначала по "order" (если есть), иначе как пришло
    // (у тебя фронт присылает order)
    val withOrder = arr.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val idAny = m["id"]
        val id = when (idAny) {
            is Number -> idAny.toInt()
            is String -> idAny.toIntOrNull()
            else -> null
        } ?: return@mapNotNull null
        val orderAny = m["order"]
        val order = when (orderAny) {
            is Number -> orderAny.toInt()
            is String -> orderAny.toIntOrNull()
            else -> null
        } ?: 0
        id to order
    }.toMap()

    return parsed.sortedWith(compareBy<Task> { it.status.ordinal }.thenBy { withOrder[it.id] ?: 0 })
}

/* -----------------------------
   Мини JSON-парсер (без зависимостей)
   Поддерживает: object/array/string/number/true/false/null
-------------------------------- */

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
