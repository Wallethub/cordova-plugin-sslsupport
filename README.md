# cordova-plugin-sslsupport

> Cordova HTTP plugin with SSL Pinning for iOS (AFnetworking) and Android (OKhttp3)

# Changes in 1.3:

-   added PUT and DELETE requests

# Breaking changes between 1.0.x and 1.1.x

-   removed setHeader method, used in iOS for setting global headers
-   for a more effective ssl pinning and flexibility of making requests that rely on system's certificates, iOS now required `target` attribute on certificate resources, similar to Android:
    previously (for iOS):

```
<resource-file src="certificates/somecertificate.cer" />
```

now (for iOS):

```
<resource-file src="certificates/somecertificate.cer" target="certificates/example.com.cer" />
```

Take not that this setting will be active for subdomains also. If you have different certificates for the subdomain, specify the subdomain as target for your certificate:

```
<resource-file src="certificates/subdomaincert.cer" target="certificates/subdomain.example.com.cer" />
```

# About

This plugin provides the ability to make http requests using native code which brings several advantages over the webview XMLHttpRequest

# Main Advantages

-   Native HTTP requests using popular libraries : [AFnetworking 3.x](https://github.com/AFNetworking/AFNetworking) (iOS) and [okhttp3](http://square.github.io/okhttp/) (Android).
-   SSL Pinning - read more at [LumberBlog](http://blog.lumberlabs.com/2012/04/why-app-developers-should-care-about.html).
-   WkWebView Support. Because the requests are handled at the native side there is no concern for CORS rules
-   Cookies support

# Installation

```
cordova plugin add cordova-plugin-sslsupport
```

# Usage

### enableSSLPinning

Enable or disable SSL pinning. This defaults to true.

```
sslHTTP.enableSSLPinning(true, function() {
    console.log('success!');
}, function() {
    console.log('error :(');
});
```

In order for pinning to work you must provide certificates and domain in `config.xml`:

```
<platform name="ios">
    <resource-file src="certificates/somecertificate.cer" target="certificates/domain.cer" />
    ....
</platform>
....
<platform name="android">
    <!-- domain pinning -->
    <resource-file src="certificates/somecertificate.cer" target="assets/certificates/example.com.cer" />
    <!-- subdomain pinning -->
    <resource-file src="certificates/subdomaincertificate.cer" target="assets/certificates/*.example.com.cer" />
    ....
</platform>
```

Where the `certificates` folder it placed on the root of your cordova project.
For Android you have to provide as the certificate name the domain name, while on iOS the certificate itself is sufficient.
We recommend using the Intermediate Certificate of the domain in order to have a longer expiration time as everytime they expire you must re-compile a new package with the new certificates.

The following screenshot shows an example of how to extract the certifcate of a domain using google chrome:

<p align="center" >
  <img src="https://d2k0escgkdw2ev.cloudfront.net/wallethub/images/wh2015/emails/certificate-extract_V9a9e807_.png">
</p>
To get the certificate simply drag it from the window on to your desktop.

### acceptAllCerts

Accept all certificates. This is usefull when its needed to allow self signed certificates. Default is false.

```
sslHTTP.acceptAllCerts(true, function() {
    console.log('success!');
}, function() {
    console.log('error :(');
});
```

### validateDomainName

Whether or not to validate the domain name in the certificate. Default is true.

```
sslHTTP.validateDomainName(true, function() {
    console.log('success!');
}, function() {
    console.log('error :(');
});
```

### getCookies

Get currently stored cookies. Internally, the plugin will store any cookie, passing it along with any request, but to javascript only cookies which do not have `httpOnly` flag will be available for reading. The plugin tries to follow the standard browser security settings when it comes to cookies.
Cookies are not shared with the webview.

```
sslHTTP.getCookies(domain, function(cookies) {
    console.log(cookies);
}, function() {
    console.log('error :(');
});
```

If you wish to get cookies from all domains pass `null` or `all` as domain value.
The success callback recevies the following object:

```
{
    cookiename : { path: <string>, value : <string>, name:<string>, domain:<string> }
}
```

### New in 1.1 - download

Perform a DOWNLOAD request

```
sslHTTP.download(params,function(response){
    console.log(response);
},function(error){
    console.log(error);
})
```

Where `params` is an object:

```
{
    ur: string,
    destination : string, // optional
    headers: {[key:string]:string}
}
```

And `response` is :

```
{ progress : number , url :string  }
```

Response will give progress as a range between 0 and 1 and will only provide value for `url` once download is completed. `url` represents the absolute path where the file has been downloaded.

### post

Perform a POST request.

```
sslHTTP.post(params,function(response){
    console.log(response);
},function(error){
    console.log(error);
})
```

Where `params` is an object:

```
{
    url : <string>,
    data: { .. },
    header: { ... }
    id : <string>
}
```

If `id` is provide it can be using in `.cancelRequest` method to cancel the request.
For the success callback the following object will be passed:

```
{
    status : <number>
    data : <string>
    header: <object>
}
```

For the failed callback the following object will be passed:

```
{
    data:<string>, // raw data that came with the response, in case of some server error
    errorcode:<number>,  // native error code
    errordomain:<string>, // ios native error domain
    errorinfo:<string>,  // native error description
    httperrorcode:<number> // http error code
}
```

In iOS `errorcode` along with `errodomain` belong to [NSURLErrorDomain](https://developer.apple.com/documentation/foundation/1508628-url_loading_system_error_codes)
For both platforms the following codes represent ssl issues:
`-1022,-1200,-1201,-1202,-1203,-1204,-1205,-1206`

### New in 1.3 put:

-   similar to post but with PUT method (sslHTTP.put(...))

### New in 1.13 delete:

-   similar to post but with DELETE method (sslHTTP.delete(...))

### get

Perform a GET request.

```
sslHTTP.get(params,function(response){
    console.log(response);
},function(error){
    console.log(error);
})
```

The `params`, `response`, `error` objects are similar to that of `.post`.

### cancelRequest

Cancel the current request or a request matching the given id.

```
sslHTTP.cancelRequest(id,function(response){
    console.log('success!');
}, function() {
    console.log('error :(');
});
```

If `id` is null it will cancel the most recent request that is still in progress.

## License

The MIT License

Copyright (c) 2017 Evolution Finance, Inc

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
