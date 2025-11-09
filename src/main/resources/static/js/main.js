'use strict';

// Simplified client-side script for a one-on-one chat. There is no username
// form; the page connects to the WebSocket as soon as it loads. Messages are
// sent to the server and replies are received on a per-session queue.

var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
// Fixed sender name for the user. The bot always identifies itself as "BOT".
var username = 'You';

// Colour palette used for avatars. Each sender name hashes to a colour.
var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

// Append a chat message to the message area. Each message shows an avatar
// (first letter of the sender's name), the sender name and the message text.
function displayMessage(sender, messageText) {
    var messageElement = document.createElement('li');
    messageElement.classList.add('chat-message');

    var avatarElement = document.createElement('i');
    var avatarText = document.createTextNode(sender[0]);
    avatarElement.appendChild(avatarText);
    avatarElement.style['background-color'] = getAvatarColor(sender);
    messageElement.appendChild(avatarElement);

    var usernameElement = document.createElement('span');
    var usernameText = document.createTextNode(sender);
    usernameElement.appendChild(usernameText);
    messageElement.appendChild(usernameElement);

    var textElement = document.createElement('p');
    var messageTextNode = document.createTextNode(messageText);
    textElement.appendChild(messageTextNode);
    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

// Establish a STOMP connection over SockJS and subscribe to the per-user queue.
function connect() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function () {
        // Subscribe to personal queue for receiving bot replies
        stompClient.subscribe('/user/queue/replies', onMessageReceived);
        // Trigger the initial greeting from the server
        stompClient.send('/app/chat.init', {}, '');
        // Hide the connecting message once connected
        connectingElement.classList.add('hidden');
    }, onError);
}

function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}

// Send a message to the server. Also display it locally so the chat area does
// not appear empty while waiting for a reply.
function sendMessage(event) {
    var messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        displayMessage(username, messageContent);
        var chatMessage = {
            text: messageContent
        };
        stompClient.send('/app/chat.user', {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

// Handle incoming bot messages from the personal reply queue.
function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);
    displayMessage(message.sender, message.content);
}

// Compute a background colour for the avatar based on the sender's name.
function getAvatarColor(sender) {
    var hash = 0;
    for (var i = 0; i < sender.length; i++) {
        hash = 31 * hash + sender.charCodeAt(i);
    }
    var index = Math.abs(hash % colors.length);
    return colors[index];
}

// Attach form event handler
messageForm.addEventListener('submit', sendMessage, true);

// Automatically connect when the script loads
connect();
