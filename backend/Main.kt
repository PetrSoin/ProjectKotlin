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

// Начальный список задач
val tasks = listOf(
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
    // поднимаем простой HTTP-сервер на 8080
    val server = HttpServer.create(InetSocketAddress(8080), 0)

    server.createContext("/") { exchange ->
        // "фронтенд" генерирует HTML-страницу по списку задач
        val response = buildPage(tasks)

        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        val bytes = response.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())

        exchange.responseBody.use { os ->
            os.write(bytes)
        }
    }

    server.executor = null
    server.start()
    println("Канбан-доска запущена: http://localhost:8080")
}
