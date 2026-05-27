@echo off
setlocal

set "ROOT=%~dp0"
set "PROJECT=%ROOT%project"
set "FRONTEND=%ROOT%frontend"
set "RUNNERS=%TEMP%\uow-dashboard-start"

if not exist "%RUNNERS%" mkdir "%RUNNERS%"

where docker >nul 2>nul
if errorlevel 1 (
    echo Docker was not found. Install or start Docker Desktop, then run this file again.
    pause
    exit /b 1
)

where mvn.cmd >nul 2>nul
if errorlevel 1 (
    echo Maven was not found. Install Maven or add it to PATH, then run this file again.
    pause
    exit /b 1
)

where npm.cmd >nul 2>nul
if errorlevel 1 (
    echo npm was not found. Install Node.js, then run this file again.
    pause
    exit /b 1
)

(
    echo @echo off
    echo title UOW Kafka
    echo cd /d "%ROOT%"
    echo docker compose -p uow-parking up -d --remove-orphans
    echo if errorlevel 1 echo Kafka could not start. Check Docker Desktop and whether port 9092 is already being used.
    echo if errorlevel 1 pause
    echo if errorlevel 1 exit /b 1
    echo docker compose -p uow-parking ps
    echo docker compose -p uow-parking logs -f kafka
) > "%RUNNERS%\kafka.bat"

(
    echo @echo off
    echo title UOW Accounts
    echo cd /d "%PROJECT%\accounts"
    echo set "APP_KAFKA_ENABLED=true"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\accounts.bat"

(
    echo @echo off
    echo title UOW API Gateway
    echo cd /d "%PROJECT%\api-gateway"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\gateway.bat"

(
    echo @echo off
    echo title UOW Spotter
    echo cd /d "%PROJECT%\spotter"
    echo set "SPOTTER_KAFKA_ENABLED=true"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\spotter.bat"

(
    echo @echo off
    echo title UOW Parking
    echo cd /d "%PROJECT%\parking"
    echo set "SPOTTER_SERVICE_URL=http://localhost:8085/api/spotter"
    echo set "ACCOUNT_SERVICE_URL=http://localhost:8081/accounts"
    echo set "APP_KAFKA_ENABLED=true"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\parking.bat"

(
    echo @echo off
    echo title UOW Occupancy
    echo cd /d "%PROJECT%\occupancy"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\occupancy.bat"

(
    echo @echo off
    echo title UOW Admin Stats
    echo cd /d "%PROJECT%\adminstats"
    echo call mvn.cmd spring-boot:run
    echo pause
) > "%RUNNERS%\adminstats.bat"

(
    echo @echo off
    echo title UOW Frontend
    echo cd /d "%FRONTEND%"
    echo set "NEXT_PUBLIC_API_URL=http://localhost:8089"
    echo set "SPOTTER_SERVICE_URL=http://localhost:8085/api/spotter"
    echo call npm.cmd install --cache .\.npm-cache
    echo call npm.cmd run dev
    echo pause
) > "%RUNNERS%\frontend.bat"

start "UOW Kafka" "%RUNNERS%\kafka.bat"
echo Waiting for Kafka on localhost:9092 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(120); do { try { $client=[Net.Sockets.TcpClient]::new(); $async=$client.BeginConnect('127.0.0.1',9092,$null,$null); if ($async.AsyncWaitHandle.WaitOne(1000)) { $client.EndConnect($async); $client.Close(); exit 0 }; $client.Close() } catch { }; Start-Sleep -Seconds 2 } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Kafka did not become ready. Check the UOW Kafka terminal for the error.
    pause
    exit /b 1
)

start "UOW Accounts" "%RUNNERS%\accounts.bat"
timeout /t 5 /nobreak >nul

start "UOW API Gateway" "%RUNNERS%\gateway.bat"
timeout /t 5 /nobreak >nul

start "UOW Spotter" "%RUNNERS%\spotter.bat"
echo Waiting for Spotter on http://localhost:8085 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(90); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8085/api/spotter/health' -TimeoutSec 2; if ($r.StatusCode -eq 200) { exit 0 } } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Spotter did not become ready. Check the UOW Spotter terminal for the error.
    pause
    exit /b 1
)

start "UOW Parking" "%RUNNERS%\parking.bat"
echo Waiting for Parking on http://localhost:8082 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(90); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8082/parking' -TimeoutSec 2; if ($r.StatusCode -lt 500) { exit 0 } } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Parking did not become ready. Check the UOW Parking terminal for the error.
    pause
    exit /b 1
)

start "UOW Occupancy" "%RUNNERS%\occupancy.bat"
echo Waiting for Occupancy on http://localhost:8083 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(90); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8083/occupancy/predictions' -TimeoutSec 2; if ($r.StatusCode -lt 500) { exit 0 } } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Occupancy did not become ready. Check the UOW Occupancy terminal for the error.
    pause
    exit /b 1
)

start "UOW Admin Stats" "%RUNNERS%\adminstats.bat"
echo Waiting for Admin Stats on http://localhost:8086 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(90); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8086/adminstats/latest' -TimeoutSec 2; if ($r.StatusCode -lt 500) { exit 0 } } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Admin Stats did not become ready. Check the UOW Admin Stats terminal for the error.
    pause
    exit /b 1
)

start "UOW Frontend" "%RUNNERS%\frontend.bat"
echo Waiting for Frontend on http://localhost:3000 ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$deadline=(Get-Date).AddSeconds(90); do { try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:3000' -TimeoutSec 2; if ($r.StatusCode -lt 500) { exit 0 } } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    echo Frontend did not become ready. Check the UOW Frontend terminal for the error.
    pause
    exit /b 1
)

start "" "http://localhost:3000/login"

echo Dashboard startup launched.
echo Leave the opened terminal windows running while you demo.
echo Staff login: admin@uowmail.edu.au / test123
pause
