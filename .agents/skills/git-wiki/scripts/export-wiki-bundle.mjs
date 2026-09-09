#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const DEFAULT_ALLOWED_EXTENSIONS = [".md"];
const DEFAULT_IGNORED_FOLDERS = [
  ".git",
  "node_modules",
  ".obsidian",
  "dist",
  "bin",
  "out",
];
const DEFAULT_FRONTMATTER_KEYS = [
  "title",
  "type",
  "status",
  "language",
  "tags",
  "description",
  "source_paths",
  "updated_at",
  "last_commit",
];

function usage() {
  process.stderr.write(
    [
      "Usage:",
      "  node skills/git-wiki/scripts/export-wiki-bundle.mjs \\",
      "    --input-dir .wiki",
      "  node skills/git-wiki/scripts/export-wiki-bundle.mjs \\",
      "    --input-dir .wiki-fr --root-name French",
      "",
      "Optional:",
      "  --output-file /path/to/index-YYYYMMDD-HHMMSS.md",
      "  --root-name <name>",
      "  --allowed-extensions .md,.txt",
      "  --ignored-folders .git,node_modules,.obsidian,dist,bin,out",
      "  --keep-frontmatter-keys title,type,status,language,tags,description,source_paths,updated_at,last_commit",
    ].join("\n"),
  );
}

function die(message) {
  process.stderr.write(`${message}\n`);
  process.exit(2);
}

function getArg(argv, name, fallback = null) {
  const index = argv.indexOf(`--${name}`);
  if (index === -1) return fallback;
  if (index === argv.length - 1) die(`Missing value for --${name}`);
  return argv[index + 1];
}

function hasFlag(argv, name) {
  return argv.includes(`--${name}`);
}

function parseCsv(value, fallback) {
  if (!value) return [...fallback];
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function toPosixPath(value) {
  return value.split(path.sep).join("/");
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function slugifyPath(value) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function cleanScalar(value) {
  return value.replace(/^['"]|['"]$/g, "").trim();
}

function getSimpleTimestamp(date = new Date()) {
  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const seconds = String(date.getSeconds()).padStart(2, "0");
  return `${year}${month}${day}-${hours}${minutes}${seconds}`;
}

function formatMetadataValue(value) {
  if (Array.isArray(value)) {
    return `[${value.join(", ")}]`;
  }
  return String(value);
}

function parseSimpleYaml(yamlString, keepKeys) {
  const wanted = new Set(keepKeys.map((key) => key.toLowerCase()));
  const data = {};
  let currentListKey = null;
  let currentBlockKey = null;
  let currentBlockIndent = 0;
  let currentBlockLines = [];

  for (const rawLine of yamlString.split(/\r?\n/)) {
    if (currentBlockKey) {
      const blockMatch = rawLine.match(/^(\s+)(.*)$/);
      const blockIndent = blockMatch ? blockMatch[1].length : 0;

      if (blockMatch && blockIndent > currentBlockIndent) {
        currentBlockLines.push(blockMatch[2]);
        continue;
      }

      data[currentBlockKey] = currentBlockLines.join(" ").trim();
      currentBlockKey = null;
      currentBlockIndent = 0;
      currentBlockLines = [];
    }

    const keyMatch = rawLine.match(/^([A-Za-z0-9_-]+):\s*(.*)$/);
    if (keyMatch) {
      const key = keyMatch[1].toLowerCase();
      const value = keyMatch[2].trim();

      currentListKey = null;
      if (!wanted.has(key)) continue;

      if (value === "|" || value === ">") {
        data[key.toUpperCase()] = "";
        currentBlockKey = key.toUpperCase();
        currentBlockIndent = rawLine.match(/^(\s*)/)?.[1]?.length ?? 0;
        currentBlockLines = [];
        continue;
      }

      if (value === "") {
        data[key.toUpperCase()] = [];
        currentListKey = key.toUpperCase();
        continue;
      }

      if (value.startsWith("[") && value.endsWith("]")) {
        const items = value
          .slice(1, -1)
          .split(",")
          .map((item) => cleanScalar(item))
          .filter(Boolean);
        data[key.toUpperCase()] = items;
        continue;
      }

      data[key.toUpperCase()] = cleanScalar(value);
      continue;
    }

    const listMatch = rawLine.match(/^\s*-\s*(.*)$/);
    if (listMatch && currentListKey) {
      data[currentListKey].push(cleanScalar(listMatch[1]));
      continue;
    }

    currentListKey = null;
  }

  if (currentBlockKey) {
    data[currentBlockKey] = currentBlockLines.join(" ").trim();
  }

  return data;
}

function cleanMarkdown(content) {
  let body = content;
  const protectedSegments = [];

  function protect(pattern) {
    body = body.replace(pattern, (match) => {
      const token = `@@WIKI_EXPORT_${protectedSegments.length}@@`;
      protectedSegments.push(match);
      return token;
    });
  }

  protect(/```[\s\S]*?```/g);
  protect(/`[^`\n]+`/g);

  body = body.replace(/!\[\[.*?\]\]/g, "");
  body = body.replace(/\[\[(?:[^|\]]*\|)?([^\]]+)\]\]/g, "$1");

  body = body.replace(/@@WIKI_EXPORT_(\d+)@@/g, (_, index) => protectedSegments[Number(index)]);
  return body.trim();
}

function getAllFiles(dirPath, options, arrayOfFiles = []) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      if (!options.ignoredFolders.includes(entry.name)) {
        getAllFiles(fullPath, options, arrayOfFiles);
      }
      continue;
    }

    if (!options.allowedExtensions.includes(path.extname(entry.name))) {
      continue;
    }

    if (path.resolve(fullPath) === options.outputFileAbsolute) {
      continue;
    }

    arrayOfFiles.push(fullPath);
  }

  return arrayOfFiles;
}

function processFile(filePath, options) {
  const relativePath = toPosixPath(path.relative(options.inputDirAbsolute, filePath));
  const rawContent = fs.readFileSync(filePath, "utf8");

  let metadata = { FILE: `/${relativePath}` };
  let body = rawContent;

  const frontmatterMatch = rawContent.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (frontmatterMatch) {
    const extracted = parseSimpleYaml(frontmatterMatch[1], options.keepFrontmatterKeys);
    metadata = { ...metadata, ...extracted };
    body = rawContent.slice(frontmatterMatch[0].length);
  }

  if (path.extname(filePath) === ".md") {
    body = cleanMarkdown(body);
  }

  const metaBlock = Object.entries(metadata)
    .map(([key, value]) => `${key}: ${formatMetadataValue(value)}`)
    .join("\n");
  const anchor = `file-${slugifyPath(relativePath)}`;

  return {
    tocEntry: {
      anchor,
      path: metadata.FILE,
      relPath: relativePath,
      tags: formatMetadataValue(metadata.TAGS || "").trim(),
    },
    formattedContent: [
      `<a id="${escapeHtml(anchor)}"></a>`,
      `## ${metadata.FILE}`,
      "",
      "---",
      metaBlock,
      "---",
      `# [FILE CONTENT START: ${metadata.FILE}]`,
      "",
      body.trim(),
      "",
      `# [FILE CONTENT END: ${metadata.FILE}]`,
      "",
      "---",
      "",
    ].join("\n"),
  };
}

function ensureParentDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function run() {
  const argv = process.argv.slice(2);
  if (hasFlag(argv, "help") || hasFlag(argv, "h")) {
    usage();
    process.exit(0);
  }

  const inputDir = getArg(argv, "input-dir", ".wiki");
  const inputDirAbsolute = path.resolve(process.cwd(), inputDir);
  const outputFile =
    getArg(argv, "output-file") ??
    path.join(inputDirAbsolute, `index-${getSimpleTimestamp()}.md`);
  const outputFileAbsolute = path.resolve(process.cwd(), outputFile);

  const options = {
    inputDirAbsolute,
    outputFileAbsolute,
    allowedExtensions: parseCsv(getArg(argv, "allowed-extensions"), DEFAULT_ALLOWED_EXTENSIONS),
    ignoredFolders: parseCsv(getArg(argv, "ignored-folders"), DEFAULT_IGNORED_FOLDERS),
    keepFrontmatterKeys: parseCsv(
      getArg(argv, "keep-frontmatter-keys"),
      DEFAULT_FRONTMATTER_KEYS,
    ),
  };

  const rootName = getArg(argv, "root-name", path.basename(inputDirAbsolute) || "wiki");

  if (!fs.existsSync(inputDirAbsolute)) {
    die(`Input directory not found: ${inputDirAbsolute}`);
  }

  const allFiles = getAllFiles(inputDirAbsolute, options)
    .sort((left, right) => left.localeCompare(right));

  if (allFiles.length === 0) {
    die(`No matching files found under ${inputDirAbsolute}`);
  }

  const results = allFiles.map((filePath) => processFile(filePath, options));
  const generatedAt = new Date().toISOString().slice(0, 10);

  let finalOutput = "# REPOSITORY MASTER BUNDLE\n";
  finalOutput += `**Generated:** ${generatedAt}\n`;
  finalOutput += `**Root:** ${rootName}\n`;
  finalOutput += `**Source Directory:** ${toPosixPath(path.relative(process.cwd(), inputDirAbsolute) || ".")}\n\n`;
  finalOutput += "## TABLE OF CONTENTS\n";

  results.forEach((result, index) => {
    const tagNote = result.tocEntry.tags ? ` - Tags: ${result.tocEntry.tags}` : "";
    finalOutput += `${index + 1}. [\`${result.tocEntry.path}\`](#${result.tocEntry.anchor})${tagNote}\n`;
  });

  finalOutput += "\n---\n\n";

  for (const result of results) {
    finalOutput += result.formattedContent;
  }

  ensureParentDir(outputFileAbsolute);
  fs.writeFileSync(outputFileAbsolute, finalOutput, "utf8");

  process.stdout.write(
    [
      `Bundle created: ${outputFileAbsolute}`,
      `Files bundled: ${results.length}`,
    ].join("\n"),
  );
}

run();
