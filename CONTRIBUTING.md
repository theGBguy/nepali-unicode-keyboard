# Contributing

Thanks for helping with Nepali Unicode Keyboard. There are three ways to contribute, and you don't need to write code for the first two.

## Report a wrong or missing word

If a Romanized spelling gives the wrong Nepali word, or a common word is missing, use the [wrong word form](https://github.com/theGBguy/nepali-unicode-keyboard/issues/new?template=wrong-word.yml). Tell us what you typed, what you got and what you expected. These reports improve the keyboard the most.

## Report a bug

For a crash, a wrong character or a key that does nothing, use the [bug report form](https://github.com/theGBguy/nepali-unicode-keyboard/issues/new?template=bug-report.yml). Include your phone model, Android version and the app version (Settings, at the bottom).

Questions and feature ideas go in [Discussions](https://github.com/theGBguy/nepali-unicode-keyboard/discussions). Security or privacy problems go through [SECURITY.md](SECURITY.md), not a public issue.

## Contribute code

Small fixes can go straight to a pull request. For anything bigger, such as a new feature, a layout change or a refactor, open an issue or discussion first so we can agree on the approach before you spend time on it.

### Setup

You need Android Studio (or the Gradle wrapper) and JDK 17. A debug build needs no signing setup:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :shared:testAndroidHostTest
```

### Fixing words

1. Add or adjust the word in `tools/wordlist/colloquial_ne.tsv`.
2. Rebuild `app/src/main/assets/words_ne.tsv` as described in [tools/wordlist/README.md](tools/wordlist/README.md).
3. Add the spelling to `NepaliWordEngineTest` so it can't regress.

### Pull request rules

- Unit tests pass. CI runs them on every pull request.
- New user-facing text goes in both `app/src/main/res/values/strings.xml` and `values-ne/strings.xml`. If you can't write the Nepali, say so in the PR and it'll be translated before merge.
- **No new permissions, network calls, analytics or tracking.** The app promises that nothing you type leaves the phone, and every change has to keep that promise.
- No new dependency without asking first.
- Keep one change per pull request.

### Sign your commits (DCO)

Every commit needs a `Signed-off-by` line. It certifies that you wrote the change or have the right to submit it, under the [Developer Certificate of Origin](https://developercertificate.org/). Add it with `-s`:

```sh
git commit -s -m "fix(words): dherai gives धेरै"
```

To sign off commits you've already made, run `git rebase --signoff main` and force-push your branch.

### Licence

By contributing, you agree that your contribution is licensed under the project's [GPL-3.0](LICENSE) **including the additional permission in [NOTICE](NOTICE)** for linking with Google ML Kit and Play services. Changes to the word list are licensed under CC BY-SA 4.0, like the rest of the list.

## After your change is merged

Merged changes ship in the next Google Play release, which the maintainer builds and signs. Contributors are credited in the release notes.
