#!/usr/bin/env node

const fs = require('fs');
const { exec } = require('child_process');

const fname = "platforms/ios/Pods/AFNetworking/AFNetworking/AFURLSessionManager.m";

function output(error, stdout, stderr) {
    if (error == null) {
        console.log("patching process successful! :)");
        process.exit(0);
    } else {
        console.error("patching process failed :(");
        console.error("error", error);
        console.error("stdout", stdout);
        console.error("stderr", stderr);
        process.exit(1);
    }
}

if (fs.existsSync(fname)) {
    console.log("patching file:", fname);
    exec(`sed -i '' 's/NSURLSessionAuthChallengeCancelAuthenticationChallenge/NSURLSessionAuthChallengeRejectProtectionSpace/g' '${fname}'`, output);
} else {
    console.log("no file to patch");
    process.exit(0);
}
