# Hubitat

Some of my first files for Hubitat

I started with a Qubino ZMNHSDx DIN Dimmer driver that does not have a temp sensor so it shows properly on Alexa. The device must be included without security or it will cause issues. Use a second controller to do this (zwave stick and SiLabs Windows Zwave Controller software).

Added the Qubino ZMNHYDx Smart Plug 16A driver as the current Hubtat does not seem to hve a driver for a switch with poer measturements. This driver reports Watts, Kwh, Voltage and Amperage. The devices should be included without security. This can be done on the Hub directly.
