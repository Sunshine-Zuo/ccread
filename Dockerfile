FROM gradle:8.13-jdk21

USER root
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
 && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" \
 && curl -fsSL -o /tmp/cmdline-tools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
 && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools" \
 && mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest" \
 && yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses \
 && "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" "platforms;android-36" "build-tools;36.0.0" \
 && rm /tmp/cmdline-tools.zip

ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"
ENV GRADLE_USER_HOME=/root/.gradle
WORKDIR /project
