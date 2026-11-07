# Guarda evidências de um aplicativo próprio já instalado em um Android de teste.
# Não instala ferramentas, não compila o aplicativo e não acessa bancos diretamente.
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$DeviceSerial,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$')]
    [string]$PackageName,
    [Parameter(Mandatory = $true)][string]$PastaSaida,
    [ValidateSet('antes', 'depois')][string]$Etapa = 'depois',
    [switch]$MedirAbertura,
    [ValidateRange(1, 30)][int]$Repeticoes = 10,
    [switch]$ConfirmoAplicativoDeTeste
)
$ErrorActionPreference = 'Stop'

if (-not $ConfirmoAplicativoDeTeste) {
    throw 'Use apenas aplicativo e dados sintéticos de teste; informe -ConfirmoAplicativoDeTeste.'
}
if ($PackageName -eq 'host.exp.exponent') {
    throw 'Selecione o pacote próprio do AstroChimp. Expo Go não equivale ao aplicativo release.'
}
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCommand) { throw 'ADB indisponível. Instale/configure Android SDK Platform Tools antes da coleta.' }
$adbPath = $adbCommand.Source

# Cada consulta usa o aparelho informado; a coleta nunca escolhe outro automaticamente.
function Consultar-Android {
    param([string[]]$Argumentos)
    $texto = & $adbPath -s $DeviceSerial @Argumentos 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($texto -join [Environment]::NewLine) }
    return ($texto -join [Environment]::NewLine)
}
if ((Consultar-Android -Argumentos @('get-state')).Trim() -ne 'device') {
    throw 'O aparelho não está pronto e autorizado no ADB.'
}
$apk = Consultar-Android -Argumentos @('shell', 'pm', 'path', $PackageName)
if ($apk -notmatch 'package:') { throw 'O pacote informado não está instalado no aparelho.' }

# Um diretório novo por coleta preserva as evidências anteriores.
$sufixo = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 6)
$destino = Join-Path ([IO.Path]::GetFullPath($PastaSaida)) ('android-' + $Etapa + '-' + $sufixo)
New-Item -ItemType Directory -Path $destino | Out-Null
$dados = [ordered]@{
    data = (Get-Date -Format o)
    etapa = $Etapa
    pacote = $PackageName
    adb = (& $adbPath version | Out-String).Trim()
    fabricante = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.product.manufacturer')).Trim()
    modelo = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.product.model')).Trim()
    android = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.build.version.release')).Trim()
    api = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.build.version.sdk')).Trim()
    hardware = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.hardware')).Trim()
    emulador = (Consultar-Android -Argumentos @('shell', 'getprop', 'ro.kernel.qemu')).Trim()
    aberturaSolicitada = [bool]$MedirAbertura
    limite = 'TotalTime aproxima a primeira exibição da Activity; não prova fonte, tela ou dados prontos.'
}
$dados | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $destino 'ambiente.json') -Encoding utf8
Consultar-Android -Argumentos @('shell', 'dumpsys', 'package', $PackageName) |
    Set-Content -LiteralPath (Join-Path $destino 'pacote.txt') -Encoding utf8
Consultar-Android -Argumentos @('shell', 'dumpsys', 'meminfo', $PackageName) |
    Set-Content -LiteralPath (Join-Path $destino 'memoria.txt') -Encoding utf8
Consultar-Android -Argumentos @('shell', 'dumpsys', 'gfxinfo', $PackageName, 'framestats') |
    Set-Content -LiteralPath (Join-Path $destino 'quadros.txt') -Encoding utf8

# Antes do cenário, reinicia somente o contador de quadros deste pacote.
if ($Etapa -eq 'antes') {
    Consultar-Android -Argumentos @('shell', 'dumpsys', 'gfxinfo', $PackageName, 'reset') |
        Set-Content -LiteralPath (Join-Path $destino 'reinicio-quadros.txt') -Encoding utf8
}

# A abertura é opcional: -S interrompe o processo de teste antes de cada partida.
# Não limpa dados do app. Estado persistido e cache de disco continuam presentes.
if ($MedirAbertura) {
    $resolucao = Consultar-Android -Argumentos @('shell', 'cmd', 'package', 'resolve-activity', '--brief',
        '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', $PackageName)
    $componente = @($resolucao -split '\r?\n' | Where-Object { $_ -match '^[\w.]+/[\w.$]+$' })[-1]
    if (-not $componente -or $componente.Split('/')[0] -ne $PackageName) {
        throw 'Não foi possível identificar a Activity inicial do pacote próprio.'
    }
    $aberturas = @()
    for ($rodada = 1; $rodada -le $Repeticoes; $rodada++) {
        $texto = Consultar-Android -Argumentos @('shell', 'am', 'start', '-S', '-W', '-n', $componente,
            '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER')
        $texto | Set-Content -LiteralPath (Join-Path $destino ("abertura-{0:00}.txt" -f $rodada)) -Encoding utf8
        $totalMs = $null
        if ($texto -match '(?m)^TotalTime:\s*(\d+)') { $totalMs = [int]$Matches[1] }
        $aberturas += [pscustomobject]@{
            rodada = $rodada
            totalTimeMs = $totalMs
            sucesso = ($texto -match '(?m)^Status:\s*ok' -and $null -ne $totalMs)
        }
        Start-Sleep -Seconds 2
    }
    $aberturas | Export-Csv -LiteralPath (Join-Path $destino 'aberturas.csv') -NoTypeInformation -Encoding utf8
}
Write-Output "Evidências salvas em $destino"
