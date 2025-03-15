$env:PATH = $env:USERPROFILE + '\.jdks\graalvm-ce-23.0.2\bin' + $env:PATH
$env:GRAALVM_HOME = $env:USERPROFILE + '\.jdks\graalvm-ce-23.0.2'
$env:JAVA_HOME = $env:USERPROFILE + '\.jdks\graalvm-ce-23.0.2'

../mvnw clean package -Pnative -DskipTests=true -DspringJavaFormatSkip=true -T 1 -pl :unleash-advanced -am
./unleash-advanced/target/unleash-advanced.exe -Dnative.encoding=UTF-8
