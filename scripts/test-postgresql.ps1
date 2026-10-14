param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string[]]$MavenArgs = @('test')
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
foreach ($binary in @('initdb.exe', 'pg_ctl.exe', 'createdb.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $PostgresBin $binary))) {
        throw "PostgreSQL não encontrado em $PostgresBin. Informe -PostgresBin."
    }
}

# Diretório exclusivo desta execução, ignorado pelo Git e sem ligação com o serviço instalado.
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('sgp-postgres-test-' + [guid]::NewGuid().ToString('N'))
$clusterPath = Join-Path $tempRoot 'data'
$passwordPath = Join-Path $tempRoot 'password.txt'
$savedEnvironment = @{}
foreach ($name in @('PGPASSWORD', 'TEST_DATABASE_URL', 'TEST_DATABASE_USERNAME', 'TEST_DATABASE_PASSWORD')) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$initialized = $false
$stopped = $false
$buildExitCode = 1
try {
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    $password = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    [IO.File]::WriteAllText($passwordPath, $password, [Text.UTF8Encoding]::new($false))
    & (Join-Path $PostgresBin 'initdb.exe') -D $clusterPath -U sgp_test_owner --encoding=UTF8 --locale=C --auth=scram-sha-256 "--pwfile=$passwordPath"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar o cluster temporário.' }
    $initialized = $true
    Remove-Item -LiteralPath $passwordPath

    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $listener.Stop()
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $clusterPath -l (Join-Path $tempRoot 'postgres.log') -o "-h 127.0.0.1 -p $port" -w start
    if ($LASTEXITCODE -ne 0) { throw "Falha ao iniciar PostgreSQL temporário. Log: $tempRoot\postgres.log" }

    $env:PGPASSWORD = $password
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $port -U sgp_test_owner -w sgp_test
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar a base sgp_test temporária.' }
    $env:TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:$port/sgp_test"
    $env:TEST_DATABASE_USERNAME = 'sgp_test_owner'
    $env:TEST_DATABASE_PASSWORD = $password
    Write-Host "Testando com PostgreSQL temporário em 127.0.0.1:$port/sgp_test"
    Push-Location $repoRoot
    try {
        & .\mvnw.cmd -B @MavenArgs
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    if ($initialized) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $clusterPath -m fast -w stop
        $stopped = $LASTEXITCODE -eq 0
        if (-not $stopped) {
            & (Join-Path $PostgresBin 'pg_ctl.exe') -D $clusterPath status
            $stopped = $LASTEXITCODE -eq 3
        }
    } else {
        $stopped = $true
    }
    foreach ($name in $savedEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
    if ($stopped -and (Test-Path -LiteralPath $tempRoot)) {
        $resolved = (Resolve-Path -LiteralPath $tempRoot).Path
        $expected = [IO.Path]::GetFullPath($tempRoot)
        $parent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')
        if ($resolved -ne $expected -or (Split-Path $resolved -Parent) -ne $parent -or
                (Split-Path $resolved -Leaf) -notmatch '^sgp-postgres-test-[a-f0-9]{32}$') {
            throw 'Limpeza recusada: diretório temporário fora do destino esperado.'
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    } elseif (-not $stopped) {
        Write-Warning "O cluster pode continuar ativo. Diretório preservado: $tempRoot"
    }
}
if ($buildExitCode -ne 0) { throw "Maven terminou com código $buildExitCode." }
