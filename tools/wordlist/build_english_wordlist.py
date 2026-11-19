#!/usr/bin/env python3
"""Builds app/src/main/assets/words_en.tsv, the word list for English suggestions.

Sources (see README.md here for the licences):
  1. Word counts from OpenSubtitles 2018 (hermitdave/FrequencyWords en_50k, CC BY-SA 4.0):
     spoken, everyday English, closer to what people type in chats than an encyclopedia.
  2. SCOWL (Kevin Atkinson, permissive licence), sizes up to 70: keeps only real words and gives
     each its usual capitals (I, I'm, London), which the lowercase counts have lost.
  3. extra_en.tsv: contractions (the subtitle counts split "don't" into "don" + "'t", so they get
     counts here and their first part loses them) and Nepal's names and words, which subtitles
     rarely use.
  4. blocked_en.txt: words never suggested (they can still be typed).

Usage (from the repo root; the downloads go to the git-ignored tools/wordlist/en-cache/):
  python3 tools/wordlist/build_english_wordlist.py

Output lines: word<TAB>frequency, most frequent first.
"""
import collections
import os
import tarfile
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "en-cache")
OUT = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "words_en.tsv")

FREQ_URL = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_50k.txt"
SCOWL_VERSION = "2020.12.07"
SCOWL_URL = f"https://sourceforge.net/projects/wordlist/files/SCOWL/{SCOWL_VERSION}/scowl-{SCOWL_VERSION}.tar.gz/download"
SCOWL_MAX_SIZE = 60
SCOWL_SPELLINGS = ("english", "american", "british", "british_z", "canadian", "australian")
# Abbreviations are left out: they bring "Th", "Hel", "Lon" into the completions
SCOWL_KINDS = ("words", "upper", "contractions", "proper-names")
MAX_WORDS = 40000
MIN_COUNT = 20
MIN_COUNT_TWO_LETTERS = 50000
MIN_COUNT_SHORT_NAMES = 5000


def fetch(url, name):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    if not os.path.exists(path):
        print("Downloading", url)
        urllib.request.urlretrieve(url, path)
    return path


def scowl_forms():
    """Lowercase word -> the forms SCOWL spells it with."""
    tar_path = fetch(SCOWL_URL, f"scowl-{SCOWL_VERSION}.tar.gz")
    root = os.path.join(CACHE, f"scowl-{SCOWL_VERSION}")
    if not os.path.isdir(root):
        with tarfile.open(tar_path) as tar:
            tar.extractall(CACHE)
    forms = collections.defaultdict(set)
    final = os.path.join(root, "final")
    for name in os.listdir(final):
        base, _, size = name.rpartition(".")
        spelling, _, kind = base.partition("-")
        if spelling not in SCOWL_SPELLINGS or kind not in SCOWL_KINDS or int(size) > SCOWL_MAX_SIZE:
            continue
        with open(os.path.join(final, name), encoding="latin-1") as f:
            for line in f:
                word = line.strip()
                if word and word.isascii():
                    forms[word.lower()].add(word)
    return forms


def pick_form(forms):
    """The lowercase spelling if SCOWL has one (may, not May), else its capitalised one."""
    lower = [w for w in forms if w == w.lower()]
    if lower:
        return lower[0]
    return sorted(forms, key=lambda w: (sum(c.isupper() for c in w), w))[0]


def read_tsv(name):
    rows = []
    with open(os.path.join(HERE, name), encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                rows.append(line.split("\t"))
    return rows


def main():
    counts = collections.Counter()
    with open(fetch(FREQ_URL, "en_50k.txt"), encoding="utf-8") as f:
        for line in f:
            word, _, count = line.strip().partition(" ")
            if word.isascii() and word.replace("'", "").isalpha():
                counts[word.lower()] += int(count)

    # Contractions: the subtitle tokenizer split them, so their first part ("don") holds their count
    extra = {}
    for word, count, *stem in read_tsv("extra_en.tsv"):
        extra[word] = int(count)
        if stem:
            counts[stem[0]] = max(counts[stem[0]] - int(count), 0)

    blocked = {w[0].lower() for w in read_tsv("blocked_en.txt")}
    forms = scowl_forms()
    words = {}
    for key, count in counts.items():
        if count < MIN_COUNT or key in blocked or key not in forms:
            continue
        if len(key) == 1 and key not in ("a", "i"):
            continue
        form = "I" if key == "i" else pick_form(forms[key])
        # Short words are mostly symbols and abbreviations (Cd, Th, ls): keep only common ones
        if len(key) == 2 and count < MIN_COUNT_TWO_LETTERS:
            continue
        if len(key) == 3 and form[0].isupper() and count < MIN_COUNT_SHORT_NAMES:
            continue
        # SCOWL lists the letter i too; the word is always I
        words[form] = count
    for word, count in extra.items():
        words.pop(word.lower(), None)
        words[word] = count

    ranked = sorted(words.items(), key=lambda kv: (-kv[1], kv[0]))[:MAX_WORDS]
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("# English word list for suggestions. Built by tools/wordlist/build_english_wordlist.py\n")
        f.write("# from OpenSubtitles word counts (FrequencyWords, CC BY-SA 4.0) filtered through SCOWL.\n")
        for word, count in ranked:
            f.write(f"{word}\t{count}\n")
    print(f"Wrote {len(ranked)} words to {os.path.relpath(OUT)}")


if __name__ == "__main__":
    main()
