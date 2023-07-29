document.addEventListener('DOMContentLoaded', function () {

    var usernameElement = document.getElementById('username');
    var username = usernameElement.dataset.username;

    document.getElementById("button-send").addEventListener("click", function () {
        send();
    });

    var websocket = new WebSocket("ws://localhost:8080/ws/chat");

    websocket.onmessage = onMessage;
    websocket.onopen = onOpen;
    websocket.onerror = onError;

    function send() {
        var msg = document.getElementById("msg");
        if (msg.value.trim() !== '') {
            console.log(username + ":" + msg.value);
            websocket.send(username + ":" + msg.value);
            msg.value = '';
        }
    }

    // Removed onClose function

    //채팅창에 들어왔을 때
    function onOpen(evt) {
        var str = username + ": 님이 입장하셨습니다.";
        websocket.send(str);
    }

    function onMessage(msg) {
        var data = msg.data;
        var sessionId = null;
        var message = null;
        var arr = data.split(":");

        var cur_session = username;

        sessionId = arr[0];
        message = arr[1];

        if (sessionId == cur_session) {
            var str = "<div class='col-6'>";
            str += "<div class='alert alert-secondary'>";
            str += "<b>" + sessionId + " : " + message + "</b>";
            str += "</div></div>";
            document.getElementById('msgArea').innerHTML += str;
        } else {
            var str = "<div class='col-6'>";
            str += "<div class='alert alert-warning'>";
            str += "<b>" + sessionId + " : " + message + "</b>";
            str += "</div></div>";
            document.getElementById('msgArea').innerHTML += str;
        }
    }

    function onError(error) {
        console.error("WebSocket error:", error);
        // Handle any WebSocket connection errors here
    }
});