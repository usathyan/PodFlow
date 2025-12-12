# AntennaPod Development Makefile
# Provides convenient commands for building, testing, and running the app

SHELL := /bin/bash
.DEFAULT_GOAL := help

# Environment variables
export JAVA_HOME := /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_SDK_ROOT := /usr/local/share/android-commandlinetools
export ANDROID_HOME := $(ANDROID_SDK_ROOT)
export PATH := $(JAVA_HOME)/bin:$(ANDROID_SDK_ROOT)/platform-tools:$(ANDROID_SDK_ROOT)/emulator:$(PATH)

# Python/uv configuration
VENV_DIR := .venv
PYTHON := $(VENV_DIR)/bin/python
UV := uv

# AVD configuration
AVD_NAME := Pixel_6_API_35
SYSTEM_IMAGE := system-images;android-35;google_apis;x86_64

# Gradle wrapper
GRADLEW := ./gradlew

.PHONY: help
help: ## Show this help message
	@echo "AntennaPod Development Commands"
	@echo "================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ============================================
# Environment Setup
# ============================================

.PHONY: setup
setup: venv sdk-setup avd-create ## Complete setup: Python venv, SDK, and emulator
	@echo "✅ Setup complete! Run 'make build' to build the app."

.PHONY: venv
venv: ## Create Python virtual environment with uv
	@echo "Creating Python virtual environment with uv..."
	$(UV) venv $(VENV_DIR)
	$(UV) pip install --python $(PYTHON) pytest requests
	@echo "✅ Virtual environment created at $(VENV_DIR)"

.PHONY: sdk-setup
sdk-setup: ## Install/update Android SDK components
	@echo "Setting up Android SDK..."
	@echo "y" | sdkmanager --sdk_root="$(ANDROID_SDK_ROOT)" \
		"platform-tools" \
		"platforms;android-34" \
		"build-tools;34.0.0" \
		"emulator" \
		"$(SYSTEM_IMAGE)" || true
	@echo "✅ SDK setup complete"

.PHONY: sdk-licenses
sdk-licenses: ## Accept all Android SDK licenses
	@echo "y" | sdkmanager --sdk_root="$(ANDROID_SDK_ROOT)" --licenses || true

.PHONY: avd-create
avd-create: ## Create Android Virtual Device (use 'make studio-avd' for GUI)
	@echo "Creating AVD: $(AVD_NAME)..."
	@echo ""
	@echo "⚠️  For best results, create the emulator via Android Studio:"
	@echo "   1. Run: make studio"
	@echo "   2. Go to: Tools > Device Manager"
	@echo "   3. Click 'Create Virtual Device'"
	@echo "   4. Select 'Pixel 6' > Next"
	@echo "   5. Select 'VanillaIceCream' (API 35) > Download if needed > Next"
	@echo "   6. Name it '$(AVD_NAME)' > Finish"
	@echo ""
	@echo "Alternatively, trying command line creation..."
	JAVA_HOME="$(JAVA_HOME)" avdmanager create avd -n "$(AVD_NAME)" -k "$(SYSTEM_IMAGE)" -d "pixel_6" --force 2>/dev/null || true

.PHONY: avd-list
avd-list: ## List available AVDs
	avdmanager list avd

# ============================================
# Build Commands
# ============================================

.PHONY: build
build: ## Build debug APK
	@echo "Building debug APK..."
	$(GRADLEW) assembleDebug
	@echo "✅ APK built at: app/build/outputs/apk/free/debug/"

.PHONY: build-play
build-play: ## Build Play Store variant (debug)
	$(GRADLEW) assemblePlayDebug

.PHONY: build-release
build-release: ## Build release APK (unsigned)
	$(GRADLEW) assembleRelease

.PHONY: clean
clean: ## Clean build outputs
	$(GRADLEW) clean
	@echo "✅ Build cleaned"

.PHONY: clean-all
clean-all: clean ## Clean everything including caches
	rm -rf .gradle build
	rm -rf $(VENV_DIR)
	@echo "✅ All build artifacts and caches cleaned"

# ============================================
# Testing Commands
# ============================================

.PHONY: test
test: ## Run unit tests
	@echo "Running unit tests..."
	$(GRADLEW) testPlayDebugUnitTest
	@echo "✅ Unit tests complete"

.PHONY: test-all
test-all: ## Run all unit tests
	$(GRADLEW) test

.PHONY: test-integration
test-integration: ## Run integration tests (requires running emulator)
	@echo "Running integration tests..."
	@echo "Make sure emulator is running: make emulator"
	sh .github/workflows/runTests.sh

.PHONY: lint
lint: ## Run lint checks
	$(GRADLEW) lintPlayDebug

.PHONY: checkstyle
checkstyle: ## Run checkstyle
	$(GRADLEW) checkstyle spotbugsPlayDebug spotbugsDebug :app:lintPlayDebug

.PHONY: check
check: checkstyle lint test ## Run all checks (checkstyle, lint, tests)

# ============================================
# Run Commands
# ============================================

.PHONY: emulator
emulator: ## Start Android emulator
	@echo "Starting emulator: $(AVD_NAME)..."
	@echo "If this fails, open Android Studio and create an emulator via AVD Manager"
	$(ANDROID_SDK_ROOT)/emulator/emulator -avd $(AVD_NAME) &
	@echo "Waiting for emulator to boot..."
	adb wait-for-device
	@echo "✅ Emulator started"

.PHONY: emulator-list
emulator-list: ## List available emulators
	$(ANDROID_SDK_ROOT)/emulator/emulator -list-avds

.PHONY: install
install: build ## Build and install debug APK on connected device/emulator
	@echo "Installing APK..."
	adb install -r app/build/outputs/apk/free/debug/app-free-debug.apk
	@echo "✅ APK installed"

.PHONY: run
run: install ## Build, install, and launch the app
	@echo "Launching AntennaPod..."
	adb shell am start -n de.danoeh.antennapod.debug/de.danoeh.antennapod.activity.MainActivity
	@echo "✅ App launched"

.PHONY: uninstall
uninstall: ## Uninstall app from device/emulator
	adb uninstall de.danoeh.antennapod.debug || true
	@echo "✅ App uninstalled"

# ============================================
# Device Commands
# ============================================

.PHONY: devices
devices: ## List connected devices/emulators
	adb devices -l

.PHONY: logcat
logcat: ## Show app logs
	adb logcat -s AntennaPod:* ActivityManager:I *:E

.PHONY: logcat-all
logcat-all: ## Show all device logs
	adb logcat

.PHONY: screenshot
screenshot: ## Take a screenshot from device
	@mkdir -p screenshots
	adb exec-out screencap -p > screenshots/screenshot_$$(date +%Y%m%d_%H%M%S).png
	@echo "✅ Screenshot saved to screenshots/"

# ============================================
# Android Studio
# ============================================

.PHONY: studio
studio: ## Open project in Android Studio
	open -a "Android Studio" .

.PHONY: studio-avd
studio-avd: ## Open AVD Manager in Android Studio (for creating emulators)
	@echo "Opening Android Studio AVD Manager..."
	@echo "Go to: Tools > Device Manager > Create Virtual Device"
	open -a "Android Studio" .

# ============================================
# Git/Development Helpers
# ============================================

.PHONY: fork-setup
fork-setup: ## Set up your fork as origin (run after forking on GitHub)
	@read -p "Enter your GitHub username: " username; \
	git remote rename origin upstream; \
	git remote add origin git@github.com:$$username/AntennaPod.git; \
	echo "✅ Remotes configured:"; \
	git remote -v

.PHONY: sync-upstream
sync-upstream: ## Sync with upstream repository
	git fetch upstream
	git checkout develop
	git merge upstream/develop
	@echo "✅ Synced with upstream"

.PHONY: branch
branch: ## Create a new feature branch (usage: make branch name=feature-name)
	@if [ -z "$(name)" ]; then \
		echo "Usage: make branch name=your-feature-name"; \
		exit 1; \
	fi
	git checkout develop
	git pull upstream develop
	git checkout -b $(name)
	@echo "✅ Created and switched to branch: $(name)"

# ============================================
# Info Commands
# ============================================

.PHONY: info
info: ## Show environment info
	@echo "=== Environment Info ==="
	@echo "JAVA_HOME: $(JAVA_HOME)"
	@echo "ANDROID_SDK_ROOT: $(ANDROID_SDK_ROOT)"
	@echo ""
	@echo "=== Java Version ==="
	@java -version 2>&1 | head -1
	@echo ""
	@echo "=== Gradle Version ==="
	@$(GRADLEW) --version 2>/dev/null | grep "Gradle" || echo "Run 'make build' to download Gradle"
	@echo ""
	@echo "=== Connected Devices ==="
	@adb devices 2>/dev/null || echo "ADB not available or no devices connected"

.PHONY: deps
deps: ## Show project dependencies
	$(GRADLEW) dependencies --configuration implementation
