//@ts-check
//@ts-ignore
/// <reference path="cordova.d.ts" />
const exec = require("cordova/exec");

function uniqid(prefix = "", more_entropy) {
	const formatSeed = (seed, reqWidth) => {
		seed = parseInt(seed, 10).toString(16);
		if (reqWidth < seed.length) {
			return seed.slice(seed.length - reqWidth);
		}
		return seed.padStart(reqWidth, "0");
	};

	if (!globalThis.php_js) {
		globalThis.php_js = {};
	}
	if (!globalThis.php_js.uniqidSeed) {
		globalThis.php_js.uniqidSeed = Math.floor(Math.random() * 0x75bcd15);
	}
	globalThis.php_js.uniqidSeed++;

	let retId = prefix;
	retId += formatSeed(Math.floor(Date.now() / 1000), 8);
	retId += formatSeed(globalThis.php_js.uniqidSeed, 5);

	if (more_entropy) {
		retId += (Math.random() * 10).toFixed(8);
	}
	return retId;
}

let currentUrlId;

const validateParams = params => {
	let isfail = 0;
	let failedparam;
	if (typeof params.url !== "string") {
		isfail = 1;
		failedparam = params.url;
	} else if (typeof params.data !== "object") {
		isfail = 1;
		failedparam = params.data;
	} else if (typeof params.headers !== "object") {
		isfail = 1;
		failedparam = params.headers;
	}

	return {
		failed: isfail,
		details: isfail ? { errorcode: -1, errordomain: "invalidParameter", errorinfo: failedparam } : null,
	};
};

const validatePostRequest = (params, failure) => {
	let url = params.url || "";
	let data = params.data || {};
	let headers = params.headers || {};
	let urlkey = params.id || uniqid();

	let is_json = headers["Content-Type"] === "application/json" && typeof data === "string";
	if (is_json) {
		try {
			data = JSON.parse(data);
		} catch {}
	}
	if (!is_json && typeof data === "object") is_json = true;

	const validation = validateParams({ url, headers, data: is_json ? data : {} });
	if (validation.failed) {
		failure(validation.details);
		return null;
	}

	return { key: urlkey, data, headers, url };
};

const http = {
	sslPinning: true,
	allCertificates: false,
	domainValidation: true,

	enableSSLPinning(enable, success, failure) {
		return exec(
			() => {
				if (success) success();
				http.sslPinning = enable;
			},
			() => {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"enableSSLPinning",
			[enable],
		);
	},

	acceptAllCerts(allow, success, failure) {
		return exec(
			() => {
				if (success) success();
				http.allCertificates = allow;
			},
			() => {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"acceptAllCerts",
			[allow],
		);
	},

	validateDomainName(validate, success, failure) {
		return exec(
			() => {
				if (success) success();
				http.domainValidation = validate;
			},
			() => {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"validateDomainName",
			[validate],
		);
	},

	getCookies(params, success, failure) {
		const domain = typeof params === "string" ? params : "all";
		return exec(success, failure, "CordovaPluginSslSupport", "getCookies", [domain]);
	},

	post(params, success, failure) {
		const options = validatePostRequest(params, failure);
		if (!options) return;
		currentUrlId = options.key;
		return exec(success, failure, "CordovaPluginSslSupport", "post", [options.url, options.data, options.headers, options.key]);
	},

	put(params, success, failure) {
		const options = validatePostRequest(params, failure);
		if (!options) return;
		currentUrlId = options.key;
		return exec(success, failure, "CordovaPluginSslSupport", "put", [options.url, options.data, options.headers, options.key]);
	},

	delete(params, success, failure) {
		const options = validatePostRequest(params, failure);
		if (!options) return;
		currentUrlId = options.key;
		return exec(success, failure, "CordovaPluginSslSupport", "delete", [options.url, options.data, options.headers, options.key]);
	},

	get(params, success, failure) {
		let url = params.url || "";
		let data = params.data || {};
		let headers = params.headers || {};
		let urlkey = params.id || uniqid();

		const validation = validateParams({ url, headers, data });
		if (validation.failed) {
			failure(validation.details);
			return;
		}

		currentUrlId = urlkey;
		return exec(success, failure, "CordovaPluginSslSupport", "get", [url, data, headers, urlkey]);
	},

	download(params, success, failure) {
		const urlkey = params.id || uniqid();
		const url = params.url || "";
		const headers = params.headers || {};
		const dest = params.dest || null;

		currentUrlId = urlkey;

		exec(success, failure, "CordovaPluginSslSupport", "download", [url, dest, headers, urlkey]);
	},

	upload(params, success, failure) {
		const url = params.url || "";
		const data = params.data;
		const headers = params.headers || {};
		const urlkey = params.id || uniqid();

		if (!(data instanceof FormData)) {
			failure({ errorcode: -1, errordomain: "invalidParameter", errorinfo: "data must be an instance of FormData" });
			return;
		}

		const postData = {};
		/** @type {File} */
		let file = null;
		let filename = "";

		for (const [key, value] of data.entries()) {
			if (value?.constructor?.name === "File") {
				//@ts-ignore
				file = value;
				postData["file"] = key;
				filename = file.name || "";

				if (file["localURL"]) {
					filename = file["localURL"].split("/").pop();
				}
			} else {
				postData[key] = value;
			}
		}

		if (!file) {
			failure({ errorcode: -1, errordomain: "invalidParameter", errorinfo: "File is required for upload" });
			return;
		}

		window["resolveLocalFileSystemURL"](window["cordova"]["file"].cacheDirectory, dir => {
			dir.getFile(
				`${Date.now()}_${filename}`,
				{ create: true, exclusive: false },
				fileEntry => {
					fileEntry.createWriter(
						fileWriter => {
							fileWriter.onwriteend = () => {
								const fileUri = fileEntry.nativeURL;
								exec(success, failure, "CordovaPluginSslSupport", "upload", [url, urlkey, fileUri, headers, postData]);
							};
							fileWriter.onerror = e => {
								failure({ errorcode: -1, errordomain: "invalidFile", errorinfo: e.message });
							};
							fileWriter.write(file);
						},
						e => {
							failure({ errorcode: -1, errordomain: "invalidFile", errorinfo: e.message });
						},
					);
				},
				e => {
					failure({ errorcode: -1, errordomain: "invalidFile", errorinfo: e.message });
				},
			);
		});
	},

	cancelRequest(urlkey, success, failure) {
		urlkey ||= currentUrlId;
		if (!urlkey) {
			if (success) success();
			return;
		}

		return exec(
			() => {
				if (success) success();
			},
			() => {
				if (failure) failure();
			},
			"CordovaPluginSslSupport",
			"cancelRequest",
			[urlkey],
		);
	},
};

exec(
	() => {},
	() => {},
	"CordovaPluginSslSupport",
	"setUserAgent",
	[navigator.userAgent],
);

module.exports = http;
window["sslHTTP"] = http;
