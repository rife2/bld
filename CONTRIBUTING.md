# Contributing

If you want to contribute to `bld` or customize it, all you have to do is clone the GitHub
repository and update the [RIFE2/core](https://github.com/rife2/rife2-core) submodule:

```console
git clone git@github.com:rife2/bld.git
cd bld
git submodule init
git submodule update
```

Then use `bld` to build itself:

```console
./bld compile
```

The project has an IntelliJ IDEA project structure. You can just open it after all
the dependencies were downloaded and peruse the code.
