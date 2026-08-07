#define AppName "Chatone"
#define AppPublisher "Rudione"
#define AppExeName "Chatone.exe"
#define AppUrl "https://github.com/Rudione/Chatone"

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif

#ifndef SourceDir
  #define SourceDir "..\build\compose\binaries\main-release\app\Chatone"
#endif

#ifndef OutputDir
  #define OutputDir "..\build\installer"
#endif

[Setup]
AppId={{8F3C1B27-4E5A-4B93-9C2D-CHATONE000001}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppUrl}
AppSupportURL={#AppUrl}/issues
AppUpdatesURL={#AppUrl}/releases
VersionInfoVersion={#AppVersion}

PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
DisableDirPage=auto
DisableReadyPage=yes
DisableWelcomePage=yes
AllowNoIcons=yes

OutputDir={#OutputDir}
OutputBaseFilename=Chatone-{#AppVersion}-setup
SetupIconFile=..\src\desktopMain\resources\logochattone.ico
UninstallDisplayIcon={app}\{#AppExeName}
UninstallDisplayName={#AppName}

Compression=lzma2/max
SolidCompression=yes
LZMANumBlockThreads=4
WizardStyle=modern
WizardSizePercent=110
ShowLanguageDialog=no

CloseApplications=yes
CloseApplicationsFilter=*.exe,*.dll
RestartApplications=yes

ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "en"; MessagesFile: "compiler:Default.isl"
Name: "ru"; MessagesFile: "compiler:Languages\Russian.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
Filename: "{app}\{#AppExeName}"; Flags: nowait runasoriginaluser; Check: WantsSilentRelaunch

[Code]
function WantsSilentRelaunch(): Boolean;
begin
  Result := WizardSilent() and (ExpandConstant('{param:RESTARTAPPLICATIONS|0}') <> '0');
end;
