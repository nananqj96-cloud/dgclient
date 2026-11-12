param(
    [string]$OutputPath = (Join-Path $PSScriptRoot 'build\SpotifyMediaBridge.exe')
)
$ErrorActionPreference = 'Stop'
$windowsRoot = [Environment]::GetFolderPath('Windows')
$framework = Join-Path $windowsRoot 'Microsoft.NET\Framework\v4.0.30319'
$csc = Join-Path $framework 'csc.exe'
$winMetadata = Join-Path $windowsRoot 'System32\WinMetadata'
if (-not (Test-Path -LiteralPath $csc)) { throw 'The .NET Framework C# compiler is required.' }
$references = @(
    (Join-Path $framework 'System.Runtime.dll'),
    (Join-Path $framework 'System.Web.Extensions.dll'),
    (Join-Path $framework 'System.Runtime.WindowsRuntime.dll'),
    (Join-Path $winMetadata 'Windows.Foundation.winmd'),
    (Join-Path $winMetadata 'Windows.Media.winmd'),
    (Join-Path $winMetadata 'Windows.Storage.winmd')
)
foreach ($reference in $references) {
    if (-not (Test-Path -LiteralPath $reference)) { throw "Missing reference: $reference" }
}
$output = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($output)) -Force | Out-Null
$args = @('/nologo', '/target:winexe', "/out:$output")
$args += $references | ForEach-Object { "/reference:$_" }
$args += (Join-Path $PSScriptRoot 'SpotifyMediaBridge.cs')
$args += (Join-Path $PSScriptRoot 'Properties\AssemblyInfo.cs')
& $csc @args
if ($LASTEXITCODE -ne 0) { throw "C# compilation failed with exit code $LASTEXITCODE" }
Write-Output $output
