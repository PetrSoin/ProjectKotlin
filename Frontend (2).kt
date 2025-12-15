package frontend

import backend.Status
import backend.Task

/**
 * Генерация полной HTML-страницы (фронтенд: верстка, стили, JS).
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

                /* Верхняя панель */
                .topbar {
                    height: 48px;
                    background: #f5f5f5;
                    border-bottom: 1px solid #ddd;
                    display: flex;
                    align-items: center;
                    padding: 0 16px;
                    box-sizing: border-box;
                    position: relative;
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
                    height: calc(100vh - 48px);
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

                /* Кнопка "+" для добавления карточки */
                .column-add {
                    width: 90%;
                    margin: 4px auto 0;
                    height: 28px;
                    border-radius: 4px;
                    border: 1px dashed #aaa;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #777;
                    font-size: 18px;
                    cursor: pointer;
                    opacity: 0.6;
                }

                .column-add:hover {
                    opacity: 1;
                    background: #f5f5f5;
                }

                /* Панель подробной карточки */
                .details {
                    flex: 1;
                    padding: 16px;
                    box-sizing: border-box;
                    background: #e8f5e9;
                    display: none;
                }

                .details.visible {
                    display: block;
                }

                .details-card {
                    background: #a5d6a7;
                    border-radius: 4px;
                    padding: 12px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                }

                .details-card-yellow {
                    background: #fff9c4;
                }

                .details-card-green {
                    background: #c8e6c9;
                }

                .details-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 8px;
                }

                .details-title-text {
                    font-weight: bold;
                    font-size: 14px;
                }

                .details-close-btn {
                    border: none;
                    background: transparent;
                    font-size: 18px;
                    line-height: 1;
                    cursor: pointer;
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

                /* Контекстное меню (удаление) */
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

                /* Выпадающий список досок */
                .boards-dropdown {
                    position: absolute;
                    top: 48px;
                    left: 140px;
                    background: #ffffff;
                    border: 1px solid #ccc;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    min-width: 220px;
                    display: none;
                    z-index: 1000;
                    padding: 4px 0;
                }

                .boards-dropdown-item,
                .boards-dropdown-add {
                    padding: 6px 12px;
                    cursor: pointer;
                    font-size: 13px;
                }

                .boards-dropdown-item:hover,
                .boards-dropdown-add:hover {
                    background: #f0f0f0;
                }

                .boards-dropdown-separator {
                    height: 1px;
                    background: #ddd;
                    margin: 4px 0;
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
                        <span class="topbar-menu-item" id="boards-toggle">Доски</span>
                        <span class="topbar-menu-item">Архив</span>
                    </div>
                </div>

                <!-- Выпадающий список досок -->
                <div id="boards-dropdown" class="boards-dropdown">
                    <div class="boards-dropdown-item" data-board-key="default">Моя канбан-доска</div>
                    <div class="boards-dropdown-separator"></div>
                    <div class="boards-dropdown-add" id="boards-add">Создать новую доску</div>
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

                <!-- ПАНЕЛЬ ПОДРОБНОЙ КАРТОЧКИ (СКРЫТА ПО УМОЛЧАНИЮ) -->
                <div class="details">
                    <div class="details-card">
                        <div class="details-header">
                            <span class="details-title-text">Карточка задачи</span>
                            <button id="details-close" class="details-close-btn" title="Закрыть">×</button>
                        </div>

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

            <!-- Контекстное меню (удаление) -->
            <div id="card-menu" class="context-menu">
                <div class="context-menu-item" data-action="delete">Удалить заметку</div>
            </div>

            <script>
                document.addEventListener("DOMContentLoaded", function () {
                    var menu = document.getElementById("card-menu");
                    var detailsPanel = document.querySelector(".details");
                    var detailsCard = document.querySelector(".details-card");
                    var closeBtn = document.getElementById("details-close");
                    var boardsToggle = document.getElementById("boards-toggle");
                    var boardsDropdown = document.getElementById("boards-dropdown");
                    var boardsAdd = document.getElementById("boards-add");
                    var boardHeader = document.querySelector(".board-header");
                    var statusRadios = document.querySelectorAll('input[name="status"]');

                    var currentDetailsCard = null;

                    // список досок
                    var boards = [
                        { key: "default", name: "Моя канбан-доска" }
                    ];
                    var currentBoardKey = "default";

                    // снимок стартового состояния колонок для default-доски
                    var defaultColumnsContent = {};
                    document.querySelectorAll(".column-body").forEach(function (body) {
                        var status = body.getAttribute("data-status");
                        if (status) {
                            defaultColumnsContent[status] = body.innerHTML;
                        }
                    });

                    // --- SYNC /edit: отправка всех карточек текущей доски ---
                    var syncTimer = null;
                    var syncAbortController = null;
                    var lastSyncHash = "";

                    function isIntString(s) {
                        return /^-?\d+$/.test(String(s || ""));
                    }

                    function ensureCardId(card) {
                        var id = card.dataset.id;
                        if (id && id !== "") return id;

                        // генерим следующий числовой id (max+1) по текущему DOM
                        var max = 0;
                        document.querySelectorAll(".card-stub[data-id]").forEach(function (el) {
                            var v = el.dataset.id;
                            if (isIntString(v)) {
                                var n = parseInt(v, 10);
                                if (n > max) max = n;
                            }
                        });

                        id = String(max + 1);
                        card.dataset.id = id;
                        return id;
                    }

                    function collectAllCardsPayload(reason) {
                        var board = boards.find(function (b) { return b.key === currentBoardKey; });

                        var payload = {
                            boardKey: currentBoardKey,
                            boardName: board ? board.name : "",
                            reason: reason || "unknown",
                            updatedAt: new Date().toISOString(),
                            tasks: []
                        };

                        document.querySelectorAll(".column-body").forEach(function (body) {
                            var status = body.getAttribute("data-status") || "";
                            var cards = body.querySelectorAll(".card-stub");

                            cards.forEach(function (card, idx) {
                                var id = ensureCardId(card);

                                payload.tasks.push({
                                    id: isIntString(id) ? parseInt(id, 10) : id,
                                    title: card.dataset.title || "",
                                    body: card.dataset.body || "",
                                    status: card.dataset.status || status || "TODO",
                                    dueDate: card.dataset.due || "",
                                    participants: card.dataset.participants || "",
                                    hashtags: card.dataset.tags || "",
                                    order: idx
                                });
                            });
                        });

                        return payload;
                    }

                    function hashString(str) {
                        var h = 0;
                        for (var i = 0; i < str.length; i++) {
                            h = ((h << 5) - h) + str.charCodeAt(i);
                            h |= 0;
                        }
                        return String(h);
                    }

                    function scheduleSync(reason) {
                        clearTimeout(syncTimer);

                        syncTimer = setTimeout(function () {
                            var payload = collectAllCardsPayload(reason);

                            // хешируем только "суть", чтобы не слать одно и то же из-за updatedAt/reason
                            var basis = JSON.stringify({ boardKey: payload.boardKey, tasks: payload.tasks });
                            var h = hashString(basis);
                            if (h === lastSyncHash) return;
                            lastSyncHash = h;

                            try {
                                if (syncAbortController) syncAbortController.abort();
                            } catch (e) {}

                            syncAbortController = ("AbortController" in window) ? new AbortController() : null;

                            fetch("/edit", {
                                method: "POST",
                                headers: { "Content-Type": "application/json" },
                                body: JSON.stringify(payload),
                                signal: syncAbortController ? syncAbortController.signal : undefined
                            }).catch(function (err) {
                                console.error("Sync /edit error:", err);
                            });

                        }, 300);
                    }

                    // страховка: если DOM меняется не через наши хэндлеры (например drag&drop)
                    function initSyncObserver() {
                        var boardEl = document.querySelector(".board");
                        if (!boardEl || boardEl.dataset.syncObserver === "1") return;
                        boardEl.dataset.syncObserver = "1";

                        var mo = new MutationObserver(function () {
                            scheduleSync("dom_mutation");
                        });

                        mo.observe(boardEl, {
                            subtree: true,
                            childList: true,
                            attributes: true,
                            attributeFilter: [
                                "data-title","data-body","data-status","data-due","data-participants","data-tags","data-id","class"
                            ]
                        });
                    }

                    // --- вспомогательные функции ---

                    function statusToColorClass(status) {
                        if (status === "TODO" || status === "DONE") {
                            return "yellow";
                        }
                        return "green";
                    }

                    function setStatusRadio(status) {
                        statusRadios.forEach(function (r) {
                            r.checked = (r.value === status);
                        });
                    }

                    function openDetails() {
                        if (detailsPanel) {
                            detailsPanel.classList.add("visible");
                        }
                    }

                    function closeDetails() {
                        if (detailsPanel) {
                            detailsPanel.classList.remove("visible");
                        }
                    }

                    function setDetailsColorFromCard(card) {
                        if (!detailsCard) return;
                        detailsCard.classList.remove("details-card-yellow", "details-card-green");
                        if (card.classList.contains("yellow")) {
                            detailsCard.classList.add("details-card-yellow");
                        } else if (card.classList.contains("green")) {
                            detailsCard.classList.add("details-card-green");
                        }
                    }

                    function fillDetailsFromCard(card) {
                        document.getElementById("detail-title").value = card.dataset.title || "";
                        document.getElementById("detail-body").value = card.dataset.body || "";
                        document.getElementById("detail-date").value = card.dataset.due || "";
                        document.getElementById("detail-participants").value = card.dataset.participants || "";
                        document.getElementById("detail-tags").value = card.dataset.tags || "";
                        setStatusRadio(card.dataset.status || "TODO");
                        setDetailsColorFromCard(card);
                    }

                    function onStubClick() {
                        currentDetailsCard = this;
                        // если вдруг карточка без id (новая) — гарантируем
                        ensureCardId(currentDetailsCard);
                        openDetails();
                        fillDetailsFromCard(this);
                    }

                    // синхронизация при изменении полей справа (и обновление видимых данных карточки)
                    function updateCurrentCardFromDetails() {
                        if (!currentDetailsCard) return;

                        var title = document.getElementById("detail-title").value || "";
                        var body = document.getElementById("detail-body").value || "";
                        var due = document.getElementById("detail-date").value || "";
                        var participants = document.getElementById("detail-participants").value || "";
                        var tags = document.getElementById("detail-tags").value || "";

                        currentDetailsCard.dataset.title = title;
                        currentDetailsCard.dataset.body = body;
                        currentDetailsCard.dataset.due = due;
                        currentDetailsCard.dataset.participants = participants;
                        currentDetailsCard.dataset.tags = tags;

                        var t = currentDetailsCard.querySelector(".card-title");
                        if (t) t.textContent = title;

                        var tg = currentDetailsCard.querySelector(".card-tags");
                        if (tg) tg.textContent = tags;

                        var d = currentDetailsCard.querySelector(".card-date");
                        if (d) d.textContent = due;

                        scheduleSync("edit_fields");
                    }

                    ["detail-title", "detail-body", "detail-date", "detail-participants", "detail-tags"].forEach(function (id) {
                        var el = document.getElementById(id);
                        if (!el) return;
                        el.addEventListener("input", updateCurrentCardFromDetails);
                        el.addEventListener("change", updateCurrentCardFromDetails);
                    });

                    // плюсики в колонках
                    function createAddButtonsForAllColumns() {
                        document.querySelectorAll(".column-body").forEach(function (body) {
                            var status = body.getAttribute("data-status");
                            if (!status) return;
                            if (body.querySelector(".column-add")) return;

                            var btn = document.createElement("div");
                            btn.className = "column-add";
                            btn.textContent = "+";
                            btn.setAttribute("data-status", status);
                            body.insertBefore(btn, body.firstChild);
                        });
                    }

                    function attachAddButtons() {
                        document.querySelectorAll(".column-add").forEach(function (btn) {
                            if (btn.dataset.clickInit === "1") return;
                            btn.dataset.clickInit = "1";

                            btn.addEventListener("click", function () {
                                var status = this.getAttribute("data-status");
                                var body = this.parentNode;
                                if (!status || !body) return;

                                var card = document.createElement("div");
                                var colorClass = statusToColorClass(status);
                                card.className = "card-stub " + colorClass;

                                var today = new Date().toISOString().slice(0, 10);

                                card.dataset.title = "Новая задача";
                                card.dataset.body = "Описание новой задачи";
                                card.dataset.status = status;
                                card.dataset.due = today;
                                card.dataset.participants = "";
                                card.dataset.tags = "#новое";

                                // гарантируем data-id, чтобы payload был полный
                                ensureCardId(card);

                                card.innerHTML =
                                    '<div class="card-title">Новая задача</div>' +
                                    '<div class="card-tags">#новое</div>' +
                                    '<div class="card-date">' + today + '</div>';

                                card.addEventListener("click", onStubClick);

                                body.appendChild(card);

                                scheduleSync("add_card");
                            });
                        });
                    }

                    function attachCardHandlers() {
                        document.querySelectorAll(".card-stub").forEach(function (stub) {
                            if (stub.dataset.clickInit === "1") return;
                            stub.dataset.clickInit = "1";
                            stub.addEventListener("click", onStubClick);
                        });
                    }

                    function refreshBoardInteractions() {
                        createAddButtonsForAllColumns();
                        attachAddButtons();
                        attachCardHandlers();
                    }

                    // начальная инициализация
                    refreshBoardInteractions();
                    initSyncObserver();
                    scheduleSync("init");

                    // кнопка закрытия панели
                    if (closeBtn) {
                        closeBtn.addEventListener("click", function () {
                            currentDetailsCard = null;
                            closeDetails();
                        });
                    }

                    // --- контекстное меню (удаление) ---
                    var currentCard = null;

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
                            if (currentDetailsCard === currentCard) {
                                currentDetailsCard = null;
                                closeDetails();
                            }
                            currentCard = null;

                            scheduleSync("delete_card");
                        }

                        menu.style.display = "none";
                    });

                    // --- изменение статуса через радио-кнопки ---
                    statusRadios.forEach(function (radio) {
                        radio.addEventListener("change", function () {
                            if (!currentDetailsCard) return;

                            var newStatus = this.value;
                            currentDetailsCard.dataset.status = newStatus;

                            // смена цвета карточки
                            currentDetailsCard.classList.remove("yellow", "green");
                            currentDetailsCard.classList.add(statusToColorClass(newStatus));

                            // перемещение карточки в нужную колонку
                            var targetBody = document.querySelector('.column-body[data-status="' + newStatus + '"]');
                            if (targetBody && currentDetailsCard.parentNode !== targetBody) {
                                targetBody.appendChild(currentDetailsCard);
                            }

                            // цвет панели
                            setDetailsColorFromCard(currentDetailsCard);

                            scheduleSync("status_change");
                        });
                    });

                    // --- работа со списком досок ---
                    function switchBoard(boardKey) {
                        currentBoardKey = boardKey;
                        currentDetailsCard = null;
                        closeDetails();

                        var board = boards.find(function (b) { return b.key === boardKey; });
                        if (board && boardHeader) {
                            boardHeader.textContent = board.name;
                        }

                        document.querySelectorAll(".column-body").forEach(function (body) {
                            var status = body.getAttribute("data-status");
                            if (!status) return;

                            if (boardKey === "default") {
                                body.innerHTML = defaultColumnsContent[status] || "";
                            } else {
                                body.innerHTML = "";
                            }
                        });

                        // после смены доски заново навешиваем плюсики и клики
                        refreshBoardInteractions();

                        // отправим состояние новой активной доски
                        scheduleSync("switch_board");
                    }

                    if (boardsToggle && boardsDropdown) {
                        boardsToggle.addEventListener("click", function (e) {
                            e.stopPropagation();
                            boardsDropdown.style.display =
                                boardsDropdown.style.display === "block" ? "none" : "block";
                        });
                    }

                    document.addEventListener("click", function (e) {
                        if (!e.target.closest("#boards-dropdown") &&
                            !e.target.closest("#boards-toggle")) {
                            boardsDropdown.style.display = "none";
                        }
                    });

                    if (boardsDropdown) {
                        boardsDropdown.addEventListener("click", function (e) {
                            var item = e.target.closest(".boards-dropdown-item");
                            if (item) {
                                var key = item.getAttribute("data-board-key");
                                if (key) {
                                    switchBoard(key);
                                    boardsDropdown.style.display = "none";
                                }
                            }
                        });
                    }

                    if (boardsAdd) {
                        boardsAdd.addEventListener("click", function () {
                            var name = prompt("Введите название новой доски", "Новая доска");
                            if (!name) return;

                            var key = "board-" + Date.now();
                            boards.push({ key: key, name: name });

                            var newItem = document.createElement("div");
                            newItem.className = "boards-dropdown-item";
                            newItem.textContent = name;
                            newItem.setAttribute("data-board-key", key);

                            boardsDropdown.insertBefore(newItem, boardsAdd.parentNode ? boardsAdd : null);

                            switchBoard(key);
                            boardsDropdown.style.display = "none";
                        });
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

/* ----- KOTLIN-ХЕЛПЕРЫ ДЛЯ КОЛОНОК И КАРТОЧЕК ----- */

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
            <div class="column-body" data-status="${status.name}">
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
