rem This demo will start Hello OpenFin demo app first, then connect to OpenFin runtime so it can control window of Hello OpenFin demo app
rem Please update -DOpenFinPath to point to OpenFinRVM.exe in your environment.
rem For any questions or issues, please contact support@openfin.co


java -cp java-desktop-adapter-3.0.1.0.jar -DOpenFinPort=9696 -DOpenFinOption=--config=\"https://demoappdirectory.openf.in/desktop/config/apps/OpenFin/HelloOpenFin/app.json\" -DStartupUUID="OpenFinHelloWorld" com.openfin.desktop.demo.OpenFinDesktopDemo
