# Spring Boot OpenFeature — agent-facing Maven targets
#
# Usage: make <target> [MODULE=<module-dir>] [TEST=<TestClass>]
#
# NOTE: Requires JDK 17. The build auto-applies Spring Java Format and runs
# Checkstyle during the validate phase; JaCoCo runs during verify.

MVN := ./mvnw

.DEFAULT_GOAL := help

# --- Build -----------------------------------------------------------------

.PHONY: build
build: ## Full build (compile + format + checkstyle + test + jacoco)
	$(MVN) clean verify

.PHONY: build-skip-tests
build-skip-tests: ## Full build without tests
	$(MVN) clean verify -DskipTests

.PHONY: compile
compile: ## Quick compile only (no tests, no format/checkstyle)
	$(MVN) compile

.PHONY: build-module
build-module: ## Build a single module with its dependencies. Requires MODULE.
	@test -n "$(MODULE)" || { echo "Usage: make build-module MODULE=<module-dir>"; exit 1; }
	$(MVN) -pl $(MODULE) -am clean verify

# --- Test --------------------------------------------------------------------

.PHONY: test
test: ## Run all tests
	$(MVN) test

.PHONY: test-module
test-module: ## Run tests for a single module. Requires MODULE.
	@test -n "$(MODULE)" || { echo "Usage: make test-module MODULE=<module-dir>"; exit 1; }
	$(MVN) -pl $(MODULE) -am test

.PHONY: test-class
test-class: ## Run a single test class. Requires MODULE and TEST.
	@test -n "$(MODULE)" -a -n "$(TEST)" || { echo "Usage: make test-class MODULE=<module-dir> TEST=<TestClass>"; exit 1; }
	$(MVN) -pl $(MODULE) -am test -Dtest=$(TEST)

# --- Quality -----------------------------------------------------------------

.PHONY: format
format: ## Apply Spring Java Format (in-place)
	$(MVN) spring-javaformat:apply

.PHONY: lint
checkstyle: ## Run Checkstyle
	$(MVN) checkstyle:check

.PHONY: checkstyle
checkstyle: ## Run Checkstyle
	$(MVN) checkstyle:check

.PHONY: coverage
coverage: ## Build with aggregate JaCoCo coverage report (sonar profile)
	$(MVN) clean verify -P sonar

# --- Housekeeping -------------------------------------------------------------

.PHONY: clean
clean: ## Remove build artifacts
	$(MVN) clean

.PHONY: install
install: ## Install all modules to the local Maven repository
	$(MVN) clean install

.PHONY: versions
versions: ## Show available dependency and plugin updates
	$(MVN) versions:display-dependency-updates

# --- Help ----------------------------------------------------------------------

.PHONY: help
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
