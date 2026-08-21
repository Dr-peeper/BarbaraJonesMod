#!/usr/bin/env node
// Refuses a jar in which any class escaped reobfuscation.
//
//   node tools/check_mapped.js <jar>
//
// WHY THIS EXISTS. The SRG-count gate in install_mod.ps1 counts references
// across the WHOLE jar and passes if the total is high enough. That catches a
// jar where reobfuscation never ran. It does not catch one where reobfuscation
// ran over only part of the classes: the total stays comfortably in the
// thousands, the gate is satisfied, and the classes that were skipped still
// call Minecraft by its official names. The mod loads and dies at the first one
// -
//
//   NoSuchMethodError: FoodProperties$Builder.nutrition(int)
//
// - which names a mod class and reads as a mod bug. It is a build bug, and it
// shipped once, from a build that was interrupted and then resumed over its own
// half-written output.
//
// WHY IT IS A CANARY AND NOT A GENERAL RULE. "Every net/minecraft member must
// carry an SRG name" sounds right and is wrong often enough to be useless as a
// gate. Enum constants keep their names (Direction.SOUTH, ChatFormatting.GRAY).
// So do members inherited from outside Minecraft - FriendlyByteBuf.writeInt is
// Netty's, not Mojang's. A checker that fails good builds gets disabled within
// the week, so this checks a short list of members that are definitely mapped
// and definitely used, all over the mod, by many different classes. If any
// class skipped reobfuscation, it is overwhelmingly likely to hold one of
// these - and a hit is unambiguous rather than a judgement call.
'use strict';

const fs = require('fs');
const { execFileSync } = require('child_process');
const path = require('path');
const os = require('os');

/**
 * Members that are certainly obfuscated at runtime and certainly called here.
 * Seeing one of these by its official name means the class holding it was never
 * processed.
 */
const CANARIES = new Set([
    'net/minecraft/world/food/FoodProperties$Builder.nutrition',
    'net/minecraft/world/food/FoodProperties$Builder.saturationMod',
    'net/minecraft/world/entity/Entity.getBbHeight',
    'net/minecraft/world/entity/Entity.getBbWidth',
    'net/minecraft/world/entity/LivingEntity.getHealth',
    'net/minecraft/world/entity/LivingEntity.getMaxHealth',
    'net/minecraft/world/level/Level.setBlock',
    'net/minecraft/world/level/Level.getBlockState',
    'net/minecraft/world/entity/Entity.getDeltaMovement',
    'net/minecraft/world/entity/Entity.setDeltaMovement',
    'net/minecraft/server/level/ServerLevel.sendParticles',
    'net/minecraft/world/entity/Mob.setTarget',
    'net/minecraft/world/entity/Mob.getTarget',
]);

const CONSTANT = { UTF8: 1, INT: 3, FLOAT: 4, LONG: 5, DOUBLE: 6, CLASS: 7, STRING: 8,
    FIELDREF: 9, METHODREF: 10, INTERFACE_METHODREF: 11, NAME_AND_TYPE: 12,
    METHOD_HANDLE: 15, METHOD_TYPE: 16, DYNAMIC: 17, INVOKE_DYNAMIC: 18,
    MODULE: 19, PACKAGE: 20 };

/** Every "owner.member" this class calls, read from the constant pool. */
function calls(buf) {
    if (buf.length < 10 || buf.readUInt32BE(0) !== 0xCAFEBABE) return [];
    const count = buf.readUInt16BE(8);
    const pool = new Array(count);
    let p = 10;

    for (let i = 1; i < count; i++) {
        const tag = buf.readUInt8(p++);
        switch (tag) {
            case CONSTANT.UTF8: {
                const len = buf.readUInt16BE(p); p += 2;
                pool[i] = { tag, text: buf.toString('utf8', p, p + len) };
                p += len;
                break;
            }
            case CONSTANT.CLASS: case CONSTANT.STRING: case CONSTANT.METHOD_TYPE:
            case CONSTANT.MODULE: case CONSTANT.PACKAGE:
                pool[i] = { tag, ref: buf.readUInt16BE(p) }; p += 2; break;
            case CONSTANT.FIELDREF: case CONSTANT.METHODREF:
            case CONSTANT.INTERFACE_METHODREF: case CONSTANT.NAME_AND_TYPE:
            case CONSTANT.DYNAMIC: case CONSTANT.INVOKE_DYNAMIC:
                pool[i] = { tag, a: buf.readUInt16BE(p), b: buf.readUInt16BE(p + 2) }; p += 4; break;
            case CONSTANT.INT: case CONSTANT.FLOAT: pool[i] = { tag }; p += 4; break;
            // Longs and doubles take two constant-pool slots. Getting this wrong
            // desynchronises every index after it and the parse silently rots.
            case CONSTANT.LONG: case CONSTANT.DOUBLE: pool[i] = { tag }; p += 8; i++; break;
            case CONSTANT.METHOD_HANDLE: pool[i] = { tag }; p += 3; break;
            default: return [];
        }
    }

    const utf8 = i => (pool[i] && pool[i].tag === CONSTANT.UTF8) ? pool[i].text : null;
    const out = [];
    for (let i = 1; i < count; i++) {
        const e = pool[i];
        if (!e || (e.tag !== CONSTANT.METHODREF && e.tag !== CONSTANT.INTERFACE_METHODREF)) {
            continue;
        }
        const cls = pool[e.a];
        const nat = pool[e.b];
        if (!cls || cls.tag !== CONSTANT.CLASS || !nat || nat.tag !== CONSTANT.NAME_AND_TYPE) {
            continue;
        }
        const owner = utf8(cls.ref);
        const member = utf8(nat.a);
        if (owner && member) out.push(owner + '.' + member);
    }
    return out;
}

const jar = process.argv[2];
if (!jar) { console.error('usage: node tools/check_mapped.js <jar>'); process.exit(2); }
if (!fs.existsSync(jar)) { console.error('No such jar: ' + jar); process.exit(2); }

// Unpacked with the JDK's own jar tool, so this needs nothing installed on a
// machine that is only set up to build the mod.
const jdkDir = path.join(__dirname, '..', '.tools', 'jdk17');
const jarExe = path.join(jdkDir, fs.readdirSync(jdkDir)[0], 'bin', 'jar.exe');
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'mapchk-'));
try {
    execFileSync(jarExe, ['-xf', path.resolve(jar), 'com'], { cwd: tmp });

    const bad = [];
    let checked = 0;
    (function walk(dir) {
        for (const name of fs.readdirSync(dir)) {
            const full = path.join(dir, name);
            if (fs.statSync(full).isDirectory()) { walk(full); continue; }
            if (!name.endsWith('.class')) continue;
            checked++;
            const hits = calls(fs.readFileSync(full)).filter(c => CANARIES.has(c));
            if (hits.length) {
                bad.push({ file: path.relative(tmp, full).replace(/\\/g, '/'), hits });
            }
        }
    })(path.join(tmp, 'com'));

    if (bad.length) {
        console.error('');
        console.error('PARTIALLY REOBFUSCATED JAR - refusing to ship it.');
        for (const b of bad.slice(0, 10)) {
            console.error('  ' + b.file + '  calls ' + b.hits[0] + ' by its official name');
        }
        if (bad.length > 10) console.error('  ...and ' + (bad.length - 10) + ' more');
        console.error('');
        console.error('  A build was interrupted and then resumed over its own half-written');
        console.error('  output. Clean and build again:');
        console.error('    .\\.tools\\gradle\\gradle-8.1.1\\bin\\gradle.bat clean --no-daemon');
        process.exit(1);
    }
    console.log('Mapping check: ' + checked + ' classes, no unmapped Minecraft calls.');
} finally {
    fs.rmSync(tmp, { recursive: true, force: true });
}
