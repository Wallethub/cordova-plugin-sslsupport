// exports.coolMethod = function(arg0, success, error) {
//     exec(success, error, "CordovaPluginSslSupport", "coolMethod", [arg0]);
// };

var exec = require('cordova/exec');

// Generate a unique string similar to php uniqid
function uniqid(prefix, more_entropy) {
    if (typeof prefix === 'undefined') {
        prefix = '';
    }
    var retId;
    var formatSeed = function (seed, reqWidth) {
        seed = parseInt(seed, 10).toString(16); // to hex str
        if (reqWidth < seed.length) { // so long we split
            return seed.slice(seed.length - reqWidth);
        }
        if (reqWidth > seed.length) { // so short we pad
            return Array(1 + (reqWidth - seed.length)).join('0') + seed;
        }
        return seed;
    };
    // BEGIN REDUNDANT
    if (!this.php_js) {
        this.php_js = {};
    }
    // END REDUNDANT
    if (!this.php_js.uniqidSeed) { // init seed with big random int
        this.php_js.uniqidSeed = Math.floor(Math.random() * 0x75bcd15);
    }
    this.php_js.uniqidSeed++;
    retId = prefix; // start with prefix, add current milliseconds hex string
    retId += formatSeed(parseInt(new Date().getTime() / 1000, 10), 8);
    retId += formatSeed(this.php_js.uniqidSeed, 5); // add seed hex string
    if (more_entropy) {
        // for more entropy we add a float lower to 10
        retId += (Math.random() * 10).toFixed(8).toString();
    }
    return retId;
};

var currentUrlId;

function validateParams(params) {
    var isfail = 0, failedparam;
    if (typeof params.url != "string") { isfail = 1; failedparam = params.url;}
    else if (typeof params.data != "object") { isfail = 1; failedparam = params.data}
    else if (typeof params.headers != "object") { isfail = 1; failedparam = params.headers}
    
    return {
        failed: isfail,
        details : isfail ? { errorcode: -1, errordomain: 'invalidParameter', errorinfo: failedparam } : null
    }
}

var http = {
    sslPinning: true,
    allCertificates : false,
    domainValidation : true,
    enableSSLPinning: function (enable, success, failure) {
        return exec(function () {
            if (success) success();
            http.sslPinning = enable;
        }, function () {
            if (failure) failure();
        }, "CordovaPluginSslSupport", "enableSSLPinning", [enable]);
    },
    acceptAllCerts: function (allow, success, failure) {
        return exec(function () {
            if (success) success();
            http.allCertificates = allow;
        }, function () {
            if (failure) failure();
        }, "CordovaPluginSslSupport", "acceptAllCerts", [allow]);
    },
    validateDomainName: function (validate, success, failure) {
        return exec(function () {
            if (success) success();
            http.domainValidation = validate;
        }, function () {
            if (failure) failure();
        }, "CordovaPluginSslSupport", "validateDomainName", [validate]);
    },
    setHeader: function (header, value, success, failure) {
        return exec(success, failure, "CordovaPluginSslSupport", "setHeader", [header, value]);
    },
    getCookies: function (params, success, failure) {
        var domain = typeof params == "string" ? (params || "all") : "all";
        return exec(success, failure, "CordovaPluginSslSupport", "getCookies", [domain]);
    },
    post: function (params, success, failure) {
        var url = ""; var data = {}; var headers = {}; var urlkey = uniqid();

        if (params.hasOwnProperty("url")) { url = params.url; }
        if (params.hasOwnProperty("data")) { data = params.data || {} }
        if (params.hasOwnProperty("headers")) { headers = params.headers || {} }
        if (params.id) { urlkey = params.id; }
        
        var validate = validateParams({ url:url,headers:headers,data:data });
        if(validate.failed) {
            if(failed) failure(validate.details);
            return;
        }

        currentUrlId = urlkey;

        return exec(function (response) {
            if (success) success(response);
        }, function (request) {
            
            if (failure) failure(request);

        }, "CordovaPluginSslSupport", "post", [url, data, headers,urlkey]);
    },
    get: function (params, success, failure) {
        var url = ""; var data = {}; var headers = {}; var urlkey = uniqid(), failedparam;

        if (params.hasOwnProperty("url")) { url = params.url;  }
        if(params.hasOwnProperty("data")) {data = params.data || {};}
        if(params.hasOwnProperty("headers")) {headers = params.headers || {};}
        if(params.id) { urlkey = params.id; }

        var validate = validateParams({ url:url,headers:headers,data:data });
        if(validate.failed) {
            if(failed) failure(validate.details);
            return;
        }

        currentUrlId = urlkey;

        return exec(function (response) {
            if (success) success(response);
        }, function (request) {
            if (failure) failure(request);
        }, "CordovaPluginSslSupport", "get", [url, data, headers, urlkey]);
    },
    cancelRequest: function (urlkey, success, failure) {
        if (!urlkey) { urlkey = currentUrlId; }

        return exec(function () {
            if (success) success();
        }, function () {
            if (failure) failure();
        }, "CordovaPluginSslSupport", "cancelRequest", [urlkey]);
    }
};

module.exports = http;
window.sslHTTP = http;