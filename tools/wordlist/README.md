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
