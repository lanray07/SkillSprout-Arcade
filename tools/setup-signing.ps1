$ErrorActionPreference = 'Stop'
$signingDirectory = Join-Path $env:USERPROFILE '.skillsprout/release-signing'
$propertiesFile = Join-Path $signingDirectory 'signing.properties'
$keystoreFile = Join-Path $signingDirectory 'skillsprout-production.jks'
if ((Test-Path -LiteralPath $propertiesFile) -or (Test-Path -LiteralPath $keystoreFile)) {
    throw 'Signing material already exists. Reuse it; do not replace a publishing identity.'
}
New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$currentSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $signingDirectory /inheritance:r /grant:r "*${currentSid}:(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Unable to restrict signing directory permissions.' }
$secretBytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $random.GetBytes($secretBytes) } finally { $random.Dispose() }
$signingPassword = [BitConverter]::ToString($secretBytes).Replace('-', '').ToLowerInvariant()
$env:SKILLSPROUT_KEYTOOL_PASSWORD = $signingPassword
try {
    & keytool.exe -genkeypair -keystore $keystoreFile -storetype JKS -alias skillsprout-production -keyalg RSA -keysize 4096 -validity 10950 -dname 'CN=O. Bankole' -storepass:env SKILLSPROUT_KEYTOOL_PASSWORD -keypass:env SKILLSPROUT_KEYTOOL_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed.' }
    $portablePath = $keystoreFile.Replace('\', '/')
    $propertiesText = "storeFile=$portablePath`nstorePassword=$signingPassword`nkeyAlias=skillsprout-production`nkeyPassword=$signingPassword`n"
    [IO.File]::WriteAllText($propertiesFile, $propertiesText, [Text.UTF8Encoding]::new($false))
    & keytool.exe -exportcert -rfc -keystore $keystoreFile -alias skillsprout-production -storepass:env SKILLSPROUT_KEYTOOL_PASSWORD -file (Join-Path $signingDirectory 'publisher-certificate.pem')
    if ($LASTEXITCODE -ne 0) { throw 'Public certificate export failed.' }
    Write-Output "Production signing identity created in $signingDirectory. Back up the directory securely; never commit its private files."
} finally {
    Remove-Item Env:SKILLSPROUT_KEYTOOL_PASSWORD -ErrorAction SilentlyContinue
    $signingPassword = $null
    $propertiesText = $null
    [Array]::Clear($secretBytes, 0, $secretBytes.Length)
}
