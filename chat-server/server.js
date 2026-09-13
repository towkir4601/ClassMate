const { WebSocketServer } = require('ws');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs');

const DB_FILE = './database.json';
let db = {
    users: {}, // uid -> { id, name, avatarUrl, isOnline, lastSeen }
    rooms: {
        "group_main": { id: "group_main", type: "group", name: "Class Group", lastMessage: "", lastTimestamp: 0 }
    },
    messages: [] // array of message objects
};

// Load database from file
if (fs.existsSync(DB_FILE)) {
    try {
        const data = fs.readFileSync(DB_FILE, 'utf8');
        db = JSON.parse(data);
    } catch (e) {
        console.error("Error reading database", e);
    }
}

// Ensure main group exists
if (!db.rooms["group_main"]) {
    db.rooms["group_main"] = { id: "group_main", type: "group", name: "Class Group", lastMessage: "", lastTimestamp: 0 };
}

function saveDb() {
    fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
}

const wss = new WebSocketServer({ port: process.env.PORT || 8080 });
console.log("WebSocket server started on port", process.env.PORT || 8080);

const clients = new Map(); // ws -> { userId, currentRoomId }

function broadcast(msgObj, filterFn = null) {
    const data = JSON.stringify(msgObj);
    wss.clients.forEach((client) => {
        if (client.readyState === 1) { // OPEN
            if (!filterFn || filterFn(clients.get(client))) {
                client.send(data);
            }
        }
    });
}

function broadcastOnlineUsers() {
    const onlineUserIds = Array.from(clients.values()).map(c => c.userId).filter(Boolean);
    broadcast({ type: "online_users", userIds: [...new Set(onlineUserIds)] });
}

wss.on('connection', (ws) => {
    clients.set(ws, { userId: null, currentRoomId: null });

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            const clientInfo = clients.get(ws);

            if (data.type === 'auth') {
                clientInfo.userId = data.userId;
                db.users[data.userId] = {
                    id: data.userId,
                    name: data.userName || "User",
                    avatarUrl: data.avatarUrl || "",
                    isOnline: true,
                    lastSeen: Date.now()
                };
                saveDb();
                
                // Send rooms
                ws.send(JSON.stringify({ type: "rooms", rooms: Object.values(db.rooms) }));
                
                broadcastOnlineUsers();
            } 
            else if (data.type === 'get_rooms') {
                ws.send(JSON.stringify({ type: "rooms", rooms: Object.values(db.rooms) }));
            }
            else if (data.type === 'get_users') {
                ws.send(JSON.stringify({ type: "users", users: Object.values(db.users) }));
            }
            else if (data.type === 'set_room') {
                clientInfo.currentRoomId = data.roomId;
            }
            else if (data.type === 'get_history') {
                const roomMsgs = db.messages.filter(m => m.roomId === data.roomId);
                ws.send(JSON.stringify({ type: "history", roomId: data.roomId, messages: roomMsgs }));
            }
            else if (data.type === 'send_message' || data.type === 'send_image') {
                const sender = db.users[clientInfo.userId];
                const msg = {
                    id: uuidv4(),
                    roomId: data.roomId,
                    senderId: clientInfo.userId,
                    senderName: sender ? sender.name : "User",
                    senderAvatarUrl: sender ? sender.avatarUrl : "",
                    text: data.text || "",
                    imageUrl: data.imageUrl || "",
                    timestamp: Date.now(),
                    type: data.type === 'send_image' ? "image" : "text",
                    isEdited: false,
                    isDeleted: false,
                    reactions: {},
                    seenBy: [clientInfo.userId]
                };
                db.messages.push(msg);

                // Update room last message
                if (db.rooms[data.roomId]) {
                    db.rooms[data.roomId].lastMessage = data.type === 'send_image' ? "Sent an image" : msg.text;
                    db.rooms[data.roomId].lastTimestamp = msg.timestamp;
                }
                saveDb();

                broadcast({ type: "new_message", ...msg });
            }
            else if (data.type === 'typing') {
                broadcast({ type: "typing", roomId: data.roomId, userId: clientInfo.userId });
            }
            else if (data.type === 'create_dm') {
                // Create DM room if doesn't exist
                const ids = [clientInfo.userId, data.targetUserId].sort();
                const roomId = `dm_${ids[0]}_${ids[1]}`;
                if (!db.rooms[roomId]) {
                    db.rooms[roomId] = {
                        id: roomId,
                        type: "direct",
                        member1Id: ids[0],
                        member2Id: ids[1],
                        lastMessage: "",
                        lastTimestamp: Date.now()
                    };
                    saveDb();
                }
                // Send back dm_created
                ws.send(JSON.stringify({ type: "dm_created", room: db.rooms[roomId] }));
            }
        } catch (e) {
            console.error("Invalid message format", e);
        }
    });

    ws.on('close', () => {
        const clientInfo = clients.get(ws);
        if (clientInfo && clientInfo.userId && db.users[clientInfo.userId]) {
            db.users[clientInfo.userId].isOnline = false;
            db.users[clientInfo.userId].lastSeen = Date.now();
            saveDb();
        }
        clients.delete(ws);
        broadcastOnlineUsers();
    });
});
