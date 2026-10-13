param(
    [ValidateSet('build','generate','test','test-original','test-generated','test-final','original','generated','final','all')]
    [string]$Stage = 'all',
    [string]$Maven = 'mvn',
    [string]$Python = 'python'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
New-Item -ItemType Directory -Force evidence | Out-Null
function Invoke-Maven([string[]]$Arguments, [string]$Log) {
    & $Maven @Arguments *> $Log
    $code = $LASTEXITCODE
    Get-Content -LiteralPath $Log -Tail 12
    if ($code -ne 0) { throw "Maven failed ($code). See $Log" }
}
switch ($Stage) {
    'build' {
        Invoke-Maven @('-B','-ntp','-Pfast','-pl','tika-core','-am','install','-DskipTests') 'evidence/local-build.log'
    }
    'generate' {
        foreach ($method in @('close','markSupported','read')) {
            Invoke-Maven @('-B','-ntp','-f','tika-core/pom.xml','-Ptache2-generation',"-DselectMethod=org.apache.tika.io.LookaheadInputStream#$method",'io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:method') "evidence/chatunitest-$method.log"
        }
    }
    { $_ -in @('test','test-original','test-generated','test-final') } {
        $suite = $Stage.Replace('test-','')
        if ($suite -eq 'test') { $suite = 'final' }
        $selector = '*'
        if ($suite -eq 'original') { $selector = '*,!LookaheadInputStream_*_Test,!LookaheadInputStreamManualTest' }
        if ($suite -eq 'generated') { $selector = '*,!LookaheadInputStreamManualTest' }
        foreach ($file in @('tika-core/target/jacoco.exec')) {
            if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
        }
        Get-ChildItem -LiteralPath 'tika-core/target/surefire-reports' -File -ErrorAction SilentlyContinue | Remove-Item
        Invoke-Maven @('-B','-ntp','-Pfast','-DskipTests=false',"-Dtest=$selector",'-Dsurefire.failIfNoSpecifiedTests=false','-pl','tika-core','-am','test') "evidence/$suite-suite.log"
        $coverage = "evidence/coverage-$suite"
        $surefire = "evidence/surefire-$suite"
        New-Item -ItemType Directory -Force $coverage,$surefire | Out-Null
        Copy-Item 'tika-core/target/site/jacoco/*' $coverage -Recurse -Force
        Copy-Item 'tika-core/target/surefire-reports/*' $surefire -Force
    }
    { $_ -in @('original','generated','final') } {
        $selector = 'org.apache.tika.io.LookaheadInputStreamTest'
        $arguments = @('-B','-ntp','-f','tika-core/pom.xml','-Ptache2-mutation')
        if ($Stage -ne 'original') { $selector = 'org.apache.tika.io.LookaheadInputStream*' }
        if ($Stage -eq 'generated') { $arguments += '-Dtache2.excludedTestClasses=org.apache.tika.io.LookaheadInputStreamManualTest' }
        if ($Stage -eq 'final') { $arguments += '-Dtache2.mutationThreshold=100' }
        $arguments += @("-Dtache2.targetTests=$selector", "-Dtache2.reportDirectory=$root/evidence/pit-$Stage",'org.pitest:pitest-maven:mutationCoverage')
        Invoke-Maven $arguments "evidence/pit-$Stage.log"
    }
    'all' {
        & $PSCommandPath build -Maven $Maven -Python $Python
        foreach ($suite in @('original','generated','final')) {
            & $PSCommandPath "test-$suite" -Maven $Maven -Python $Python
            & $PSCommandPath $suite -Maven $Maven -Python $Python
        }
        & $Python scripts/summarize-mutations.py
        if ($LASTEXITCODE -ne 0) { throw 'Mutation comparison failed' }
    }
}
