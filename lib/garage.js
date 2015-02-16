var Promise = require('bluebird');
var Gpio = require('onoff').Gpio;
var request = require('request');
var config = require('../config.json');

var reedSwitch = new Gpio(config.reed_switch_pin, 'in');
var relaySwitch = new Gpio(config.relay_pin, 'in');

var autoCloseInProgress = false;
function autoClose(time) {
  autoCloseInProgress = true;
  console.log('[%s] Closing garage in %dms', (new Date()).toISOString(), time);
  setTimeout(function () {
    if (module.exports.status === 'closed') {
      console.log('[%s] Garage closed... stoping autoClose', (new Date()).toISOString());
      autoCloseInProgress = false;
      return;
    }
    console.log('[%s] Closing garage via autoClose.', (new Date()).toISOString());
    module.exports.relay();
    console.log('[%s] Making sure it\'s closed in %dms', (new Date()).toISOString(), time);
    autoClose(time);
  }, time);
}

function read() {
  reedSwitch.read(function (err, value) {
    var status  = value ? 'open' : 'closed';
    if (status !== module.exports.status) {
      console.log('[%s] The garage is now %s', (new Date()).toISOString(), status);
      if (module.exports.callback) {
        request({
          url: module.exports.callback,
          method: 'POST',
          json: { status: status }
        }, function (err, resp , body) {
          if (err) {
            return console.error('[%s] Request to %s failed', (new Date()).toISOString(), module.exports.callback);
          }
          console.log('[%s] SmartThings updated successfuly.', (new Date()).toISOString());
        });
      }
    }
    module.exports.status = status;
    if (status === 'open' && !autoCloseInProgress) autoClose(config.timeout);
    setTimeout(read, 500);
  });
}

process.on('SIGINT', function () {
  reedSwitch.unexport();
  relaySwitch.unexport();
  console.log('Goodbye!');
  process.exit();
});



module.exports = {
  callback: config && config.callback || null,
  status: 'closed',
  setup: function () {
    read();
    return Promise.resolve();
  },
  relay: function () {
    return new Promise(function (resolve, reject) {
      relaySwitch.setDirection('out');
      setTimeout(function () {
        relaySwitch.setDirection('in');
        resolve();
      }, 300);
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
