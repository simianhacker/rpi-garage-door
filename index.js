var connect = require('connect');
var morgan = require('morgan');
var connectRouter = require('connect-route');
var http = require('http');
var garage = require('./lib/garage');
var port = 3000;

garage.setup().then(function () {
  var app = connect();
  app.use(morgan('[:date[clf]] :method :url :status :response-time ms - :res[content-length]'));

  app.use(connectRouter(function (router) {

    router.get('/', function (req, res, next) {
      res.statusCode = 200;
      res.setHeader('content-type', 'application/json');
      res.end(JSON.stringify({ status: garage.status }));
    });

    router.post('/', function (req, res, next) {
      garage.relay().then(function (status) {
        res.statusCode = 201;
        res.setHeader('content-type', 'application/json');
        res.end(JSON.stringify({ status: garage.status }));
      });
    });

    router.post('/subscribe', function (req, res, next) {
      var sendError = function () {
        res.statusCode = 400;
        res.setHeader('content-type', 'application/json');
        res.end(JSON.stringify({ status: garage.status, err: 'Callback missing.' }));
      };
      if (!req.headers['x-callback']) {
        return sendError();
      }
      garage.subscribe(req.headers['x-callback']).then(function () {
        res.statusCode = 201;
        res.setHeader('content-type', 'application/json');
        res.end(JSON.stringify({ status: garage.status }));
      }).catch(sendError);
    });

  }));

  http.createServer(app).listen(port, function () {
    console.log('[%s] Starting server on port %d', (new Date()).toString(), port);
  });
}).catch(function (err) {
console.error(err);
});



