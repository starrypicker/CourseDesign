@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE__=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $proxy=''; [Net.WebRequest]::DefaultWebProxy.Credentials=[Net.CredentialCache]::DefaultCredentials; $script='%~dp0mvnw'; $d=$scriptDir; while($true){$p=Join-Path $d '.mvn\wrapper\MavenWrapperDownloader.java'; if(Test-Path $p){$c=get-content $p -raw; break}; $d=(Split-Path $d -Parent); if($d -eq ''){exit 1}}; $m2=\"$env:USERPROFILE\.m2\wrapper\dists\"; $f=Join-Path $m2 'maven-wrapper.properties'; if(!(Test-Path $f)){New-Item -ItemType Directory -Path $m2 -Force|Out-Null; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.properties' -OutFile $f}; $p=get-content $f|ConvertFrom-StringData; $u=$p.distributionUrl; if($u -eq $null){$u='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip'}; $m2=\"$env:USERPROFILE\.m2\wrapper\dists\"; $h=$u.GetHashCode().ToString('x'); $d=Join-Path $m2 $h; $z=Join-Path $d 'maven.zip'; if(!(Test-Path $d)){New-Item -ItemType Directory -Path $d -Force|Out-Null}; if(!(Test-Path $z)){Write-Host 'Downloading Maven...'; Invoke-WebRequest -Uri $u -OutFile $z}; $m=Join-Path $d 'apache-maven'; if(!(Test-Path $m)){Expand-Archive -Path $z -DestinationPath $d -Force}; $mvn=Get-ChildItem -Path $d -Filter 'mvn.cmd' -Recurse|Select-Object -First 1; Write-Output $mvn.FullName}"`) DO @(
  IF NOT "%%A"=="" SET __MVNW_CMD__=%%A
)

@SET PSModulePath=%__MVNW_PSMODULEP_SAVE__%

@IF "%__MVNW_CMD__%"=="" (
  @ECHO Failed to download Maven Wrapper. Please install Maven manually.
  @EXIT /B 1
)

%__MVNW_CMD__% %*
