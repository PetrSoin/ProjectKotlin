package frontend

import backend.Status
import backend.Task

/**
 * Генерация полной HTML-страницы.
 * Здесь "фронтэнд": верстка, стили, JS-логика.
 */
fun buildPage(tasks: List<Task>): String {
    return """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <title>Канбан-доска</title>
            <style>
                body {
                    margin: 0;
                    font-family: Arial, sans-serif;
                    background: #f0f0f0;
                }

                /* Верхняя панель с иконкой профиля и меню */
                .topbar {
                    height: 48px;
                    background: #f5f5f5;
                    border-bottom: 1px solid #ddd;
                    display: flex;
                    align-items: center;
                    padding: 0 16px;
                    box-sizing: border-box;
                }

                .topbar-left {
                    display: flex;
                    align-items: center;
                }

                .profile-avatar {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    background: #cfcfcf;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-right: 12px;
                }

                /* Внутри аватарки делаем условный "силуэт" */
                .profile-avatar::before {
                    content: "";
                    width: 60%;
                    height: 60%;
                    border-radius: 50%;
                    background: #f5f5f5;
                    display: block;
                }

                .topbar-menu {
                    display: flex;
                    align-items: center;
                    gap: 16px;
                    font-size: 14px;
                }

                .topbar-menu-item {
                    cursor: pointer;
                    color: #333;
                }

                .topbar-menu-item:hover {
                    text-decoration: underline;
                }

                .app {
                    display: flex;
                    height: calc(100vh - 48px); /* вычитаем высоту верхней панели */
                }

                .board {
                    flex: 2;
                    padding: 16px;
                    background: #ffffff;
                    box-sizing: border-box;
                    overflow: auto;
                    border-right: 1px solid #ddd;
                }

                .board-header {
                    font-size: 20px;
                    margin-bottom: 12px;
                }

                .columns {
                    display: flex;
                    gap: 8px;
                    height: calc(100% - 40px);
                }

                .column {
                    flex: 1;
                    background: #fafafa;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    padding: 8px;
                    box-sizing: border-box;
                }

                .column-title {
                    font-weight: bold;
                    text-align: center;
                    margin-bottom: 8px;
                }

                .column-body {
                    border-top: 1px solid #ddd;
                    padding-top: 8px;
                    min-height: 100px;
                }

                .card-stub {
                    width: 90%;
                    margin: 8px auto;
                    border-radius: 4px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    cursor: pointer;
                    padding: 6px 8px;
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    font-size: 12px;
                }

                .card-stub.yellow {
                    background: #fff9c4;
                }

                .card-stub.green {
                    background: #c8e6c9;
                }

                .card-title {
                    font-weight: bold;
                    margin-bottom: 4px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .card-tags {
                    font-size: 11px;
                    color: #555;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .card-date {
                    font-size: 11px;
                    text-align: right;
                    margin-top: 4px;
                }

                .details {
                    flex: 1;
                    padding: 16px;
                    box-sizing: border-box;
                    background: #e8f5e9;
                }

                .details-card {
                    background: #a5d6a7;
                    border-radius: 4px;
                    padding: 12px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                }

                .details-field {
                    margin-bottom: 10px;
                }

                .details-field label {
                    display: block;
                    font-size: 12px;
                    margin-bottom: 4px;
                }

                .details-field input[type="text"],
                .details-field input[type="date"],
                .details-field textarea {
                    width: 100%;
                    box-sizing: border-box;
                    border-radius: 4px;
                    border: 1px solid #ccc;
                    padding: 4px;
                    font-size: 13px;
                }

                .details-field textarea {
                    min-height: 80px;
                    resize: vertical;
                }

                .status-group label {
                    margin-right: 8px;
                    font-size: 12px;
                }

                .context-menu {
                    position: fixed;
                    background: #ffffff;
                    border: 1px solid #ccc;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    display: none;
                    z-index: 1000;
                    font-size: 13px;
                    min-width: 160px;
                }

                .context-menu-item {
                    padding: 6px 12px;
                    cursor: pointer;
                }

                .context-menu-item:hover {
                    background: #eeeeee;
                }
            </style>
        </head>
        <body>
            <!-- ВЕРХНЯЯ ПАНЕЛЬ -->
            <div class="topbar">
                <div class="topbar-left">
                    <div class="profile-avatar"></div>
                    <div class="topbar-menu">
                        <span class="topbar-menu-item">Профиль</span>
                        <span class="topbar-menu-item">Уведомления</span>
                        <span class="topbar-menu-item">Поиск</span>
                        <span class="topbar-menu-item">Доски</span>
                        <span class="topbar-menu-item">Архив</span>
                    </div>
                </div>
            </div>

            <!-- ОСНОВНОЕ ПОЛЕ С ДОСКОЙ -->
            <div class="app">
                <div class="board">
                    <div class="board-header">Моя канбан-доска</div>
                    <div class="columns">
                        ${buildColumn("Сделать", Status.TODO, tasks)}
                        ${buildColumn("В работе", Status.IN_PROGRESS, tasks)}
                        ${buildColumn("Показать заказчику", Status.REVIEW, tasks)}
                        ${buildColumn("Готово", Status.DONE, tasks)}
                    </div>
                </div>

                <div class="details">
                    <div class="details-card">
                        <div class="details-field">
                            <label>Статус:</label>
                            <div class="status-group">
                                <label><input type="radio" name="status" value="TODO"> Сделать</label>
                                <label><input type="radio" name="status" value="IN_PROGRESS"> В работе</label>
                                <label><input type="radio" name="status" value="REVIEW"> Показать заказчику</label>
                                <label><input type="radio" name="status" value="DONE"> Готово</label>
                            </div>
                        </div>

                        <div class="details-field">
                            <label>Тема:</label>
                            <input type="text" id="detail-title" placeholder="Выбери карточку слева">
                        </div>

                        <div class="details-field">
                            <label>Описание задачи:</label>
                            <textarea id="detail-body"></textarea>
                        </div>

                        <div class="details-field">
                            <label>Срок сдачи:</label>
                            <input type="date" id="detail-date">
                        </div>

                        <div class="details-field">
                            <label>Участники:</label>
                            <input type="text" id="detail-participants">
                        </div>

                        <div class="details-field">
                            <label>Хэштеги:</label>
                            <input type="text" id="detail-tags">
                        </div>
                    </div>
                </div>
            </div>

            <!-- Контекстное меню для карточек -->
            <div id="card-menu" class="context-menu">
                <div class="context-menu-item" data-action="add">Добавить заметку</div>
                <div class="context-menu-item" data-action="delete">Удалить заметку</div>
            </div>

            <script>
                document.addEventListener("DOMContentLoaded", function () {
                    var stubs = document.querySelectorAll(".card-stub");
                    var menu = document.getElementById("card-menu");
                    var currentCard = null;

                    function setStatusRadio(status) {
                        var radios = document.querySelectorAll('input[name="status"]');
                        radios.forEach(function (r) {
                            r.checked = (r.value === status);
                        });
                    }

                    function fillDetailsFromCard(card) {
                        document.getElementById("detail-title").value = card.dataset.title || "";
                        document.getElementById("detail-body").value = card.dataset.body || "";
                        document.getElementById("detail-date").value = card.dataset.due || "";
                        document.getElementById("detail-participants").value = card.dataset.participants || "";
                        document.getElementById("detail-tags").value = card.dataset.tags || "";
                        setStatusRadio(card.dataset.status || "TODO");
                    }

                    function onStubClick() {
                        fillDetailsFromCard(this);
                    }

                    stubs.forEach(function (stub) {
                        stub.addEventListener("click", onStubClick);
                    });

                    document.addEventListener("contextmenu", function (e) {
                        var card = e.target.closest(".card-stub");
                        if (card) {
                            e.preventDefault();
                            currentCard = card;
                            menu.style.display = "block";
                            menu.style.left = e.pageX + "px";
                            menu.style.top = e.pageY + "px";
                        } else {
                            menu.style.display = "none";
                        }
                    });

                    document.addEventListener("click", function (e) {
                        if (!e.target.closest("#card-menu")) {
                            menu.style.display = "none";
                        }
                    });

                    menu.addEventListener("click", function (e) {
                        var item = e.target.closest(".context-menu-item");
                        if (!item || !currentCard) {
                            return;
                        }
                        var action = item.getAttribute("data-action");

                        if (action === "delete") {
                            if (currentCard.parentNode) {
                                currentCard.parentNode.removeChild(currentCard);
                            }
                            currentCard = null;
                        }

                        if (action === "add") {
                            var parent = currentCard.parentNode;
                            var newCard = document.createElement("div");
                            newCard.className = currentCard.className;

                            newCard.setAttribute("data-title", "Новая задача");
                            newCard.setAttribute("data-body", "Описание новой задачи");
                            newCard.setAttribute("data-status", currentCard.getAttribute("data-status") || "TODO");
                            newCard.setAttribute("data-due", new Date().toISOString().slice(0, 10));
                            newCard.setAttribute("data-participants", "");
                            newCard.setAttribute("data-tags", "#новое");

                            newCard.innerHTML =
                                '<div class="card-title">Новая задача</div>' +
                                '<div class="card-tags">#новое</div>' +
                                '<div class="card-date">' + newCard.getAttribute("data-due") + '</div>';

                            newCard.addEventListener("click", onStubClick);

                            if (currentCard.nextSibling) {
                                parent.insertBefore(newCard, currentCard.nextSibling);
                            } else {
                                parent.appendChild(newCard);
                            }
                        }

                        menu.style.display = "none";
                    });
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

/* ----- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ДЛЯ КОЛОНОК И КАРТОЧЕК ----- */

private fun buildColumn(
    title: String,
    status: Status,
    tasks: List<Task>
): String {
    val cardsHtml = tasks
        .filter { it.status == status }
        .joinToString("\n") { buildCardStub(it) }

    return """
        <div class="column">
            <div class="column-title">$title</div>
            <div class="column-body">
                $cardsHtml
            </div>
        </div>
    """.trimIndent()
}

private fun buildCardStub(task: Task): String {
    val colorClass = if (task.status == Status.TODO || task.status == Status.DONE) "yellow" else "green"

    return """
        <div class="card-stub $colorClass"
             data-id="${task.id}"
             data-title="${task.title}"
             data-body="${task.body}"
             data-status="${task.status.name}"
             data-due="${task.dueDate}"
             data-participants="${task.participants}"
             data-tags="${task.hashtags}">
            <div class="card-title">${task.title}</div>
            <div class="card-tags">${task.hashtags}</div>
            <div class="card-date">${task.dueDate}</div>
        </div>
    """.trimIndent()
}
