<#
.SYNOPSIS
    Kills any process listening on specified UDP and TCP ports on localhost,
    using netstat to enumerate listeners and taskkill to terminate them.
#>

# Make sure we're elevated
if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
        ).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)) {
    Write-Warning "This script must be run as Administrator."
    break
}

# Ports to scan
$udpPorts = @(8080) + (10001..10005)
$tcpPorts = 20000..20005

function Kill-PortProcess {
    param(
        [string]$Protocol,   # “UDP” or “TCP”
        [int]   $Port,
        [string]$StateFilter # for TCP, only “LISTENING” lines
    )

    # Run netstat for this protocol
    $lines = netstat -ano -p $Protocol 2>$null |
             Select-String ":$Port\s"

    if (-not $lines) {
        Write-Host "[$Protocol] Port $Port: no listeners found."
        return
    }

    foreach ($line in $lines) {
        # Split on whitespace; PID is always the last token
        $tokens = ($line -replace '\s+',' ') -split ' '
        $local        = $tokens[1]
        $stateOrBlank = if ($Protocol -eq 'TCP') { $tokens[3] } else { '' }
        $pid          = $tokens[-1]

        # If TCP, only kill LISTENING state
        if ($Protocol -eq 'TCP' -and $stateOrBlank -ne $StateFilter) {
            continue
        }

        # Double-check process exists
        if (Get-Process -Id $pid -ErrorAction SilentlyContinue) {
            Write-Host "[$Protocol] Killing PID $pid listening on $local"
            taskkill /PID $pid /F | Out-Null
        } else {
            Write-Host "[$Protocol] PID $pid not found (already exited?)."
        }
    }
}

# Kill UDP listeners
foreach ($port in $udpPorts) {
    Kill-PortProcess -Protocol 'UDP' -Port $port -StateFilter ''
}

# Kill TCP LISTENING
foreach ($port in $tcpPorts) {
    Kill-PortProcess -Protocol 'TCP' -Port $port -StateFilter 'LISTENING'
}
