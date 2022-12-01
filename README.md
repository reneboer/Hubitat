# Hubitat

Some of my first files for Hubitat

I started with a Qubino ZMNHSDx DIN Dimmer driver. The build in driver will show in Alxes as a temp sensor even if you do not have a temp sensor connected. This driver will have it show properly as a dimmer on Alexa. The device must be included without security or it will cause issues, also with the build in driver. Use a second controller to do this (zwave stick and SiLabs Windows Zwave Controller software).

Added the Qubino ZMNHYDx Smart Plug 16A driver as the current Hubtat does not seem to have a driver for a switch with power measturements. This driver reports Watts, Kwh, Voltage and Amperage. The devices should be included without security. This can be done on the Hub directly.
