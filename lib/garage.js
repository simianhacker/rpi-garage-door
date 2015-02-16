var Promise = require('bluebird');
var gpio = require('pi-gpio');
var request = require('request');
var config = require('../config.json');

function setupPin(channel, direction) {
  return new Promise(function (resolve, reject) {
    gpio.open(channel, direction, function (err) {
      if (err) return reject(err);
      resolve();
    });
  });
}


var autoCloseInProgress = false;
function autoClose(time) {
  autoCloseInProgress = true;
  console.log('[%s] Closing garage in %dms', (new Date()).toString(), time);
  setTimeout(function () {
    if (module.exports.status === 'closed') {
      console.log('[%s] Garage closed... stoping autoClose', (new Date()).toString());
      autoCloseInProgress = false;
      return;
    }
    console.log('[%s] Closing garage via autoClose.', (new Date()).toString());
    module.exports.relay();
    console.log('[%s] Making sure it\'s closed in %dms', (new Date()).toString(), time);
    autoClose(time);
  }, time);
}

function read() {
  gpio.read(config.reed_switch_pin, function (err, value) {
    var status  = value ? 'open' : 'closed';
    if (status !== module.exports.status) {
      console.log('[%s] The garage is now %s', (new Date()).toString(), status);
      if (module.exports.callback) {
        request({
          url: module.exports.callback,
          method: 'POST',
          json: { status: status }
        }, function (err, resp , body) {
          if (err) {
            return console.error('[%s] Request to %s failed', (new Date()).toString(), module.exports.callback);
          }
          console.log('[%s] SmartThings updated successfuly.', (new Date()).toString());
        });
      }
    }
    module.exports.status = status;
    if (status === 'open' && !autoCloseInProgress) autoClose(config.timeout);
    setTimeout(read, 500);
  });
}

process.on('SIGINT', function () {
  gpio.close(config.reed_switch_pin, function () {
    gpio.close(config.relay_pin, function () {
      console.log('Goodbye!');
      process.exit();
    });
  });
});



module.exports = {
  callback: config && config.callback || null,
  status: 'closed',
  setup: function () {
    return Promise.all([
      setupPin(config.reed_switch_pin, 'in').then(read),
      setupPin(config.relay_pin, 'in')
    ]);
  },
  relay: function () {
    return new Promise(function (resolve, reject) {
      gpio.setDirection(config.relay_pin, 'out', function (err) {
        if (err) return reject(err);
        setTimeout(function () {
          gpio.setDirection(config.relay_pin, 'in', function (err) {
            if (err) return reject(err);
            resolve();
          });
        }, 300);
      });
    });
  },
  subscribe: function (callback) {
    module.exports.callback = callback;
    var options = { encoding: 'utf8' };
    return new Promise(function (resolve, reject) {
      fs.writeFile('../config.json', JSON.stringify({ callback: callback }), options, function (err) {
        if (err) {
          console.error(err);
          return reject(err);
        }
        resolve();
      });
    });
  }
};
