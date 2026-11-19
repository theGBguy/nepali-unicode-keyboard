# Word list for Romanized typing

`app/src/main/assets/words_ne.tsv` is built by `build_wordlist.py` from:

1. **Word frequencies counted from the Nepali Wikipedia** article dump
   (<https://dumps.wikimedia.org/newiki/>). Wikipedia text is licensed under
   [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); the word list derived from
   it is shared under the same licence. Attribution: "Contains word frequencies derived from
   Nepali Wikipedia (CC BY-SA 4.0)." This line must appear in the app's About/Licences screen.
2. **`colloquial_ne.tsv`**: everyday and chat words written for this project, with boosted
   frequencies so they win over formal Wikipedia vocabulary.

Not used: the Leipzig Corpora Collection (its terms are CC BY-NC, which rules out an app that
may show ads).

Rebuild after editing `colloquial_ne.tsv`:

```sh
curl -L -o /tmp/newiki.xml.bz2 https://dumps.wikimedia.org/newiki/latest/newiki-latest-pages-articles.xml.bz2
python3 tools/wordlist/build_wordlist.py /tmp/newiki.xml.bz2
```

Then run the unit tests (`NepaliWordEngineTest` checks real words against the list).

# Word list for English suggestions

`app/src/main/assets/words_en.tsv` is built by `build_english_wordlist.py` from:

1. **Word counts from OpenSubtitles 2018**, as published by Hermit Dave's
   [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (`en_50k`). The content is
   licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); the word list
   derived from it is shared under the same licence. Subtitles are spoken, everyday English, close
   to what people type in chats.
2. **[SCOWL](http://wordlist.aspell.net/)** 2020.12.07, sizes up to 60 (© Kevin Atkinson; permission
   to use, copy, modify, distribute and sell is granted provided the copyright notice is kept).
   Its full copyright and permission notice is in `SCOWL-COPYRIGHT.txt`. Only words SCOWL lists are kept, with SCOWL's capitals (I, I'm, London).
3. **`extra_en.tsv`**: contractions (the subtitle counts split "don't" into "don" + "'t") and
   Nepal's places, festivals and everyday words, with estimated counts.
4. **`blocked_en.txt`**: profanity and slurs, never suggested.

Attribution (in the app's Licences screen on both platforms): "Contains word frequencies derived
from OpenSubtitles by Hermit Dave's FrequencyWords, under CC BY-SA 4.0, checked against SCOWL,
© Kevin Atkinson, used with permission."

Rebuild after editing `extra_en.tsv` or `blocked_en.txt` (downloads go to the git-ignored
`tools/wordlist/en-cache/`):

```sh
python3 tools/wordlist/build_english_wordlist.py
```

Then run `EnglishWordListTest`.
