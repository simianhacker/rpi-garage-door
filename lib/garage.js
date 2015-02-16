var Promise = require('bluebird');
var gpio = require('pi-gpio');
var autoCloseWait = 120000;
var pins = {};
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
  gpio.read(7, function (err, value) {
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
    if (status === 'open' && !autoCloseInProgress) autoClose(autoCloseWait);
    setTimeout(read, 500);
  });
}

process.on('SIGINT', function () {
  gpio.close(7, function () {
    gpio.close(12, function () {
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
      setupPin(7, 'in').then(read),
      setupPin(12, 'in')
    ]);
  },
  relay: function () {
    return new Promise(function (resolve, reject) {
      gpio.setDirection(12, 'out', function (err) {
        if (err) return reject(err);
        setTimeout(function () {
          gpio.setDirection(12, 'in', function (err) {
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
