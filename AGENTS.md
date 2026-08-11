# AGENTS.md

FeignClient Assistant (ID `com.lyflexi.feignx`, package `com.lyflexi.feignx`) — an IntelliJ IDEA plugin that draws gutters to navigate between Spring Cloud `@FeignClient` interfaces and `@RestController`/`@Controller` classes, plus URL-to-clipboard copy. Formerly named FeignX. All code/comments/changelog are mostly Chinese.

## Build

- Gradle project root is `feignx/` — the repo root has no build files. Run gradle from there, e.g. `feignx\gradlew.bat buildPlugin`.
- Wrapper Gradle 7.4.2, `org.jetbrains.intellij` 1.5.2. First build downloads the IntelliJ 2021.2 (IC) SDK (`intellij.version` in `feignx/build.gradle.kts`); slow and offline-sensitive.
- Java 11 (`sourceCompatibility`/`targetCompatibility`), UTF-8, `since-build=203` (IDEA 2020.3+), depends on `com.intellij.modules.java`.
- Dev loop: `runIde` launches a sandboxed IDE (`-Xmx4096m` already set); `.run/Run Plugin.run.xml` wires it from IDEA.
- **No automated tests and no CI.** Verify manually in the sandbox IDE against the bundled multi-module Maven sample app `feignx/sample/debug_openfeign` (client/server/api modules with feign clients and controllers).

## Versioning (easy to miss)

- Version `5.6.4.4` is duplicated and must match in both `feignx/build.gradle.kts` (`version =`) and `feignx/src/main/resources/META-INF/plugin.xml` (`<version>`). Bump both and add a `feignx/docs/updateLog.md` entry.
- `publishPlugin`/`signPlugin` read env vars only: `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`.

## Architecture

- Entry points are registered in `feignx/src/main/resources/META-INF/plugin.xml`: four `LineMarkerProvider`s (`F2C`, `C2F`, `CopyControllerUrl`, `CopyFeignUrl`), two tab `IconProvider`s, settings `UserPluginConfigurableUI`, and the listeners.
- Providers read the static `BilateralCacheManager` (maps keyed per project by `project.getBasePath()`; inner key = `PsiClass.qualifiedName + methodName`). `InitialPsiClassCacheManager` does the one-time project scan; `PsiClassGitChangeListener` (PSI tree) + `CacheCleanListener` (project open/close) refresh caches.
- Scan via `ControllerClassScanUtils`/`FeignClassScanUtils`. The `scanAll*By*` methods in `ProjectUtils` are `@Deprecated` and return empty — don't use or "fix" them.
- Annotation matching: `AnnotationParserUtils` + `SpringCloudClassAnnotation`/`SpringBootClassAnnotation`/`SpringBootMethodAnnotation` enums. Prefer `PsiMethod.hasAnnotation(...)` over manual string checks.
- URL prefixes (`server.servlet.context-path`, `spring.mvc.servlet.path`) are parsed from `application|bootstrap.{properties,yml,yaml}` by `properties/ConfigReader` + `ServerParser` (snakeyaml 1.29 is bundled for this — keep it).
- Legacy `ApplicationComponent`/`ProjectComponent` APIs are still used and registered in plugin.xml; keep that registration in sync when moving those classes.

## Gotchas (all learned from past bug fixes in the changelog)

- Every provider/listener must guard with `ProjectUtils.isBizElement(element)` (excludes jar/library PSI) and `DumbService.isDumb(project)` before touching PSI, and access PSI only inside read actions / EDT. Skipping these caused `PsiInvalidElementAccessException`, `IndexNotReadyException`, and freezes.
- Gutter icons are intentionally anchored to the Restful annotation element, not the method name (keeps gutters stable during Enter/comment edits) — see comment in `F2CLineMarkerProvider`.
- Editing comments (`/** */`) temporarily strips annotations; cache setters must tolerate null `HttpMappingInfo` (see `BilateralCacheManager.setFeignCache`).
- Do not introduce newer IntelliJ Platform APIs; binary incompatibility with old IDE versions shipped before (v4.1.6 `NoSuchMethodError`). Compile against the 2021.2 SDK.
- Keep the repo's branch/commit style: branch names like `feat/main-*`, `hotfix/main-*`; changelog entries in Chinese.
