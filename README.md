# LSS02ConfigurationLib

LSS02ConfigurationLib is a small Java library that sits on top of [SimpleYAML](https://github.com/Carleslc/SimpleYAML)'s `YamlFile` and removes the repetitive parts of working with YAML configuration files: literal path strings scattered across the codebase, default values duplicated at every call site, and configuration files that silently drift out of sync with the code that reads them.

You don't get a new configuration format or a replacement for `YamlFile`. You get a thin abstract class you extend, plus an enum you write yourself to describe every key your configuration has.

The full tutorial, with the same examples used below, is published at [littleshystar02.github.io/LSS02ConfigurationLib-Wiki](https://littleshystar02.github.io/LSS02ConfigurationLib-Wiki/). The site is dark mode only for now, it doesn't follow your system theme.

## How it works

You define an enum that implements `ConfigKey`. Each constant carries the YAML path it maps to and the value that should be returned when that path is missing from the file.

```java
public enum AppKeys implements ConfigKey {

    SERVER_NAME("server.name", "My Server"),
    MAX_PLAYERS("server.max-players", 20),
    MOTD_LINES("server.motd", List.of("Welcome!"));

    private final String path;
    private final Object defaultValue;

    AppKeys(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
```

Then you extend `AbstractConfiguration`, which itself extends `YamlFile`, so every method `YamlFile` already gives you (`load()`, `save()`, `set(String, Object)`, `getString(String)` and so on) is still there, untouched.

```java
public class AppConfig extends AbstractConfiguration {

    public AppConfig(File file) {
        super(file);
    }
}
```

```java
AppConfig config = new AppConfig(new File(dataFolder, "config.yml"));
config.load();

String name = config.getString(AppKeys.SERVER_NAME);
int maxPlayers = config.getInt(AppKeys.MAX_PLAYERS);
List<String> motd = config.getStringList(AppKeys.MOTD_LINES);

config.set(AppKeys.MAX_PLAYERS, 32);
config.save();
```

If `server.max-players` isn't in the file yet, `getInt(AppKeys.MAX_PLAYERS)` returns `20` without throwing anything. A missing key never throws: the default is returned, and `ConfigKey` just gives that default a name you can reuse everywhere instead of retyping it. The only exception is a key whose default is `null` read through a primitive getter, which fails with an `IllegalStateException` that names the path (see "Defaults and their types" below).

Every typed getter you'd expect on `ConfigurationSection` has a `ConfigKey` overload: `getString`, `getInt`, `getBoolean`, `getDouble`, `getLong`, `getFloat`, `getByte`, `getShort`, `getCharacter`, the untyped `get`, and the list variants of each. The string-based methods you already know from `YamlFile` keep working exactly as before, nothing is overridden.

`addDefault(ConfigKey)` registers the key's default in `YamlFile`'s own defaults section, so even a plain `getInt("server.max-players")` call without a second argument benefits from it.

### Defaults and their types

An enum constant cannot carry a type parameter, so the legacy getters rely on the default having the right runtime type. The failures are explicit and name the key path: a default of the wrong type throws a `ClassCastException`, and a `null` default read through a primitive getter (`getInt`, `getBoolean`, ...) throws an `IllegalStateException`. Numeric defaults are converted, so an `Integer` default read with `getLong`, `getDouble`, `getFloat` or `getShort` works.

### Compile-time typed keys

For keys whose type you want checked by the compiler, declare `TypedKey` constants in a holder class and read them with `get(TypedKey)`:

```java
public final class AppKeys2 {
    public static final TypedKey<Integer> PORT = TypedKey.of("server.port", Integer.class, 25565)
            .withValidator(p -> p > 0 && p < 65536);
    public static final TypedKey<String> NAME = TypedKey.of("server.name", String.class, "My Server");
}

int port = config.get(AppKeys2.PORT);
```

`TypedKey` implements `ConfigKey`, so everything above keeps working. Numbers are converted when lossless; a value of the wrong type (or one rejected by the validator) is reported with its path instead of being silently replaced.

### Comments and reload

`YamlFile#load()` does not keep the comments of the file; use `loadWithComments()` before `save()` when users edit a commented template, otherwise the first `save()` drops them. Defaults registered with `addDefault` stay registered across a new `load()`.

`AbstractConfiguration` is mutable and not thread-safe, and reloading it in place can expose half-loaded content to concurrent readers. For plugins that read from async tasks, `ConfigHolder` builds a brand new instance, validates it, and swaps it in with a single volatile write; if loading or validation fails, the previous snapshot stays in place.

```java
ConfigHolder<AppConfig> holder = new ConfigHolder<>(
        () -> AppConfig.loadFrom(file), // your factory: new AppConfig(file), c.load(), wrap checked exceptions
        c -> c.validate(AppKeys.class));

holder.get().getInt(AppKeys.MAX_PLAYERS); // always a complete snapshot
holder.reload();                          // on /reload
```

Treat published snapshots as read-only.

### Contract with SimpleYAML

`AbstractConfiguration` intentionally extends `YamlFile`: the whole SimpleYAML API is part of the public surface, and the supported SimpleYAML range is the one this library is built against (currently 1.8.5). Validation is the only part that does not depend on it: `ConfigKeyValidator` works on the small `ConfigView` interface.

## Catching drift between the enum and the file

Once you have an enum and a real file, you can ask the library to check that the two agree.

```java
config.load();
config.validate(AppKeys.class);
```

`validate` walks every leaf key actually present in the loaded file and fails with a `ConfigValidationException` if it finds a path with no matching enum constant, or two enum constants pointing at the same path. It does not complain about an enum constant that isn't in the file yet: that's exactly the case the default value exists for, not an error.

That asymmetry leaves one gap open on purpose: a typo inside the enum itself, one that doesn't match anything in the file, will never be flagged this way, because the file simply won't contain an orphan for it. `validateTemplate` closes that gap by checking the *default* YAML resource you ship inside your jar, in both directions: an unexpected path in the template is still an error, and now so is an enum constant that the template doesn't define either.

```java
try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
    YamlFile template = new YamlFile();
    template.load(in);
    ConfigKeyValidator.validateTemplate(template, AppKeys.class); // run this in a test, against the packaged default
}
```

`validate` and `validateTemplate` only compare paths. To also catch a wrong value type or an out-of-range value in the user's file (for example `server.max-players: "abc"`), call `config.validateValues(AppKeys.class)`: each present key must hold a value that `ConfigKey#accepts(Object)` approves (by default, a value that fits the type of the default).

If a key stands for a whole section with free children (for example `worlds`, keyed by world name), override `isOpenSection()` to return `true` on that constant: every path below it is then treated as known instead of being reported as an orphan.

Run `validate` against the user's live file at startup, and `validateTemplate` against your bundled default file in a unit test, and a path typo in the enum has nowhere left to hide.

## Extracting the bundled default file

Standalone applications and Minecraft plugins load their jar's classpath resources differently. `ClassLoaderResourceExtractor`, in `config-core`, copies a classpath resource to a target file using plain `ClassLoader#getResourceAsStream`, the same way regardless of platform, and does nothing if the target already exists. The copy goes through a temporary file in the same directory and is moved into place without replacing, so an interrupted extraction never leaves a truncated file and concurrent extractions are safe.

```java
File target = new File(dataFolder, "config.yml");
new ClassLoaderResourceExtractor(getClass().getClassLoader()).extract("config.yml", target);
```

## Bukkit and Paper

The `config-bukkit` module adds `BukkitResourceExtractor`, which prefers `JavaPlugin#saveResource` when the requested target is the plugin's own data folder, since that's the native mechanism Bukkit already provides, and falls back to `ClassLoaderResourceExtractor` for anything else. Like the fallback, it leaves a target file that already exists untouched, so `saveResource` is never called for it and Paper does not log an "already exists" warning at every startup.

```java
File target = new File(plugin.getDataFolder(), "config.yml");
new BukkitResourceExtractor(plugin).extract("config.yml", target);
```

`config-bukkit` targets Java 21, matching Paper's current stable line. `config-core` only requires Java 17 and has no dependency on any server API, so it works the same in a standalone application, a Bukkit plugin, or anywhere else on the classpath.

## Thread-safety

`AbstractConfiguration` adds no synchronization of its own. It extends `YamlFile`, and `YamlFile` isn't thread-safe either, so touching the same configuration instance from more than one thread at a time needs to be synchronized by the code calling into it. The supported pattern is the immutable snapshot: use `ConfigHolder` (see above) and never mutate a published instance.

## Modules and requirements

| Module | Requires | Depends on |
|---|---|---|
| `config-core` | Java 17 | SimpleYAML |
| `config-bukkit` | Java 21 | `config-core`, Paper API |

## Adding it to your build

Both modules are published to Maven Central under the group `io.github.littleshystar02`, so no extra repository is needed. `config-core` pulls in SimpleYAML, which is published only on JitPack: add `maven("https://jitpack.io")` to your repositories.

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.github.littleshystar02:config-core:0.1.3-beta.3")
    // implementation("io.github.littleshystar02:config-bukkit:0.1.3-beta.3")
}
```

Restrict JitPack to SimpleYAML's group so no other dependency can be resolved from it: in Gradle use `exclusiveContent { forRepository { maven("https://jitpack.io") }; filter { includeGroupAndSubgroups("com.github.Carleslc") } }` instead of a bare `maven("https://jitpack.io")`.

To build the current source instead of the published jar, clone the repository and publish it to your local Maven repository, then add `mavenLocal()` before `mavenCentral()`:

```bash
git clone https://github.com/LittleShyStar02/LSS02Libraries-ConfigurationLib.git
cd LSS02Libraries-ConfigurationLib
./gradlew publishToMavenLocal
```

## Building, testing and releasing

- Build and test: `./gradlew build` (Gradle wrapper 9.7.1). The modules use Gradle toolchains, so JDK 17 (`config-core`) and JDK 21 (`config-bukkit`) must be available; the foojay resolver plugin downloads a missing one. `build` also runs JaCoCo (minimum 80% line coverage per module), `japicmpCheck` (binary compatibility of `config-core` against the last published release) and compiles with `-Xlint:all -Werror`. Versions live in `gradle/libs.versions.toml`, dependency checksums in `gradle/verification-metadata.xml` (regenerate with `./gradlew --write-verification-metadata sha256 build` after changing a dependency). The GitHub Action `.github/workflows/build.yml` runs the build on every push and pull request to `main`, and Dependabot proposes dependency updates.
- Versions: the library version is the `version` in `gradle.properties` (semantic versioning, currently `0.1.3-beta.3`); each release is a git tag `v<version>`. Pushing a `v*` tag runs `.github/workflows/release.yml`, which uploads the signed artifacts to the Central Portal using repository secrets; the deployment is then released manually from the Portal. Publishing locally uses the same `com.vanniktech.maven.publish` plugin (`publishToMavenCentral`) and needs the owner's signing key and Portal credentials, which are never stored in the repository. Commits, pushes and tags are done by the owner.
- Related repositories: [LSS02Libraries-DatabasesLib](https://github.com/LittleShyStar02/LSS02Libraries-DatabasesLib) (builds on this library), `LSS02PluginLib-MultiLanguageSystem` (plugin that uses both) and `AetheriaMC-WebIntegration` (uses `config-core` and `db-core`).

## License

LSS02ConfigurationLib is licensed under the [GNU General Public License v3.0](LICENSE.md) (`GPL-3.0-only`, declared in every source file with an SPDX header). You're free to use, modify and share it, including commercially, as long as any distributed derivative work is released under the same license with its source code available. The choice of a copyleft license for a library is deliberate and was reconfirmed in the 0.1.3 line; consumers inside the NeoGalaxy ecosystem are GPL-compatible.
