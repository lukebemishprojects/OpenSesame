# OpenSesame

[![Central](https://img.shields.io/badge/maven_central-blue?style=for-the-badge)](https://central.sonatype.com/artifact/dev.lukebemish.opensesame/opensesame-core)
[![javadoc](https://img.shields.io/maven-central/v/dev.lukebemish.opensesame/opensesame-core?style=for-the-badge&label=javadoc%20-%20core&color=green)](https://javadoc.io/doc/dev.lukebemish.opensesame/opensesame-core)
[![Snapshot](https://img.shields.io/github/actions/workflow/status/lukebemishprojects/OpenSesame/snapshot.yml?style=for-the-badge)](https://github.com/lukebemishprojects/OpenSesame/actions/workflows/snapshot.yml)

OpenSesame provides a tool to break through basically any form of encapsulation in the JVM, in a way that allows you to work with simple accessor method in your code, instead of worrying about `MethodHandle`s or reflection. These accessors are turned
into `INVOKEDYNAMIC` instructions at compile time which call a lightweight runtime component; the use of `INVOKEDYNAMIC` allows the runtime dependency to be extremely lightweight, and for the JVM to inline calls to members you are accessing when the
call site is first evaluated, assuring that your code runs as fast as code accessing your target without breaking encapsulation. Normally, OpenSesame simply breaks though access control, but it can be told to break through module boundaries as well.
OpenSesame requires Java 17 or higher.

## Setup

OpenSesame is available on maven central

```gradle
repositories {
    mavenCentral()
}
```

### Groovy

OpenSesame provides a Groovy ASTT. To use, simply add `opensesame-groovy` as a dependency:

```gradle
dependencies {
    implementation 'dev.lukebemish.opensesame:opensesame-groovy:<version>'
}
```

If you do not need the ASTT present at runtime, you may split the dependency and at runtime depend only on `opensesame-core`:

```gradle
dependencies {
    compileOnly 'dev.lukebemish.opensesame:opensesame-groovy:<version>'
    runtimeOnly 'dev.lukebemish.opensesame:opensesame-core:<version>'
}
```

### Java

OpenSesame can be used with Java in several different ways, depending on your use case.

#### Gradle Plugin

Using the gradle plugin, OpenSesame can insert a processing step into the compilation of a source set with ease. This
should have greater compatibility than the javac plugin. To use, simply apply the plugin, and then apply it to the source
set you want to process.

```gradle
plugins {
    id 'dev.lukebemish.opensesame' version '<version>'
}

opensesame.apply(sourceSets.main)

dependencies {
    implementation 'dev.lukebemish.opensesame:opensesame-core:<version>'
}
```

#### Gradle Plugin - Loom Integration

if you are using the [`fabric-loom`](https://github.com/FabricMC/fabric-loom/) plugin, OpenSesame can integrate itself
into the plugin's remapping step, simultaneously processing annotations and remapping provided method, field, or class
names to intermediary, which can be remapped to named mappings at runtime. To use, apply the loom integration plugin and
depend on the `opensesame-fabric` module:

```gradle
plugins {
    id 'fabric-loom' version '<loom-version>'
    id 'dev.lukebemish.opensesame.loom' version '<version>'
}

dependencies {
    modImplementation 'dev.lukebemish.opensesame:opensesame-fabric:<version>'
    include 'dev.lukebemish.opensesame:opensesame-fabric:<version>'
}
```

#### Javac Compiler Plugin

OpenSesame provides a javac plugin. Note that this will not work with eclipse's ecj compiler. To use, simply add the `opensesame-javac` dependency and specify the plugin in the compiler arguments. As there is almost no reason to have the compiler
plugin present at runtime, you will likely want to split it into its runtime and compile time components:

```gradle
dependencies {
    compileOnly 'dev.lukebemish.opensesame:opensesame-javac:<version>'
    runtimeOnly 'dev.lukebemish.opensesame:opensesame-core:<version>'
}

tasks.named('compileJava', JavaCompile).configure {
    options.compilerArgs.add '-Xplugin:OpenSesame'
}
```

## Use

### General Use

The core of OpenSesame is the `@Open` annotation. Applying this annotation to a method tells the compile-time processors to turn that method into an accessor method. This annotation can specify the target class in several ways. The simplest
way is to simply specify the class, if it is publicly accessible to your class:

```java
public class Target {
    private static void someMethod() {
        ...
    }
}

public class Host {
    @Open(
            name = "someMethod",
            targetClass = Target.class,
            type = Open.Type.STATIC
    )
    public static void accessSomeMethod() {
        throw new RuntimeException();
    }
}
```

Calling `Host.accessSomeMethod` will call `Target.someMethod`. If your target class is not accessible, you can use `targetName` to provide a name or descriptor of the class to look for. If even that is not enough to find the class, `targetProvider`
allows you to provide a class to be used to look up the target class at runtime.

The descriptor of the method or field that OpenSesame searches for is determined by the descriptor of your accessor method. If it is impossible to make these two match - say, you are accessing a method with private return or argument types - you
can use `@Coerce` annotation to specify the real type of an argument or return type, while placing in your accessor method a type that that target type can be cast to.

### Groovy

If you are using Groovy, OpenSesame has several useful additional features. The first is `@OpenClass`, which gets rid of the need for accessor methods when accessing public classes:

```groovy
class Target {
    private static void someMethod() {
        ...
    }
}

@CompileStatic
class Host {
    @OpenClass(Target)
    static void someCode() {
        Target.someMethod()
    }
}
```

Note that the use of `@OpenClass` requires `@CompileStatic`, and your IDE may not like it. An additional feature is that the `targetProvider` argument of `@Coerce` or `@Open` can take a closure instead of a class extending `ClassProvider`.
