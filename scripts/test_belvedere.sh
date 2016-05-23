#!/bin/bash

if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo "This is a branch build, skipping testing"
    exit 0;
fi

export TEST_SDK=true

cat << "EOF"
 ____________
< Unit tests >
 ------------
     \
      \
          oO)-.                       .-(Oo
         /__  _\                     /_  __\
         \  \(  |     ()~()         |  )/  /
          \__|\ |    (-___-)        | /|__/
          '  '--'    ==`-'==        '--'  '
EOF


./gradlew :belvedere:check :belvedere:test --continue

if [ $? -ne 0 ]; then
cat << "EOF"
 ^__^   (UNIT TEST COW)
 (xx)\_____________
 (__)\  belvedere  )\/\
  U  ||--------w- |
     ||          ||

EOF
unset TEST_SDK
exit 1
fi

cat << "EOF"
 _______________________
< Instrumentation tests >
 -----------------------
          \           \  /
           \           \/
               (__)    /\
               (oo)   O  O
               _\/_   //
         *    (    ) //
          \  (\\    //
           \(  \\    )
            (   \\   )   /\
  ___[\______/^^^^^^^\__/) o-)__
 |\__[=======______//________)__\
 \|_______________//____________|
     |||      || //||     |||
     |||      || @.||     |||
      ||      \/  .\/      ||
                 . .
                '.'.`

            COW-OPERATION
EOF

# Wait for the emulator if we are on travis
which android-wait-for-emulator > /dev/null

if [ "$?" -eq 0 ]; then
  echo "Waiting for emulator..."
  android-wait-for-emulator
fi

# Unlock the emulator .......
adb shell input keyevent 82 &

./gradlew :belvedere:connectedAndroidTest

if [ $? -ne 0 ]; then
cat << "EOF"
 ^__^    INSTRUMENT COW
 (xx)\___________
 (__)\ belvedere )\/\
  U  ||--------w |
     ||         ||
EOF
exit 1
unset TEST_SDK
fi