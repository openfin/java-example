# Version 6.0.0.3.SNAPSHOT
## New Features

* Support for launching Runtime from a remote manifest.

## Bug Fixes

* Use ProcessBuilder to launch OpenFin to solve an issue with security on Citrix server.
* Added proper clean-up for ExternalWindowObserver

# Version 6.0.0.2
## New Features
* added ApplicationOptions.put method.

## Bug Fixes
* Fixed an issue with websocket timeout

# Version 6.0.0.1

## New Features
* Requires Version 6.0+ version of OpenFin Runtime.
* Improved support for window embedding
* Replaced WebSocket library with org.eclipse.jetty.websocket

# Version 5.44.3.2

## New Features

* Users ProcessBuilder to launch OpenFinLauncher.

# Version 5.44.3.1

## New Features

* Added RuntimeConfiguration class to improve configurability of Runtime from Java programs.
* Added DesktopConnection.connect(RuntimeConfiguration)
* Added DesktopStateListener.onClose to notify connection to Runtime is closed.
* Added Application.getGroups

## Bug Fixes
* Fixed API doc for WindowOptions.setTaskbarIcon
* Updated OpenFinInstaller.exe to handle client certificate
* Fixed an issue with duplicate UUIDs for DesktopConnection

# Version 5.44.2.4
## New Features
* Added setRdmUrl and setRuntimeAssetesUrl in DesktopConnect
* Use proper names for threads created by DesktopConnection during launching Runtime

# Version 5.44.2.3
## New Features
* Cross-app docking: Windows from different HTML5 applications now can join the same group and dock to each other.  Runtime 5.44.11.10 is required.
* DesktopConnection catches exceptions from onSuccess and onError in AckListener

## Bug Fixes
* Application.close(AckListener) is deprecated
* fixed an issue in getGroup

# Version 5.44.2.2
## New Features
* Implemented timeout logout logic for DesktopConnection.connectToVersion
* Added support for app-connected event for Window class
* Added Application.createChildWindow method
* Replaced java logging with slf4j.

## Bug Fixes
* Fixed an issue with reconnect with port discovery.
* Fixed an issue with Window.addEventListener

# Version 5.44.2.1
## New Features
* InterApplicationBus.send/publish accepts AckListener callback.
* Added support for security realm in DesktopConnection
