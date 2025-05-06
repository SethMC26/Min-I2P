<#
.SYNOPSIS
    Kills processes listening on specified UDP and TCP ports on localhost.
#>

# ---- 1. Require elevation ---------------------------------------------------
if (-not ([Security.Principal.WindowsPrincipal] `
        [Security.Principal.WindowsIdentity]::GetCurrent()
       ).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)) {
    Write-Warning "ERROR: Run this script in an *elevated* PowerShell window."
    exit 1
}

# ---- 2. Port lists ----------------------------------------------------------
$portsMap = @{
    UDP = @(8080) + (10001..10005)
    TCP = 20000..20005
}

# ---- 3. Helper: kill by PID -------------------------------------------------
function Kill-ProcId {
    param(
        [int]    $ProcId,
        [string] $Proto,
        [int]    $Port
    )

    if (Get-Process -Id $ProcId -ErrorAction SilentlyContinue) {
        Write-Host "[$Proto] Killing PID $ProcId on port $Port"
        taskkill /PID $ProcId /F | Out-Null
    } else {
        Write-Host "[$Proto] PID $ProcId not found (already exited?)"
    }
}

# ---- 4. Scan & kill ---------------------------------------------------------
foreach ($proto in $portsMap.Keys) {
    foreach ($port in $portsMap[$proto]) {

        try {
            # Regex with capture group 1 = PID
            $regex = if ($proto -eq 'UDP') {
                "^\s*UDP\s+\S+:$port\s+\S+\s+(\d+)"
            } else {
                "^\s*TCP\s+\S+:$port\s+\S+\s+LISTENING\s+(\d+)"
            }

            $lines = netstat -ano -p $proto | Select-String -Pattern $regex

            if (-not $lines) {
                Write-Host "[$proto] Port $port is free."
                continue
            }

            foreach ($match in $lines) {
                $procId = [int]$match.Matches[0].Groups[1].Value
                Kill-ProcId -ProcId $procId -Proto $proto -Port $port
            }
        }
        catch {
            Write-Warning "Error processing $proto port $port : $_"
        }
    }
}
