let stompClient = null;
let currentRoomId = null;

const chatLineElementId = "chatLine";
const roomIdElementId = "roomId";
const messageElementId = "message";
const SPECIAL_ROOM_ID = "1408";

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    checkSpecialRoom();
});

const setConnected = (connected) => {
    const connectBtn = document.getElementById("connect");
    const disconnectBtn = document.getElementById("disconnect");

    connectBtn.disabled = connected;
    disconnectBtn.disabled = !connected;
    const chatLine = document.getElementById(chatLineElementId);
    chatLine.hidden = !connected;
}

const checkSpecialRoom = () => {
    const roomId = document.getElementById(roomIdElementId).value;
    const mainContent = document.getElementById("main-content");

    if (roomId === SPECIAL_ROOM_ID) {
        mainContent.classList.add('room-1408');
    } else {
        mainContent.classList.remove('room-1408');
    }
}

const connect = () => {
    const roomId = document.getElementById(roomIdElementId).value;
    if (!roomId) {
        alert("Please enter a room ID");
        return;
    }

    currentRoomId = roomId;

    stompClient = Stomp.over(new SockJS('/gs-guide-websocket'));

    // Настраиваем reconnect если соединение потеряно
    stompClient.reconnect_delay = 5000;

    stompClient.connect({}, (frame) => {
        setConnected(true);
        const userName = frame.headers["user-name"];
        console.log(`Connected to roomId: ${roomId} as user: ${userName}`);

        const topicName = `/topic/response.${roomId}`;
        const topicNameUser = `/user/${userName}${topicName}`;

        // Подписываемся на общий топик комнаты
        stompClient.subscribe(topicName, (message) => {
            const messageBody = JSON.parse(message.body);
            showMessage(messageBody.messageStr, false);
        });

        // Подписываемся на персональный топик (для истории сообщений)
        stompClient.subscribe(topicNameUser, (message) => {
            const messageBody = JSON.parse(message.body);
            showMessage(messageBody.messageStr, true);
        });

        // Очищаем чат при подключении к новой комнате
        clearChat();

        // Добавляем приветственное сообщение для комнаты 1408
        if (roomId === SPECIAL_ROOM_ID) {
            showMessage("📢 Вы вошли в комнату 1408. Сюда транслируются все сообщения из других комнат.", false);
        }

        // Обновляем UI в зависимости от комнаты
        checkSpecialRoom();
    }, (error) => {
        console.error("Connection error: ", error);
        alert("Failed to connect to server");
    });
}

const disconnect = () => {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    currentRoomId = null;

    clearChat();
    checkSpecialRoom(); // Возвращаем обычный UI

    console.log("Disconnected");
}

const clearChat = () => {
    const chatLine = document.getElementById(chatLineElementId);
    chatLine.innerHTML = '';
}

const sendMsg = () => {
    // Дополнительная проверка для комнаты 1408
    if (currentRoomId === SPECIAL_ROOM_ID) {
        showMessage("❌ Отправка сообщений запрещена в комнате 1408", false);
        return;
    }

    const roomId = document.getElementById(roomIdElementId).value;
    const message = document.getElementById(messageElementId).value;

    if (!message.trim()) {
        alert("Please enter a message");
        return;
    }

    // Отправляем сообщение
    stompClient.send(`/app/message.${roomId}`, {}, JSON.stringify({'messageStr': message}));

    // Очищаем поле ввода
    document.getElementById(messageElementId).value = '';
}

const showMessage = (message, isHistorical = false) => {
    const chatLine = document.getElementById(chatLineElementId);
    let newRow = chatLine.insertRow(-1);

    // Добавляем класс для стилизации
    newRow.className = 'chat-message';

    // Если это сообщение для комнаты 1408 (с префиксом), добавляем специальный класс
    if (currentRoomId === SPECIAL_ROOM_ID && message.includes('[Room')) {
        newRow.classList.add('room-1408-message');
    }

    let newCell = newRow.insertCell(0);

    if (isHistorical) {
        message = "📜 " + message;
    }

    let newText = document.createTextNode(message);
    newCell.appendChild(newText);

    // Автоскролл к новому сообщению
    const chatTable = document.getElementById("conversation");
    chatTable.scrollTop = chatTable.scrollHeight;
}

// Добавляем обработчик клавиши Enter для отправки сообщения
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && document.getElementById('send').disabled === false) {
        const messageInput = document.getElementById(messageElementId);
        if (document.activeElement === messageInput) {
            sendMsg();
        }
    }
});

// Проверяем комнату при изменении поля ввода
document.getElementById(roomIdElementId).addEventListener('input', checkSpecialRoom);
document.getElementById(roomIdElementId).addEventListener('change', checkSpecialRoom);

// Добавляем валидацию перед отправкой
window.onload = function() {
    checkSpecialRoom();
};