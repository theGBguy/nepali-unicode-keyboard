#!/usr/bin/env python3
"""Builds app/src/main/assets/words_ne.tsv, the keyboard's Romanized-typing word list.

Sources:
  1. Word counts from the Nepali Wikipedia dump (text CC BY-SA 4.0; see README.md here).
  2. colloquial_ne.tsv: everyday words written for this project (Wikipedia is formal).

Usage (from the repo root; the dump is ~55 MB and is not committed):
  curl -L -o /tmp/newiki.xml.bz2 https://dumps.wikimedia.org/newiki/latest/newiki-latest-pages-articles.xml.bz2
  python3 tools/wordlist/build_wordlist.py /tmp/newiki.xml.bz2

Output lines: word<TAB>frequency[<TAB>aliases], most frequent first.
"""
import bz2
import collections
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "words_ne.tsv")
MAX_WORDS = 60000
MIN_COUNT = 3
MAX_LEN = 20

# Devanagari letters and signs used in Nepali, excluding danda and digits
TOKEN = re.compile(r"[\u0900-\u0963\u0971-\u097F]+")
# A word must start with an independent vowel or consonant (not a vowel sign or halant)
VALID_START = re.compile(r"^[\u0905-\u0914\u0915-\u0939\u0958-\u095F]")
# Wikipedia markup words that dominate the counts but are never typed in chat
MARKUP = {"श्रेणी", "चित्र", "सन्दर्भ", "सामग्रीहरू", "जनगणना", "बाह्य", "कडीहरू", "स्रोत", "टिप्पणी"}


def wiki_counts(path):
    counts = collections.Counter()
    in_text = False
    with bz2.open(path, "rt", encoding="utf-8") as f:
        for line in f:
            if "<text" in line:
                in_text = True
            if in_text:
                counts.update(TOKEN.findall(line))
            if "</text>" in line:
                in_text = False
    return counts


def colloquial():
    rows = []
    with open(os.path.join(HERE, "colloquial_ne.tsv"), encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line.strip() or line.startswith("#"):
                continue
            parts = line.split("\t")
            rows.append((parts[0], int(parts[1]), parts[2] if len(parts) > 2 else ""))
    return rows


def main():
    counts = wiki_counts(sys.argv[1])
    words = {}
    for word, n in counts.most_common():
        if n < MIN_COUNT or len(words) >= MAX_WORDS:
            break
        if word in MARKUP or len(word) > MAX_LEN or not VALID_START.match(word):
            continue
        words[word] = [n, ""]
    for word, n, aliases in colloquial():
        entry = words.setdefault(word, [0, ""])
        entry[0] = max(entry[0], n)
        entry[1] = aliases
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("# Nepali word list for Romanized typing. Built by tools/wordlist/build_wordlist.py\n")
        f.write("# from Nepali Wikipedia word counts (CC BY-SA 4.0) and tools/wordlist/colloquial_ne.tsv.\n")
        for word, (n, aliases) in sorted(words.items(), key=lambda kv: -kv[1][0]):
            f.write(f"{word}\t{n}\t{aliases}\n" if aliases else f"{word}\t{n}\n")
    print(f"wrote {len(words)} words to {os.path.normpath(OUT)} ({os.path.getsize(OUT) // 1024} KB)")


if __name__ == "__main__":
    main()
