#!/usr/bin/env bash

# common stuff
isPullRequest() {
    [[ "$TRAVIS_PULL_REQUEST" == "false" ]] && return 1 || return 0
}

exitOnFailedBuild() {
    if [ $? -ne 0 ]; then
        exit 1
    fi
}

boxOut(){
    local s="$*"
    tput setaf 3
    echo -e " =${s//?/=}=\n| $(tput setaf 4)$s$(tput setaf 3) |\n =${s//?/=}=\n"
    tput sgr 0
}

acceptLicenses() {
    mkdir -p ${ANDROID_HOME}licenses
    echo -e "\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > ${ANDROID_HOME}licenses/android-sdk-license
}

buildBelvedere() {
    ./gradlew :belvedere-core:assembleRelease :belvedere:assembleRelease
    exitOnFailedBuild
}

testBelvedere() {
    ./gradlew clean :belvedere-core:lintRelease :belvedere:lintRelease :belvedere-core:testRelease :belvedere:testRelease
    exitOnFailedBuild
}

uploadSnapshotSdk() {
    export LOCAL_BUILD="false"

    ./gradlew --settings-file scripts/gradle/build-belvedere-core.gradle -Pandroid.useAndroidX=true clean :belvedere-core:uploadArchive
    exitOnFailedBuild

    ./gradlew --settings-file scripts/gradle/build-belvedere.gradle -Pandroid.useAndroidX=true clean :belvedere:uploadArchive
    exitOnFailedBuild

    buildSampleApp
    exitOnFailedBuild

    unset LOCAL_BUILD
}

# SampleApp specific stuff
buildSampleApp() {
    ./gradlew :belvedere-sample:assembleTravis
    exitOnFailedBuild
}

uploadSampleApp() {
    ./gradlew :SampleApp:uploadDebugToHockeyApp
    exitOnFailedBuild
}

# Build types
pullRequestBuild() {
    export LOCAL_BUILD="true"

    boxOut "Build Belvedere Core SDK"
    buildBelvedere

    boxOut "Test Belvedere"
    testBelvedere

    boxOut "Build Sample App"
    buildSampleApp

    unset LOCAL_BUILD
}

branchBuild() {
    pullRequestBuild

    boxOut "Upload Belvedere Snapshots"
    uploadSnapshotSdk
}

acceptLicenses

# do the thing
if isPullRequest ; then
    boxOut "This is a PR"
    pullRequestBuild
else
    boxOut "This is not a PR"
    branchBuild
fi