#!/usr/bin/env node
// Regenerates src/main/java/com/accountbuildplanner/QuestData.java from the OSRS Wiki.
//
//   node tools/quest-data.js [path/to/runelite-api.jar]
//
// Route order and quest points: the wiki's "Optimal quest guide".
// Requirements: the wiki's Module:Questreq/data, falling back to each quest page's {{Quest details}} for quests the
// module doesn't have yet. Quest constants: RuneLite's Quest enum, read from the runelite-api jar (newest one in the
// Gradle cache unless a path is given). Needs Node 18+ and `java` (11+) on PATH.
'use strict';
const fs = require('fs'), os = require('os'), path = require('path'), { execFileSync } = require('child_process');

const OUT = path.join(__dirname, '..', 'src', 'main', 'java', 'com', 'accountbuildplanner', 'QuestData.java');
const UA = { 'User-Agent': 'osrs-build-planner/1.0 (RuneLite plugin quest data generator)' };
const SKILLS = ['ATTACK', 'DEFENCE', 'STRENGTH', 'HITPOINTS', 'RANGED', 'PRAYER', 'MAGIC', 'COOKING', 'WOODCUTTING',
	'FLETCHING', 'FISHING', 'FIREMAKING', 'CRAFTING', 'SMITHING', 'MINING', 'HERBLORE', 'AGILITY', 'THIEVING', 'SLAYER',
	'FARMING', 'RUNECRAFT', 'HUNTER', 'CONSTRUCTION', 'SAILING'];
// Wiki names for the RFD subquests -> RuneLite constants
const ALIAS = {
	'recipe for disaster/freeing the mountain dwarf': 'RECIPE_FOR_DISASTER__MOUNTAIN_DWARF',
	'recipe for disaster/freeing the goblin generals': 'RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE',
	'recipe for disaster/freeing pirate pete': 'RECIPE_FOR_DISASTER__PIRATE_PETE',
	'recipe for disaster/freeing the lumbridge guide': 'RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE',
	'recipe for disaster/freeing evil dave': 'RECIPE_FOR_DISASTER__EVIL_DAVE',
	'recipe for disaster/freeing skrach uglogwee': 'RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE',
	'recipe for disaster/freeing sir amik varze': 'RECIPE_FOR_DISASTER__SIR_AMIK_VARZE',
	'recipe for disaster/freeing king awowogei': 'RECIPE_FOR_DISASTER__KING_AWOWOGEI',
	'recipe for disaster/defeating the culinaromancer': 'RECIPE_FOR_DISASTER__CULINAROMANCER',
};

const wikiRaw = async title => {
	const res = await fetch('https://oldschool.runescape.wiki/w/' + encodeURIComponent(title.replace(/ /g, '_')) + '?action=raw', { headers: UA });
	if (!res.ok) throw new Error(title + ': HTTP ' + res.status);
	return res.text();
};
const norm = s => s.toLowerCase().replace(/’/g, "'").replace(/\s+/g, ' ').replace(/ - /g, '/').trim();

/** Quest constant names from RuneLite's Quest enum: normalized in-game name -> constant. */
function questEnum(jarArg) {
	let jar = jarArg;
	if (!jar) {
		const dir = path.join(os.homedir(), '.gradle', 'caches', 'modules-2', 'files-2.1', 'net.runelite', 'runelite-api');
		const jars = [];
		for (const v of fs.readdirSync(dir)) for (const h of fs.readdirSync(path.join(dir, v))) for (const f of fs.readdirSync(path.join(dir, v, h)))
			if (f === 'runelite-api-' + v + '.jar') jars.push({ v, f: path.join(dir, v, h, f) });
		jars.sort((a, b) => a.v.localeCompare(b.v, undefined, { numeric: true }));
		if (!jars.length) throw new Error('no runelite-api jar in the Gradle cache; build the project once or pass the jar path');
		jar = jars[jars.length - 1].f;
	}
	const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'questdump-'));
	const src = path.join(tmp, 'QuestDump.java');
	fs.writeFileSync(src, 'public class QuestDump { public static void main(String[] a) { for (net.runelite.api.Quest q : net.runelite.api.Quest.values()) System.out.println(q.name() + "\\t" + q.getName()); } }');
	const out = execFileSync('java', ['-cp', jar, src], { encoding: 'utf8' });
	fs.rmSync(tmp, { recursive: true, force: true });
	console.log('Quest enum from', jar);
	return new Map(out.trim().split(/\r?\n/).map(l => { const [c, n] = l.split('\t'); return [norm(n), c]; }));
}

/** Module:Questreq/data -> { name: { quests: [...], skills: [[name, level, ...flags]] } }. A tiny Lua table reader. */
function parseModule(src) {
	src = src.replace(/--\[\[[\s\S]*?\]\]/g, '').replace(/--[^\n]*/g, '');
	const toks = [];
	for (let i = 0; i < src.length;) {
		const c = src[i];
		if (/\s/.test(c)) { i++; continue; }
		if (c === "'" || c === '"') {
			let s = ''; i++;
			while (src[i] !== c) { if (src[i] === '\\') i++; s += src[i++]; }
			i++; toks.push({ t: 's', v: s }); continue;
		}
		if (/[0-9]/.test(c)) { let s = ''; while (i < src.length && /[0-9.]/.test(src[i])) s += src[i++]; toks.push({ t: 'n', v: +s }); continue; }
		if (/[A-Za-z_]/.test(c)) { let s = ''; while (i < src.length && /\w/.test(src[i])) s += src[i++]; toks.push({ t: 'w', v: s }); continue; }
		toks.push({ t: c }); i++;
	}
	let p = toks.findIndex(t => t.t === 'w' && t.v === 'questReqs') + 2;
	const value = () => toks[p].t === '{' ? table() : toks[p++].v;
	function table() {
		p++; const arr = [], obj = {}; let isObj = false;
		while (toks[p].t !== '}') {
			if (toks[p].t === '[') { p++; const k = toks[p++].v; p += 2; obj[k] = value(); isObj = true; }
			else arr.push(value());
			if (toks[p].t === ',') p++;
		}
		p++; return isObj ? obj : arr;
	}
	return table();
}

/** Same shape as a module entry, read from a quest page's {{Quest details|requirements=...}}. */
function parsePage(text) {
	const start = text.search(/\|\s*requirements\s*=/);
	if (start < 0) return { quests: [], skills: [] };
	const rest = text.slice(start).replace(/^\|\s*requirements\s*=/, '');
	const end = rest.search(/\n\|\s*\w+\s*=/);
	const out = { quests: [], skills: [] };
	let underList = false; // inside "*Completion of the following quests:"
	for (const raw of (end < 0 ? rest : rest.slice(0, end)).split('\n')) {
		const line = raw.trim(), depth = (/^\*+/.exec(line) || [''])[0].length, body = line.slice(depth).trim();
		if (!depth) continue;
		for (const m of body.matchAll(/\{\{SCP\|([^|}]+)\|(\d+)[^}]*\}\}/g)) {
			const sk = m[1].trim();
			if (/^quest/i.test(sk)) out.skills.push(['Quest point', +m[2]]);
			else if (depth === 1) out.skills.push([sk, +m[2]]);
		}
		if (depth === 1) underList = /following quests/i.test(body);
		const link = /\[\[([^\]|#]+)/.exec(body);
		const direct = (depth === 1 && /^(completion of\s+|started\s+|partial completion of\s+)?\[\[/i.test(body))
			|| (depth === 2 && underList && /^\[\[/.test(body));
		if (direct && link) out.quests.push((/\b(started|partial completion)\b/i.test(body) ? 'Started:' : '') + link[1].trim());
	}
	return out;
}

(async () => {
	const byName = questEnum(process.argv[2]);
	const toEnum = n => ALIAS[norm(n)] || byName.get(norm(n));
	const module = parseModule(await wikiRaw('Module:Questreq/data'));
	const moduleBy = new Map(Object.entries(module).map(([k, v]) => [toEnum(k), v]));

	// Guide rows in order: constant, QP, display name
	const guide = await wikiRaw('Optimal quest guide');
	const rows = [], missing = [], seen = new Set();
	for (const part of guide.split('|- data-rowid="').slice(1)) {
		const head = part.split('\n|-')[0];
		const name = (/\[\[([^\]|#]+)/.exec(head) || [, part.slice(0, part.indexOf('"'))])[1].trim();
		const qp = +((/Optimal quest\/qp\|(\d+)/.exec(head) || [, 0])[1]);
		const c = toEnum(name) || toEnum(part.slice(0, part.indexOf('"')));
		if (!c) { if (qp > 0) missing.push(name); continue; }
		if (!seen.has(c)) { seen.add(c); rows.push({ c, qp, name }); }
	}

	// Requirements per constant: module first, quest page for anything the module lacks
	const reqs = new Map();
	for (const r of rows) {
		let e = moduleBy.get(r.c);
		if (!e) { e = parsePage(await wikiRaw(r.name)); console.log('  from quest page:', r.name); }
		reqs.set(r.c, e);
	}

	// Route: quests (QP > 0) and everything they need, in guide order
	const need = new Set();
	const add = c => { if (need.has(c)) return; need.add(c); for (const q of (reqs.get(c) || {}).quests || []) { const d = toEnum(q); if (d) add(d); } };
	rows.filter(r => r.qp > 0).forEach(r => add(r.c));
	const notInGuide = [...need].filter(c => !seen.has(c));
	if (notInGuide.length) throw new Error('needed but not in the Optimal quest guide: ' + notInGuide.join(', '));
	const route = rows.filter(r => need.has(r.c));
	const pos = new Map(route.map((r, i) => [r.c, i]));

	const lines = [];
	let total = 0;
	for (const [i, r] of route.entries()) {
		const e = reqs.get(r.c), after = [], skills = [], notes = [];
		let qpReq = 0;
		for (const q of e.quests || []) {
			const started = q.startsWith('Started:'), name = started ? q.slice(8) : q, d = toEnum(name);
			if (d && !started) {
				if (!(pos.get(d) < i)) throw new Error(r.name + ' needs ' + name + ' earlier in the route');
				after.push(d);
			}
			else if (!(d && pos.get(d) < i)) notes.push((started ? 'start ' : 'needs ') + name);
		}
		for (const [sk, lvl, ...flags] of e.skills || []) {
			if (flags.includes('ironman')) continue;
			if (sk === 'Quest point') qpReq = lvl;
			else if (SKILLS.includes(sk.toUpperCase())) skills.push([sk.toUpperCase(), lvl]);
			else notes.push(sk + ' ' + lvl);
		}
		if (qpReq > total) throw new Error(r.name + ' needs ' + qpReq + ' QP, the route only has ' + total + ' by then');
		if (i === route.length - 1 && missing.length) notes.push('also do ' + missing.join(', ') + ' (RuneLite cannot track them yet)');
		let s = 'q(' + r.c + ', ' + r.qp + ')';
		if (after.length) s += '.after(' + after.join(', ') + ')';
		for (const [k, v] of skills) s += '.skill(' + k + ', ' + v + ')';
		if (qpReq) s += '.qpReq(' + qpReq + ')';
		if (notes.length) s += '.note(' + JSON.stringify(notes.join(', ')) + ')';
		lines.push('\t\t\t' + s);
		total += r.qp;
	}

	const today = new Date().toISOString().slice(0, 10);
	fs.writeFileSync(OUT, `package com.accountbuildplanner;

import static com.accountbuildplanner.Step.q;
import java.util.List;
import static net.runelite.api.Quest.*;
import static net.runelite.api.Skill.*;

/**
 * Every quest RuneLite can track, plus the miniquests they need, in the OSRS Wiki's Optimal quest guide order.
 * Generated by tools/quest-data.js on ${today} from the OSRS Wiki. Don't edit by hand: rerun the tool.
 */
final class QuestData
{
	private QuestData()
	{
	}

	/** Fresh steps on every call, so each build can stage its own copy. */
	static List<Step> optimalOrder()
	{
		return List.of(
${lines.join(',\n')}
		);
	}
}
`);
	console.log(route.length + ' steps, ' + total + ' QP -> ' + OUT);
	if (missing.length) console.log('Not in RuneLite yet (left out): ' + missing.join(', '));
})().catch(e => { console.error('quest-data: ' + e.message); process.exit(1); });
