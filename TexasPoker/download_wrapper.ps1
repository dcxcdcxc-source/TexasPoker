$url = "https://raw.githubusercontent.com/gradle/gradle/v8.1.1/gradle/wrapper/gradle-wrapper.jar"
$dest = "c:\Users\lidec\WorkBuddy\Claw\TexasPoker\gradle\wrapper\gradle-wrapper.jar"
Write-Output "Downloading gradle-wrapper.jar..."
try {
    Invoke-WebRequest -Uri $url -OutFile $dest -TimeoutSec 60
    $size = (Get-Item $dest).Length
    Write-Output "Downloaded successfully! Size: $size bytes"
} catch {
    Write-Output "Failed: $_"
    # Try alternative source
    $url2 = "https://github.com/gradle/gradle/raw/v8.1.1/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $url2 -OutFile $dest -TimeoutSec 60
        $size = (Get-Item $dest).Length
        Write-Output "Downloaded from alt source! Size: $size bytes"
    } catch {
        Write-Output "Alt also failed: $_"
    }
}
