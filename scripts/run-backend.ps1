param(
    [string]$EnvironmentFile = ".env"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentPath = if ([System.IO.Path]::IsPathRooted($EnvironmentFile)) {
    $EnvironmentFile
} else {
    Join-Path $projectRoot $EnvironmentFile
}

function Import-DotEnv {
    param(
        [string]$Path,
        [bool]$OverrideExisting
    )

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmedLine = $line.Trim()

        if (-not $trimmedLine -or $trimmedLine.StartsWith("#")) {
            continue
        }

        $separatorIndex = $trimmedLine.IndexOf("=")
        if ($separatorIndex -le 0) {
            throw "Invalid environment entry in ${Path}: $trimmedLine"
        }

        $key = $trimmedLine.Substring(0, $separatorIndex).Trim()
        $value = $trimmedLine.Substring($separatorIndex + 1).Trim()

        if ($key -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            throw "Invalid environment variable name in ${Path}: $key"
        }

        if ($value.Length -ge 2) {
            $hasDoubleQuotes = $value.StartsWith('"') -and $value.EndsWith('"')
            $hasSingleQuotes = $value.StartsWith("'") -and $value.EndsWith("'")
            if ($hasDoubleQuotes -or $hasSingleQuotes) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }

        $currentValue = [Environment]::GetEnvironmentVariable($key, "Process")
        if ($OverrideExisting -or [string]::IsNullOrWhiteSpace($currentValue)) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

if (-not (Test-Path -LiteralPath $environmentPath -PathType Leaf)) {
    throw "Environment file not found: $environmentPath. Copy .env.example to .env and configure it first."
}

Import-DotEnv -Path $environmentPath -OverrideExisting $true

$frontendEnvironmentPath = Join-Path $projectRoot "frontend\.env.local"
if (Test-Path -LiteralPath $frontendEnvironmentPath -PathType Leaf) {
    Import-DotEnv -Path $frontendEnvironmentPath -OverrideExisting $false
}

$requiredVariables = @(
    "DATABASE_URL",
    "DATABASE_USERNAME",
    "DATABASE_PASSWORD",
    "SUPABASE_AUTH_ISSUER",
    "SUPABASE_JWKS_URI",
    "CORS_ALLOWED_ORIGINS"
)

$invalidVariables = foreach ($key in $requiredVariables) {
    $value = [Environment]::GetEnvironmentVariable($key, "Process")
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'your-project|your-password|<[^>]+>') {
        $key
    }
}

$storageConfiguration = @(
    @{ Name = "SUPABASE_URL or VITE_SUPABASE_URL"; Keys = @("SUPABASE_URL", "VITE_SUPABASE_URL") },
    @{ Name = "SUPABASE_PUBLISHABLE_KEY or VITE_SUPABASE_PUBLISHABLE_KEY"; Keys = @("SUPABASE_PUBLISHABLE_KEY", "VITE_SUPABASE_PUBLISHABLE_KEY") }
)

foreach ($configuration in $storageConfiguration) {
    $hasValidValue = $false
    foreach ($key in $configuration.Keys) {
        $value = [Environment]::GetEnvironmentVariable($key, "Process")
        if (-not [string]::IsNullOrWhiteSpace($value) -and $value -notmatch 'your-project|your-publishable-key|<[^>]+>') {
            $hasValidValue = $true
            break
        }
    }
    if (-not $hasValidValue) {
        $invalidVariables += $configuration.Name
    }
}

if ($invalidVariables.Count -gt 0) {
    throw "Missing or placeholder backend configuration: $($invalidVariables -join ', '). See docs/supabase-setup.md."
}

Write-Host "Backend configuration loaded from $environmentPath (secret values hidden)."

if ([string]::IsNullOrWhiteSpace($env:MAVEN_USER_HOME) -and -not [string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
    $env:MAVEN_USER_HOME = Join-Path $env:USERPROFILE ".m2"
}

Push-Location (Join-Path $projectRoot "backend")
try {
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
