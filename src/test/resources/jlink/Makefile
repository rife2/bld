.PHONY: clean
clean:
	rm -rf build

.PHONY: compile
compile: clean
	javac -d build/javac --module-source-path "./*/src" --module dev.mccue.tree,dev.mccue.apple

.PHONY: package
package: compile
	mkdir -p build/jar

	jar --create --file build/jar/dev.mccue.apple.jar \
		-C build/javac/dev.mccue.apple . \
		-C dev.mccue.apple/res .
	jar --create --file build/jar/dev.mccue.tree.jar \
		-C build/javac/dev.mccue.tree .

.PHONY: link
link: package
	mkdir -p build/jmod

	jmod create \
		--legal-notices dev.mccue.apple/legal \
		--class-path build/jar/dev.mccue.apple.jar \
		build/jmod/dev.mccue.apple.jmod

	jmod create \
		--class-path build/jar/dev.mccue.tree.jar \
		build/jmod/dev.mccue.tree.jmod

	jlink \
		--module-path build/jmod \
		--add-modules dev.mccue.tree \
		--launcher tree=dev.mccue.tree/dev.mccue.tree.Tree \
		--output build/jlink

.PHONY: test
test: link
	./build/jlink/bin/tree
