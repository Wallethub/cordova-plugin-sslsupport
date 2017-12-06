#!/usr/bin/env node

console.log("=======================AfterPluginAdd======");


var fs = require('fs');
var path = require('path');

var exec = require('child_process').exec;

var fname = "platforms/ios/Pods/AFNetworking/AFNetworking/AFURLSessionManager.m";

function output(error, stdout, stderr) {
    if (error == null) {
        console.log("patching process successful! :)");
    }
    else {
        console.log("patching process failed :(");
        console.log("error", error);
        console.log("stdout", stdout);
        console.log("stderr", stderr);
    }
}
if (fs.existsSync(fname)) {
    console.log("patching file:", fname);
    exec("sed -i '' 's/NSURLSessionAuthChallengeCancelAuthenticationChallenge/NSURLSessionAuthChallengeRejectProtectionSpace/g' '" + fname + "'", output);

} else {
    console.log("no file to patch");
}

return;

//the code below is not executed, may be we can use for windows machine, but anyways this code will run only on a mac machine for ios version

var rootdir = process.argv[2]; //pugins
var rootdir = '';
console.log("rootdir:", rootdir);

function replace_string_in_file(filename, to_replace, replace_with) {
    var data = fs.readFileSync(filename, 'utf8');

    var result = data.replace(new RegExp(to_replace, "g"), replace_with);
    fs.writeFileSync(filename, result, 'utf8');
}



var filestoreplace = [
    "platforms/ios/Pods/AFNetworking/AFNetworking/AFURLSessionManager.m",
];
filestoreplace.forEach(function (val, index, array) {
    var fullfilename = path.join(rootdir, val);
    console.log("modifying:", fullfilename);
    if (fs.existsSync(fullfilename)) {
        replace_string_in_file(fullfilename,
            "NSURLSessionAuthChallengeCancelAuthenticationChallenge",
            "NSURLSessionAuthChallengeRejectProtectionSpace");
    } else {
        console.error("missing: " + fullfilename);
    }
});

// }