% Migration of JDK Classes to Value Classes

## Introduction

The "Value Objects" JEP introduces value objects and migrates suitable classes
to value classes. This means that when preview features are enabled, a different
classpath is available to Java programs. To accomplish this, the JDK has a
custom build and deployment process to allow access to different classpaths.

## The Build Process

The build system has a fully automated pipeline from preview-only source files
for each module to preview-ready image binaries.  The source files are currently
hardcoded in `GENERATED_PREVIEW_SUBDIRS` variable in
[`make/common/Modules.gmk`](../make/common/Modules.gmk).

### Hardcoding for Value Objects JEP

For the Value Objects JEP, for each module, the preview source files are in
`$(SUPPORT_OUTPUTDIR)/gensrc-valueclasses/<module>/` directory. However, only
the module `java.base` actually makes use of this directory, created by
[`GensrcValueClasses.gmk`](../make/modules/java.base/gensrc/GensrcValueClasses.gmk).
This processor simply looks for occurrences of `/*value*/ class` and
`/*value*/ record` in the hardcoded list of source files, and generate preview
source files where these occurrences are replaced by `value class` and
`value record`, respectively.

The Value Objects JEP does not intend to introduce value classes that are not
migrated from a class that is available when preview features are disabled, nor
does it intend to introduce migrations from modules other than `java.base`. The
preview source file setup would require substantial changes to accomplish these
goals.

### Preview Classes in Interim `javac`

The preview classpath files are not available to the interim `javac`, most
commonly used in `SetupJavaCompilation` call. For each module that ships
preview-specific binaries, the following javac flag is required for a
preview-enabled compilation:

```
--patch-module <module>=$(SUPPORT_OUTPUTDIR)/preview/<module>
```

See [`BuildMicroBenchmarks.gmk`](../make/test/BuildMicrobenchmark.gmk) for an example.

## Testing

In addition to tests that require preview features to be enabled, tests that do
not depend on preview features wish to run with preview features enabled to
ensure compatibility:

1. Some tests wish to run against the preview system classpath.

   The jtreg tests may be run with `JTREG=VM_OPTIONS=--enable-preview`.

2. Some tests wish to run against their own classes migrated to value classes.

   The jtreg [VALUE_CLASS_PLUGIN](testing.html#VALUE_CLASS_PLUGIN) allows tests
   to migrate their own classes to value classes when running with the plugin.

## Wrapper Class Caches

Currently, wrapper class caches are retained even when preview features are
enabled. They are created with `ValueClass.newReferenceArray`, which creates
a non-flat array storing object references, allowing a value object wrapping
a primitive value to be available quickly without redundant allocations and
associated performance regressions. Their existence have no semantic meaning,
and they may be removed when the performance regressions are eliminated or
reduced.

## Editing This Document

If you want to contribute changes to this document, edit `doc/value-class-preview.md`
and then run `make update-build-docs` to generate the same changes in
`doc/value-class-preview.html`.

---
# Override styles from the base CSS file that are not ideal for this document.
header-includes:
 - '<style type="text/css">pre, code, tt { color: #1d6ae5; }</style>'
---
