//@ts-check
//@ts-ignore
var exec = require("cordova/exec");

// Generate a unique string similar to php uniqid
function uniqid(prefix, more_entropy) {
	if (typeof prefix === "undefined") {
		prefix = "";
	}
	var retId;
	var formatSeed = function (seed, reqWidth) {
		seed = parseInt(seed, 10).toString(16); // to hex str
		if (reqWidth < seed.length) {
			// so long we split
			return seed.slice(seed.length - reqWidth);
		}
		if (reqWidth > seed.length) {
			// so short we pad
			return Array(1 + (reqWidth - seed.length)).join("0") + seed;
		}
		return seed;
	};
	// BEGIN REDUNDANT
	if (!this.php_js) {
		this.php_js = {};
	}
	// END REDUNDANT
	if (!this.php_js.uniqidSeed) {
		// init seed with big random int
		this.php_js.uniqidSeed = Math.floor(Math.random() * 0x75bcd15);
	}
	this.php_js.uniqidSeed++;
	retId = prefix; // start with prefix, add current milliseconds hex string
	retId += formatSeed(parseInt((new Date().getTime() / 1000).toString(), 10), 8);
	retId += formatSeed(this.php_js.uniqidSeed, 5); // add seed hex string
	if (more_entropy) {
		// for more entropy we add a float lower to 10
		retId += (Math.random() * 10).toFixed(8).toString();
	}
	return retId;
}

var currentUrlId;

function validateParams(params) {
	var isfail = 0,
		failedparam;
	if (typeof params.url != "string") {
		isfail = 1;
		failedparam = params.url;
	} else if (typeof params.data != "object") {
		isfail = 1;
		failedparam = params.data;
	} else if (typeof params.headers != "object") {
		isfail = 1;
		failedparam = params.headers;
	}

	return {
		failed: isfail,
		details: isfail ? { errorcode: -1, errordomain: "invalidParameter", errorinfo: failedparam } : null,
	};
}

function validatePostRequest(params, failure) {
	var url = "";
	var data = {};
	var headers = {};
	var urlkey = uniqid();

	if (params.hasOwnProperty("url")) {
		url = params.url;
	}
	if (params.hasOwnProperty("data")) {
		data = params.data || {};
	}
	if (params.hasOwnProperty("headers")) {
		headers = params.headers || {};
	}
	if (params.id) {
		urlkey = params.id;
	}

	var is_json = false;
	if (headers["Content-Type"] && headers["Content-Type"] == "application/json" && typeof data == "string") {
		is_json = true;
		try {
			data = JSON.parse(data);
		} catch (e) {}
	}
	if (!is_json && typeof data == "object") is_json = true;
	// do not validate a form post data
	var validate = validateParams({ url: url, headers: headers, data: is_json ? data : {} });
	if (validate.failed) {
		failure(validate.details);
		return null;
	}

	return { key: urlkey, data: data, headers: headers, url: url };
}

var http = {
	sslPinning: true,
	allCertificates: false,
	domainValidation: true,
	enableSSLPinning: function (enable, success, failure) {
		return exec(
			function () {
				if (success) success();
				http.sslPinning = enable;
			},
			function () {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"enableSSLPinning",
			[enable],
		);
	},
	acceptAllCerts: function (allow, success, failure) {
		return exec(
			function () {
				if (success) success();
				http.allCertificates = allow;
			},
			function () {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"acceptAllCerts",
			[allow],
		);
	},
	validateDomainName: function (validate, success, failure) {
		return exec(
			function () {
				if (success) success();
				http.domainValidation = validate;
			},
			function () {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"validateDomainName",
			[validate],
		);
	},
	getCookies: function (params, success, failure) {
		var domain = typeof params == "string" ? params || "all" : "all";
		return exec(success, failure, "CordovaPluginSslSupport", "getCookies", [domain]);
	},
	post: function (params, success, failure) {
		var options = validatePostRequest(params, failure);
		if (!options) return;

		currentUrlId = options.key;

		return exec(
			function (response) {
				if (success) success(response);
			},
			function (request) {
				if (failure) failure(request);
			},
			"CordovaPluginSslSupport",
			"post",
			[options.url, options.data, options.headers, options.key],
		);
	},
	put: function (params, success, failure) {
		var options = validatePostRequest(params, failure);
		if (!options) return;

		currentUrlId = options.key;

		return exec(
			function (response) {
				if (success) success(response);
			},
			function (request) {
				if (failure) failure(request);
			},
			"CordovaPluginSslSupport",
			"put",
			[options.url, options.data, options.headers, options.key],
		);
	},
	delete: function (params, success, failure) {
		var options = validatePostRequest(params, failure);
		if (!options) return;

		currentUrlId = options.key;

		return exec(
			function (response) {
				if (success) success(response);
			},
			function (request) {
				if (failure) failure(request);
			},
			"CordovaPluginSslSupport",
			"delete",
			[options.url, options.data, options.headers, options.key],
		);
	},
	get: function (params, success, failure) {
		var url = "";
		var data = {};
		var headers = {};
		var urlkey = uniqid(),
			failedparam;

		if (params.hasOwnProperty("url")) {
			url = params.url;
		}
		if (params.hasOwnProperty("data")) {
			data = params.data || {};
		}
		if (params.hasOwnProperty("headers")) {
			headers = params.headers || {};
		}
		if (params.id) {
			urlkey = params.id;
		}

		var validate = validateParams({ url: url, headers: headers, data: data });
		if (validate.failed) {
			failure(validate.details);
			return;
		}

		currentUrlId = urlkey;

		return exec(
			function (response) {
				if (success) success(response);
			},
			function (request) {
				if (failure) failure(request);
			},
			"CordovaPluginSslSupport",
			"get",
			[url, data, headers, urlkey],
		);
	},

	download: function (params, success, failure) {
		var urlkey = uniqid(),
			url = "",
			headers = {};

		if (params.id) urlkey = params.id;
		if (params.url) url = params.url;
		if (params.headers) headers = params.headers;

		var dest = null;
		if (params.dest) dest = params.dest;

		currentUrlId = urlkey;

		exec(
			function (response) {
				if (success) success(response);
			},
			function (request) {
				if (failure) failure(request);
			},
			"CordovaPluginSslSupport",
			"download",
			[url, dest, headers, urlkey],
		);
	},

	cancelRequest: function (urlkey, success, failure) {
		if (!urlkey) {
			urlkey = currentUrlId;
		}
		if (!urlkey) {
			if (success) success();
			return;
		}

		return exec(
			function () {
				if (success) success();
			},
			function () {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"cancelRequest",
			[urlkey],
		);
	},
};

exec(
	function () {},
	function () {},
	"CordovaPluginSslSupport",
	"setUserAgent",
	[navigator.userAgent],
);

module.exports = http;

window["sslHTTP"] = http;
