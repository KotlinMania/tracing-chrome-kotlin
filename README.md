# tracing-chrome-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Ftracing-chrome-kotlin-blue.svg)](https://github.com/KotlinMania/tracing-chrome-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/tracing-chrome-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/tracing-chrome-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/tracing-chrome-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/tracing-chrome-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`thoren-d/tracing-chrome`](https://github.com/thoren-d/tracing-chrome) (crate `tracing-chrome`).

**Original Project:** This port is based on [`thoren-d/tracing-chrome`](https://github.com/thoren-d/tracing-chrome). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `thoren-d/tracing-chrome`

> The text below is reproduced from [`https://github.com/thoren-d/tracing-chrome`](https://github.com/thoren-d/tracing-chrome). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs where appropriate so they continue to resolve from this repository.

tracing-chrome
======

[![Crates.io](https://img.shields.io/crates/v/tracing-chrome)](https://crates.io/crates/tracing-chrome)
[![Documentation](https://docs.rs/tracing-chrome/badge.svg)](https://docs.rs/tracing-chrome/)
![GitHub](https://img.shields.io/github/license/Antigroup/tracing-chrome)
![CI](https://github.com/thoren-d/tracing-chrome/workflows/CI/badge.svg?branch=develop)

# Overview

tracing-chrome is a Layer for [tracing-subscriber](https://crates.io/crates/tracing-subscriber) that outputs traces in Chrome's trace viewer format that can be viewed with `chrome://tracing` or [ui.perfetto.dev](https://ui.perfetto.dev).

# Usage

Add this near the beginning of `main`:
```rust
use tracing_chrome::ChromeLayerBuilder;
use tracing_subscriber::{registry::Registry, prelude::*};

let (chrome_layer, _guard) = ChromeLayerBuilder::new().build();
tracing_subscriber::registry().with(chrome_layer).init();
```

When `_guard` is dropped, your trace will be in a file like `trace-1668480819035032.json`.

Open that file with [ui.perfetto.dev](https://ui.perfetto.dev) (or `chrome://tracing`) and take a look at your pretty trace.

![](https://github.com/thoren-d/tracing-chrome/raw/develop/doc/images/perfetto-screenshot.png)

# License

Licensed under the [MIT license](http://opensource.org/licenses/MIT)

## Contributions

Unless you state otherwise, any contribution intentionally submitted for inclusion in the work shall be licensed as above.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:tracing-chrome-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same license terms as the upstream [`thoren-d/tracing-chrome`](https://github.com/thoren-d/tracing-chrome). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the tracing-chrome authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`thoren-d/tracing-chrome`](https://github.com/thoren-d/tracing-chrome) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
