#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root/backend"
./mvnw -B test-compile dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/browser-classpath.txt
java_command=java
if [[ -n "${JAVA_HOME:-}" ]]; then java_command="$JAVA_HOME/bin/java"; fi
exec "$java_command" -Dfindmyclass.browserTests=true -cp "target/test-classes:target/classes:$(cat target/browser-classpath.txt)" com.findmyclass.BrowserTestServer
