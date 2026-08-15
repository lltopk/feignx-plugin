# AGENTS.md

FeignClient Assistant (ID `com.lyflexi.feignx`, package `com.lyflexi.feignx`) — an IntelliJ IDEA plugin that draws gutters to navigate between Spring Cloud `@FeignClient` interfaces and `@RestController`/`@Controller` classes, plus an Endpoints tool window listing all SpringBoot/MVC/Feign endpoints with HTTP request scripting. Formerly named FeignX. All code/comments/changelog are mostly Chinese.

## Build

- Gradle project root is `feignx/` — the repo root has no build files. Run gradle from there, e.g. `feignx\gradlew.bat buildPlugin`.
- Wrapper Gradle 7.4.2, `org.jetbrains.intellij` 1.5.2. First build downloads the IntelliJ 2021.2 (IC) SDK (`intellij.version` in `feignx/build.gradle.kts`); slow and offline-sensitive.
- Java 11 (`sourceCompatibility`/`targetCompatibility`), UTF-8, `since-build=203` (IDEA 2020.3+), depends on `com.intellij.modules.java`.
- Dev loop: `runIde` launches a sandboxed IDE (`-Xmx4096m` already set); `.run/Run Plugin.run.xml` wires it from IDEA.
- **The `feignx` module uses JDK 11** (`sourceCompatibility`/`targetCompatibility`, Gradle toolchain targets Java 11) — build/compile the plugin with JDK 11, e.g. `JAVA_HOME=<jdk11> feignx\gradlew.bat buildPlugin`. Do not use JDK 17+ for the feignx module itself (the mirror sample at `springcloud-openfeign-practice` may use corretto-17).
- **No automated tests and no CI.** Verify manually in the sandbox IDE against the bundled multi-module Maven sample app `feignx/sample/debug_openfeign` (client/server/api modules with feign clients and controllers).
- **Every manual test case (issue reproduction / demo) added under `feignx/sample/debug_openfeign` must ALSO be mirrored** into `E:\github\springcloud-openfeign-practice\debug-openfeign` (same modules/structure). The mirror project is a real Maven build — compile it (`JAVA_HOME=corretto-17`, `mvn -o -q -DskipTests compile`) to catch type errors the plugin sample never compiles. Its existing controllers/feign paths use no `/hello/world` prefix; when adding a module-level `server.servlet.context-path`, update existing feign client paths accordingly so the original demo keeps matching.

## Versioning (easy to miss)

- Version `5.7.2` is duplicated and must match in both `feignx/build.gradle.kts` (`version =`) and `feignx/src/main/resources/META-INF/plugin.xml` (`<version>`). Bump both and add a `feignx/docs/updateLog.md` entry.
- `publishPlugin`/`signPlugin` read env vars only: `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`.

## Architecture

- Entry points are registered in `feignx/src/main/resources/META-INF/plugin.xml`: three `LineMarkerProvider`s (`F2C`, `C2F`, `NavigateToEndpoints`), two tab `IconProvider`s (`FeignClassIconProvider`/`RestControllerIconProvider`), the settings page `UserPluginConfigurableUI`, a `StatusBarWidgetFactory`, and the Endpoints `toolWindow`. There are no listeners and no legacy components anymore.
- Package layout: `core/` (scanning engine `PsiCoreEngine`, deprecated multi-project helper `PsiCoreEngineMultiProject`, refresh orchestrator `ProjectRefreshManager`), `resolver/` (controller/feign path resolvers), `utils/` (helpers incl. `BizChecker`).
- Everything is computed on demand from PSI, relying on IntelliJ's own PSI index/caching:
  - `resolver/ControllerMappingResolver.scanControllerPaths` / `resolver/FeignMappingResolver.scanFeignInterfaces` scan every time a gutter is collected, via `core/PsiCoreEngine.searchClassesByAnnotation` (project-scope only) instead of walking all packages. `PsiCoreEngine` queries the Java annotation stub index (`JavaAnnotationIndex`, keyed by short name) then matches precisely with `ref.resolve()` + `areElementsEquivalent`, and falls back to a full `.java` file walk (`FileTypeIndex` + `hasAnnotation(全限定名)`) on `RuntimeException|LinkageError`. It never uses `AnnotatedElementsSearch` — that extension point triggers the Kotlin plugin's `KotlinAnnotatedElementsSearcher`, which throws `Cannot find service KaGlobalSearchScopeMerger` when the Analysis API is not ready. No cross-invocation state; each scan call builds a local `path → HttpMappingInfo` index (`FeignMappingResolver.scanFeignIndex`, private) and caches per-module Spring config parsing (`server.servlet.context-path`/`spring.mvc.servlet.path`) in a local map keyed by module root.
  - The current method's own `HttpMappingInfo` is computed directly via `FeignMappingResolver.feignOfPsiMethod(...)` (feign side) and `ControllerMappingResolver.controllerOfPsiMethod(...)` (controller side); matching is pure path comparison.
- The legacy multi-project helpers (`PsiCoreEngineMultiProject.getOpenProjects`/`scanProjectCls`) are `@Deprecated` and unused — don't resurrect them.
- Annotation matching: `AnnotationParserUtils` + `SpringCloudClassAnnotation`/`SpringBootClassAnnotation`/`SpringBootMethodAnnotation` enums. Prefer `PsiMethod.hasAnnotation(...)` over manual string checks.
- URL prefixes (`server.servlet.context-path`, `spring.mvc.servlet.path`) are parsed from `application|bootstrap.{properties,yml,yaml}` **plus profile-specific files** (`application-{profile}.yml/yaml/properties`, active profile resolved from `spring.profiles.active`, with a fallback scan of `application-*.yml` when the value is a Maven placeholder) by `properties/ConfigReader` + `ServerParser` (snakeyaml 1.29 is bundled for this — keep it).

## Gotchas (all learned from past bug fixes in the changelog)

- Every provider must guard with `BizChecker.isBizElement(element)` (excludes jar/library PSI) and `DumbService.isDumb(project)` before touching PSI, and access PSI only inside read actions / EDT. Skipping these caused `PsiInvalidElementAccessException`, `IndexNotReadyException`, and freezes.
- Gutter icons are intentionally anchored to the Restful annotation element, not the method name (keeps gutters stable during Enter/comment edits) — see comment in `F2CLineMarkerProvider`.
- Editing comments (`/** */`) temporarily strips annotations; `HttpMappingInfo.of` then returns null, so `feignOfPsiMethod`/`controllerOfPsiMethod` can return null — matching/gutter callers must tolerate it.
- Do not introduce newer IntelliJ Platform APIs; binary incompatibility with old IDE versions shipped before (v4.1.6 `NoSuchMethodError`). Compile against the 2021.2 SDK.
- Keep the repo's branch/commit style: branch names like `feat/main-*`, `hotfix/main-*`; changelog entries in Chinese.
