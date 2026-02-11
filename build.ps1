param (
    [switch]$SkipTests,
    [switch]$UpdateSnapshots
)

$logFile = "build.log"
$mavenCommand = "mvn clean install"

if ($SkipTests) {
    $mavenCommand += " -DskipTests"
}

if ($UpdateSnapshots) {
    $mavenCommand += " -U"
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Starting Build: $mavenCommand" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Run Maven and capture output to file and console
$processOptions = @{
    FilePath = "powershell"
    ArgumentList = "-Command", "Invoke-Expression '$mavenCommand 2>&1 | Tee-Object -FilePath $logFile'"
    PassThru = $true
    Wait = $true
}

$process = Start-Process @processOptions

if ($process.ExitCode -eq 0) {
    Write-Host "`nBUILD SUCCESSFUL ✅" -ForegroundColor Green
} else {
    Write-Host "`nBUILD FAILED ❌" -ForegroundColor Red
    Write-Host "Checking for errors in $logFile..." -ForegroundColor Yellow
    
    # Extract and display relevant error lines
    $content = Get-Content $logFile
    $errors = $content | Select-String -Pattern "ERROR", "Failed", "Could not find artifact", "Exception" -Context 0, 2
    
    if ($errors) {
        Write-Host "`n--- Error Summary ---" -ForegroundColor Red
        $errors | Select-Object -First 20 | ForEach-Object { Write-Host $_ }
        Write-Host "---------------------" -ForegroundColor Red
    }
    
    Write-Host "`nFull log available at: $logFile"
}
