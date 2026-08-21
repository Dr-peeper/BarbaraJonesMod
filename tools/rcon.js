#!/usr/bin/env node
// Talks to the test server's RCON port.
//
//   node tools/rcon.js "/time set day" "/forge tps"
//   node tools/rcon.js --file commands.txt
//
// Every argument is one command; each result is printed under the command that
// produced it. Exits non-zero if the server cannot be reached or the password
// is wrong, so a script can gate on it.
//
// Written against the raw protocol rather than pulling an rcon package in:
// the whole thing is four integers and a null-terminated string, and this
// project has no npm dependency tree to add one to.
//
// Protocol, for whoever reads this next:
//   int32le length (of everything after this field)
//   int32le request id  - echoed back, and -1 means the login was rejected
//   int32le type        - 3 login, 2 command, 0 response
//   body                - ASCII, null terminated
//   int8    a second trailing null
'use strict';

const net = require('net');
const fs = require('fs');
const path = require('path');

const TYPE_LOGIN = 3;
const TYPE_COMMAND = 2;
const TYPE_RESPONSE = 0;

/** Reads host/port/password out of the server.properties we wrote. */
function serverConfig() {
    const p = path.join(__dirname, '..', 'run', 'server.properties');
    if (!fs.existsSync(p)) {
        throw new Error('run/server.properties missing - start the test server first');
    }
    const props = {};
    for (const line of fs.readFileSync(p, 'utf8').split(/\r?\n/)) {
        const m = line.match(/^([^#=]+)=(.*)$/);
        if (m) props[m[1].trim()] = m[2].trim();
    }
    if (props['enable-rcon'] !== 'true') {
        throw new Error('rcon is disabled in run/server.properties');
    }
    return {
        host: '127.0.0.1',
        port: parseInt(props['rcon.port'] || '25575', 10),
        password: props['rcon.password'] || '',
    };
}

function encode(id, type, body) {
    const payload = Buffer.from(body, 'ascii');
    const buf = Buffer.alloc(14 + payload.length);
    buf.writeInt32LE(10 + payload.length, 0);
    buf.writeInt32LE(id, 4);
    buf.writeInt32LE(type, 8);
    payload.copy(buf, 12);
    // Two trailing nulls: one ends the body, one ends the packet.
    buf.writeUInt8(0, 12 + payload.length);
    buf.writeUInt8(0, 13 + payload.length);
    return buf;
}

function run(commands) {
    const cfg = serverConfig();
    return new Promise((resolve, reject) => {
        const socket = net.createConnection(cfg.port, cfg.host);
        socket.setTimeout(20000);

        let pending = Buffer.alloc(0);
        const results = [];
        let stage = 'login';
        let next = 0;
        // A long command reply arrives split across packets with no length up
        // front, so after each real command we send a harmless empty one. Its
        // reply cannot overtake the packets queued before it, so seeing that
        // sentinel come back is how we know the previous reply finished.
        const SENTINEL = 1000;
        let collected = '';

        const send = () => {
            if (next >= commands.length) {
                socket.end();
                resolve(results);
                return;
            }
            collected = '';
            socket.write(encode(next, TYPE_COMMAND, commands[next]));
            socket.write(encode(SENTINEL, TYPE_RESPONSE, ''));
        };

        socket.on('connect', () => socket.write(encode(0, TYPE_LOGIN, cfg.password)));
        socket.on('timeout', () => { socket.destroy(); reject(new Error('rcon timed out')); });
        socket.on('error', reject);

        socket.on('data', chunk => {
            pending = Buffer.concat([pending, chunk]);
            while (pending.length >= 4) {
                const len = pending.readInt32LE(0);
                if (pending.length < len + 4) break;
                const id = pending.readInt32LE(4);
                const body = pending.toString('ascii', 12, 4 + len - 2);
                pending = pending.subarray(4 + len);

                if (stage === 'login') {
                    if (id === -1) { socket.destroy(); reject(new Error('rcon password rejected')); return; }
                    stage = 'commands';
                    send();
                } else if (id === SENTINEL) {
                    results.push({ command: commands[next], output: collected.trim() });
                    next++;
                    send();
                } else {
                    collected += body;
                }
            }
        });
    });
}

let commands = process.argv.slice(2);
if (commands[0] === '--file') {
    commands = fs.readFileSync(commands[1], 'utf8')
            .split(/\r?\n/).map(l => l.trim()).filter(l => l && !l.startsWith('#'));
}
if (!commands.length) {
    console.error('usage: node tools/rcon.js "<command>" ["<command>" ...]');
    console.error('       node tools/rcon.js --file <file>');
    process.exit(2);
}

run(commands).then(results => {
    for (const r of results) {
        console.log('> ' + r.command);
        console.log(r.output || '(no output)');
        console.log('');
    }
}).catch(err => {
    console.error('RCON FAILED: ' + err.message);
    process.exit(1);
});
