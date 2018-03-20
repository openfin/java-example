
# Version 7.0.1-SNAPSHOT

## New Features
* Improved thread safety with concurrent collection classes.
* Added error message to DesktopStateListener.onClose (breaking change)

## Bug Fixes
* Fixed an issue with passing arguments in System.launchExternalProcess

# Version 6.0.2.1

## New Features
* Added Application.registerUser.
* Upgrade JNA library to 4.5.1.

# Version 6.0.1.3

## New Features
* Port discovery support on Mac and Linux with openfin-cli.
* Add support for legacy connecting to Runtime by hard-coded port number

# Version 6.0.1.2

## Bug Fixes
* Fixed an issue with deprecated DesktopConnect.connect method.
* Fixed an issue with sending connection info to Runtime.

# Version 6.0.1.1
## New Features
* Requires JDK1.7+
* Add browser-style navigation to Window class
* Add support for fallbackVersion of Runtime
* Add support for non-persistent connections to Runtime
* Improve suppot for app assets

## Bug Fixes
* Fixed an issue with loop of re-connecting to Runtime
* Fixed an issue with Window.close defaulting 'force' to true

# Version 6.0.1.0
## New Features

* Support for launching Runtime from a remote manifest.
* Added Window.executeJavaScript
* Added Window.showDeveloperTools
* Added Window.navigate
* Added Application.wrapWindow
* Added OpenFinRuntime.getHostSpecs
* Support for port discovery of Runtime process at different integrity levels

## Bug Fixes

* Use ProcessBuilder to launch OpenFin to solve an issue with security on Citrix server.
* Added proper clean-up for ExternalWindowObserver
* Fixed an issue with retrying connection to Runtime

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

# Version 5.44.5.3
* Fixed an issue with connection timeout at 5 seconds

# Version 5.44.5.2
## New Features
* Add support for fallbackVersion of Runtime

# Version 5.44.5.1
## New Features
* Branch from Version 6.0.1.1 to support Java 1.6

# Version 5.44.3.6

* Fixed an issue with loop of re-connecting to Runtime

# Version 5.44.3.5

* Fixed an issue with Window.close defaulting 'force' to true

# Version 5.44.3.4

* Fixed an issue with retrying connection to Runtime

# Version 5.44.3.3

## New Features
* Support for port discovery of Runtime process at different integrity levels

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
* Fixed an issue with duplicate UUIDs for DesktopConnection in version 5.44.11.10 of Runtime 

# Version 5.44.2.5

## New Features
* Support for port discovery of Runtime process at different integrity levels

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
