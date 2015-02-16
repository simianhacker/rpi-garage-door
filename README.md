# Raspberry Pi Garage Door Controller

Have you ever woken up in the morning to find you left your garage door open? This project attempts to solve that problem by creating an automatic garage door controller that close the garage door after being open for 2 minutes. I've also included my custom device type and connect app for integrating this with the SmartThings hub.

### Prep

I'm assuming you know how to setup your Raspberry Pi with your favorite distro and Node.js (v0.10). I'm also leaving it up to you to figure out how to either start the server via upstart, a cron with the @reboot directive, or runnig it via screen.

### Install

1 Install quick2wire/quick2wire-gpio-admin (One issue I found with this project is that I had to be running 2014-12-24-wheezy-raspbian. It seems that things changed in the 3.18 kernel which make it incompatible with quickwire-gpio-admin)
1 `git clone https://github.com/simianhacker/rpi-garage-door.git`
1 `cd rpi-garage-door && npm install`
1 `node index.js`

### Parts List

- Raspberry Pi A+/B+ with case, wifi adapter, micro SD card and 2000mA power suppy.
- 2 Chanel Relay Switch [link](http://www.amazon.com/gp/product/B0057OC6D8/ref=as_li_tl?ie=UTF8&camp=1789&creative=390957&creativeASIN=B0057OC6D8&linkCode=as2&tag=driscocityc0a-20&linkId=TBY7IJIXMEFS3Y3U)
- Magnetic Switch (Reed Switch) [link](http://www.amazon.com/gp/product/B0009SUF08/ref=as_li_tl?ie=UTF8&camp=1789&creative=390957&creativeASIN=B0009SUF08&linkCode=as2&tag=driscocityc0a-20&linkId=Y3OFNKEOINL6LPKT)
- 1kΩ Resistor
- 10kΩ Resistor
- 30 feet of wiring (Cat 5 works really well because you can use the left over for jumpers on breadboards and the wires that run from the relay to the garage door switch)
- 5 Female to Female jumper cables
- Sodering Iron
- Soder
- Heat shrink tubing

### Wiring Diagarm

![RPI Garage Door](https://raw.githubusercontent.com/simianhacker/rpi-garage-door/master/rpi-garage-door_bb.png)

### SmartThings Integration

![SmartThings Integration](https://dl.dropboxusercontent.com/u/41596401/smartthings-garage.png?raw=1)

### Inspiration

http://www.driscocity.com/idiots-guide-to-a-raspberry-pi-garage-door-opener/
