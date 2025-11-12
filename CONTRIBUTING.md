## Doki contribution guidelines

**About compatibility:**

+ **The [base](https://github.com/DokiTeam/Doki/tree/base) branch will be the main branch. It will only work with Android API 21, 22 / Android 5.0 and Android 5.1 devices.**<br>
+ **The [legacy](https://github.com/DokiTeam/Doki/tree/legacy) branch will be the secondary branch. It will be suitable for Android API 23+ / Android 6.0+ devices**

<i>👉 **Make sure the pull request you are about to contribute will be suitable for both branches or just one of them (select the checkbox when submitting your pull request)**</i>

+ If you want to **fix bugs** or **implement new features** that **already have an [issue card](https://github.com/DokiTeam/Doki/issues)**: Please assign this issue to you and/or comment about it.
+ If you want to **implement a new feature**: Open an issue or discussion regarding it to ensure it will be accepted.
+ **Translations: TODO**
+ In case you want to **add a new extension**, refer to the [extensions repository](https://github.com/DokiTeam/doki-exts).

**Refactoring** or some **dev-faces improvements** might also be accepted. However, please stick to the following principles:

+ **Performance matters.** In the case of choosing between source code beauty and performance, performance should be a priority.
+ Please, **do not modify readme and other information files** (except for typos).
+ **Avoid adding new dependencies** unless required. APK size is important.
