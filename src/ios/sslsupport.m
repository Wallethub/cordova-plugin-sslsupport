/********* cordova-plugin-sslsupport.m Cordova Plugin Implementation *******/
#import "sslsupport.h"
#import <Cordova/CDV.h>
#import "TextResponseSerializer.h"

@interface CordovaPluginSslSupport ()
- (void)setRequestHeaders:(NSDictionary*)headers forManager:(AFHTTPSessionManager*)manager;
@end

@implementation CordovaPluginSslSupport {
    AFHTTPRequestSerializer *requestSerializer;
    AFSecurityPolicy *securityPolicy;
    NSMutableArray *arrayOfTasks;
    NSMutableDictionary *taskDictionary;

    AFHTTPSessionManager *manager;
    UIWebView *sampleWebView;
    NSString *UserAgent;
}

- (void)pluginInitialize {
    requestSerializer = [AFHTTPRequestSerializer serializer];
    securityPolicy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeNone];
    securityPolicy.allowInvalidCertificates = NO;
    manager = [AFHTTPSessionManager manager];
    manager.responseSerializer = [TextResponseSerializer serializer];
    manager.securityPolicy = securityPolicy;
    arrayOfTasks = [[NSMutableArray alloc] init];
    taskDictionary = [NSMutableDictionary dictionary];
    
    sampleWebView = [[UIWebView alloc] initWithFrame:CGRectZero];
    UserAgent = [sampleWebView stringByEvaluatingJavaScriptFromString:@"navigator.userAgent"];

    
    //    AFSecurityPolicy *policy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeCertificate];
    //    NSData *localCertificate = [NSData dataWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"my" ofType:@"cer"]];
    //    securityPolicy.pinnedCertificates = [[NSSet alloc] initWithObjects:localCertificate, nil];
}

- (void)setRequestHeaders:(NSDictionary*)headers forManager:(AFHTTPSessionManager*)manager {
    manager.requestSerializer = [AFHTTPRequestSerializer serializer];

    NSString *contentType = [headers objectForKey:@"Content-Type"];
    if([contentType rangeOfString:@"json"].location != NSNotFound) {  //application/json
        manager.requestSerializer = [AFJSONRequestSerializer serializer];
        NSLog(@"%@", @"YES THIS IS JSON");
    } else {
        manager.requestSerializer = [AFHTTPRequestSerializer serializer];
        NSLog(@"%@", @"YES THIS IS FORM POST");
    }
    
    [manager.requestSerializer setValue:UserAgent forHTTPHeaderField:@"User-Agent"];
    
    [manager.requestSerializer.HTTPRequestHeaders enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        NSLog(@"%@ = %@", key, obj);
        if([obj isKindOfClass:[NSString class]])
        {
            [manager.requestSerializer setValue:obj forHTTPHeaderField:key];    
        }
        
    }];
    [headers enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        NSLog(@"%@ = %@", key, obj);
         if([obj isKindOfClass:[NSString class]])
        {
            [manager.requestSerializer setValue:obj forHTTPHeaderField:key];
        }
    }];
    
    
}




- (void)setHeader:(CDVInvokedUrlCommand*)command {
    NSString *header = [command.arguments objectAtIndex:0];
    NSString *value = [command.arguments objectAtIndex:1];
    [requestSerializer setValue:value forHTTPHeaderField: header];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(NSString*) getErrStr: (int) errcode {
    if (errcode == 1) {
        return @"kCFHostErrorHostNotFound";
    }
    else if(errcode == 2){
        // Query the kCFGetAddrInfoFailureKey to get the value returned from getaddrinfo; lookup in netdb.h
        return @"kCFHostErrorUnknown";
    }
    // SOCKS errors; in all cases you may query kCFSOCKSStatusCodeKey to recover the status code returned by the server
    else if (errcode == 100) {
        return @"kCFSOCKSErrorUnknownClientVersion";
    }
    else if(errcode == 101){
        // Query the kCFSOCKSVersionKey to find the version requested by the server
        return @"  kCFSOCKSErrorUnsupportedServerVersion";
    }
    else if (errcode == 110) {
        // request rejected or failed by the server
        return @" kCFSOCKS4ErrorRequestFailed";
    }
    else if(errcode == 111){
        // request rejected because SOCKS server cannot connect to identd on the client
        return @"kCFSOCKS4ErrorIdentdFailed";
    }
    else if (errcode == 112) {
        // request rejected because the client program and identd report different user-ids
        return @"kCFSOCKS4ErrorIdConflict";
    }
    else if(errcode == 113){
        return @"kCFSOCKS4ErrorUnknownStatusCode";
    }
    // SOCKS5-specific errors
    else if (errcode == 120) {
        return @"kCFSOCKS5ErrorBadState";
    }
    else if (errcode == 121) {
        return @"kCFSOCKS5ErrorBadResponseAddr ";
    }
    else if (errcode == 122) {
        return @"kCFSOCKS5ErrorBadCredentials";
    }
    else if (errcode == 123) {
        // query kCFSOCKSNegotiationMethodKey to find the method requested
        return @"kCFSOCKS5ErrorUnsupportedNegotiationMethod";
    }
    else if (errcode == 124) {
        return @"kCFSOCKS5ErrorNoAcceptableMethod";
    }
    else if (errcode == 200) {
        // FTP errors; query the kCFFTPStatusCodeKey to get the status code returned by the server
        return @"kCFFTPErrorUnexpectedStatusCode";
    }
    // HTTP errors
    else if(errcode == 300) {
        return @"kCFErrorHTTPAuthenticationTypeUnsupported";
    }
    else if(errcode == 301) {
        return @"kCFErrorHTTPBadCredentials";
    }
    else if(errcode == 302) {
        return @"kCFErrorHTTPConnectionLost";
    }
    else if(errcode == 303) {
        return @"kCFErrorHTTPParseFailure";
    }
    else if(errcode == 304) {
        return @"kCFErrorHTTPRedirectionLoopDetected";
    }
    else if(errcode == 305) {
        return @"kCFErrorHTTPBadURL";
    }
    else if(errcode == 306) {
        return @"kCFErrorHTTPProxyConnectionFailure";
    }
    else if(errcode == 307) {
        return @"kCFErrorHTTPBadProxyCredentials";
    }
    else if(errcode == 308) {
        return @"kCFErrorPACFileError";
    }
    else if(errcode == 309) {
        return @"kCFErrorPACFileAuth";
    }
    else if(errcode == 310) {
        return @"kCFErrorHTTPSProxyConnectionFailure";
    }
    // Error codes for CFURLConnection and CFURLProtocol
    else if(errcode == -998) {
        return @"kCFURLErrorUnknown";
    }
    else if(errcode == -999) {
        return @"kCFURLErrorCancelled";
    }
    else if(errcode == -1000) {
        return @"kCFURLErrorBadURL";
    }
    else if(errcode == -1001) {
        return @"kCFURLErrorTimedOut";
    }
    else if(errcode == -1002) {
        return @"kCFURLErrorUnsupportedURL";
    }
    else if(errcode == -1003) {
        return @"kCFURLErrorCannotFindHost";
    }
    else if(errcode == -1004) {
        return @"kCFURLErrorCannotConnectToHost";
    }
    else if(errcode == -1005) {
        return @"kCFURLErrorNetworkConnectionLost";
    }
    else if(errcode == -1006) {
        return @"kCFURLErrorDNSLookupFailed";
    }
    else if(errcode == -1007) {
        return @"kCFURLErrorHTTPTooManyRedirects";
    }
    else if(errcode == -1008) {
        return @"kCFURLErrorResourceUnavailable";
    }
    else if(errcode == -1009) {
        return @"kCFURLErrorNotConnectedToInternet";
    }
    else if(errcode == -1010) {
        return @"kCFURLErrorRedirectToNonExistentLocation";
    }
    else if(errcode == -1011) {
        return @"kCFURLErrorBadServerResponse";
    }
    else if(errcode == -1012) {
        return @"kCFURLErrorUserCancelledAuthentication";
    }
    else if(errcode == -1013) {
        return @"kCFURLErrorUserAuthenticationRequired";
    }
    else if(errcode == -1014) {
        return @"kCFURLErrorZeroByteResource";
    }
    else if(errcode == -1015) {
        return @"kCFURLErrorCannotDecodeRawData";
    }
    else if(errcode == -1016) {
        return @"kCFURLErrorCannotDecodeContentData";
    }
    else if(errcode == -1017) {
        return @"kCFURLErrorCannotParseResponse";
    }
    else if(errcode == -1018) {
        return @"kCFURLErrorInternationalRoamingOff";
    }
    else if(errcode == -1019) {
        return @"kCFURLErrorCallIsActive";
    }
    else if(errcode == -1020) {
        return @"kCFURLErrorDataNotAllowed";
    }
    else if(errcode == -1021) {
        return @"kCFURLErrorRequestBodyStreamExhausted";
    }
    else if(errcode == -1100) {
        return @"kCFURLErrorFileDoesNotExist";
    }
    else if(errcode == -1101) {
        return @"kCFURLErrorFileIsDirectory";
    }
    else if(errcode == -1102) {
        return @"kCFURLErrorNoPermissionsToReadFile";
    }
    else if(errcode == -1103) {
        return @"kCFURLErrorDataLengthExceedsMaximum";
    }
    // SSL errors
    else if(errcode == -1200) {
        return @"kCFURLErrorSecureConnectionFailed";
    }
    else if(errcode == -1201) {
        return @"kCFURLErrorServerCertificateHasBadDate";
    }
    else if(errcode == -1202) {
        return @"kCFURLErrorServerCertificateUntrusted";
    }
    else if(errcode == -1203) {
        return @"kCFURLErrorServerCertificateHasUnknownRoot";
    }
    else if(errcode == -1204) {
        return @"kCFURLErrorServerCertificateNotYetValid";
    }
    else if(errcode == -1205) {
        return @"kCFURLErrorClientCertificateRejected";
    }
    else if(errcode == -1206) {
        return @"kCFURLErrorClientCertificateRequired";
    }
    else if(errcode == -2000) {
        return @"kCFURLErrorCannotLoadFromNetwork";
    }
    else if(errcode == -3000) {
        return @"kCFURLErrorCannotCreateFile";
    }
    else if(errcode == -3001) {
        return @"kCFURLErrorCannotOpenFile";
    }
    else if(errcode == -3002) {
        return @"kCFURLErrorCannotCloseFile";
    }
    else if(errcode == -3003) {
        return @"kCFURLErrorCannotWriteToFile";
    }
    else if(errcode == -3004) {
        return @"kCFURLErrorCannotRemoveFile";
    }
    else if(errcode == -3005) {
        return @"kCFURLErrorCannotMoveFile";
    }
    else if(errcode == -3006) {
        return @"kCFURLErrorDownloadDecodingFailedMidStream";
    }
    else if(errcode == -3007) {
        return @"kCFURLErrorDownloadDecodingFailedToComplete";
    }
    // Cookie errors
    else if(errcode == -4000) {
        return @"kCFHTTPCookieCannotParseCookieFile";
    }
    // Errors originating from CFNetServices
    else if(errcode == -72000L) {
        return @"kCFNetServiceErrorUnknown";
    }
    else if(errcode == -72001L) {
        return @"kCFNetServiceErrorCollision";
    }
    else if(errcode == -72002L) {
        return @"kCFNetServiceErrorNotFound";
    }
    else if(errcode == -72003L) {
        return @"kCFNetServiceErrorInProgress";
    }
    else if(errcode == -72004L) {
        return @"kCFNetServiceErrorBadArgument";
    }
    else if(errcode == -72005L) {
        return @"kCFNetServiceErrorCancel";
    }
    else if(errcode == -72006L) {
        return @"kCFNetServiceErrorInvalid";
    }
    else if(errcode == -72007L) {
        return @"kCFNetServiceErrorTimeout";
    }
    else if(errcode == -73000L) {
        // An error from DNS discovery; look at kCFDNSServiceFailureKey to get the error number and interpret using dns_sd.h
        return @"kCFNetServiceErrorDNSServiceFailure";
    }
    else{
        return @"NSURLErrorDomain";  //default
    }
}

- (void)enableSSLPinning:(CDVInvokedUrlCommand*)command {
    bool enable = [[command.arguments objectAtIndex:0] boolValue];
    if (enable) {
        securityPolicy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeCertificate];
        securityPolicy.allowInvalidCertificates = YES;
        securityPolicy.validatesDomainName = NO;
        
        //securityPolicy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModePublicKey];
//        NSData *localCertificate = [NSData dataWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"" ofType:@"cer" inDirectory:@"www/certificates"]];
//        securityPolicy.pinnedCertificates = [[NSSet alloc] initWithObjects:localCertificate, nil];
    } else {
        securityPolicy = [AFSecurityPolicy policyWithPinningMode:AFSSLPinningModeNone];
    }
    
    NSString *certPathName = [[NSBundle mainBundle] bundlePath];
//    NSString *certPathName = [appPathName stringByAppendingPathComponent:@"www/certificates"];
    NSError * error;
    NSArray * directoryContents = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:certPathName error:&error];
    NSLog(@"%@", certPathName);
    
    if(error != nil){
        NSLog(@"Error Localized desc: %@", error.localizedDescription);
        NSLog(@"Error domain: %@", [error domain]);
        NSLog(@"Error code: %ld", [error code]);
        //        NSLog(@"Error: %@", error);
    }else{
        NSLog(@"%@", directoryContents);
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)acceptAllCerts:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    bool allow = [[command.arguments objectAtIndex:0] boolValue];
    
    securityPolicy.allowInvalidCertificates = allow;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)validateDomainName:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    bool validate = [[command.arguments objectAtIndex:0] boolValue];
    
    securityPolicy.validatesDomainName = validate;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)getCookies:(CDVInvokedUrlCommand*)command {
    //    NSHTTPCookieStorage *cookies = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    NSString *domainName = @"all";
    if ([command.arguments count] > 0) {
        if ([[command.arguments objectAtIndex:0] isEqual:[NSNull null]]){
            NSLog(@"myString IS NULL!");
        }
        else{
            if ([[command.arguments objectAtIndex:0] isEqualToString:@""]) {
                NSLog(@"myString IS empty!");
            } else {
                NSLog(@"myString IS NOT empty, it is: %@", [command.arguments objectAtIndex:0]);
                domainName = [command.arguments objectAtIndex:0];
            }
            //domainName = [command.arguments objectAtIndex:0];
        }
    }
    //NSNumber *addthiscookie = [NSNumber numberWithInt:1];
    Boolean addthiscookie = true;
    
    NSMutableDictionary * responseCookies = [NSMutableDictionary dictionary];
    NSHTTPCookie *cookie;
    NSHTTPCookieStorage *cookieJar = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (cookie in [cookieJar cookies]) {
        
        if([domainName isEqualToString:@"all"])
        {
            //addthiscookie = [NSNumber numberWithInt:1];
            addthiscookie = true;
        }
        else{
            if([[cookie domain] rangeOfString:domainName].location != NSNotFound) {
                NSLog(@"cookie has the desired domain:%@", cookie);
               addthiscookie = true;
            }
            else{
                addthiscookie = false;
            }
        }
        //NSLog(@"%@", cookie);
        if(!cookie.isHTTPOnly && addthiscookie)
        {
        NSMutableDictionary *cookieProperties = [NSMutableDictionary dictionary];
        [cookieProperties setObject:cookie.name forKey:[NSHTTPCookieName lowercaseString]];
        [cookieProperties setObject:cookie.value forKey:[NSHTTPCookieValue lowercaseString]];
        [cookieProperties setObject:cookie.domain forKey:[NSHTTPCookieDomain lowercaseString]];
        [cookieProperties setObject:cookie.path forKey:[NSHTTPCookiePath lowercaseString]];
        [cookieProperties setObject:[NSNumber numberWithInt:cookie.version] forKey:[NSHTTPCookieVersion lowercaseString]];
        [responseCookies setObject:cookieProperties forKey:cookie.name];
        }
    }
    // cookie code ends
    NSLog(@"%@", responseCookies);
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:responseCookies];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)post:(CDVInvokedUrlCommand*)command {
    AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
    manager.responseSerializer = [TextResponseSerializer serializer];
    manager.securityPolicy = securityPolicy;
    
    NSString *URL = [command.arguments objectAtIndex:0];
    NSDictionary *parameters = [command.arguments objectAtIndex:1];
    NSDictionary *headers = [command.arguments objectAtIndex:2];
    NSString *URLkey = [command.arguments objectAtIndex:3];
    [self setRequestHeaders: headers forManager: manager];
    
    CordovaPluginSslSupport* __weak weakSelf = self;

      if ([taskDictionary objectForKey:URLkey]) {
        // key exists.
        NSLog(@"ArrCancelled: %@", URLkey);
        [[taskDictionary objectForKey:URLkey] cancel];
        [taskDictionary removeObjectForKey:URLkey];
    }
    else
    {
        // ...
    }
    
    NSURLSessionDataTask *task = [manager POST:URL parameters:parameters progress:nil success:^(NSURLSessionTask *operation, id responseObject) {
        //NSLog(@"JSON: %@", responseObject);
        //NSLog(@"%@", operation.response);
        NSHTTPURLResponse *response = (NSHTTPURLResponse *) [operation response];

        NSArray *cookies = [NSHTTPCookie cookiesWithResponseHeaderFields:[response allHeaderFields] forURL:[response URL]];
        [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookies forURL:[response URL] mainDocumentURL:nil];
        
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        [dictionary setObject:[NSNumber numberWithInt:response.statusCode] forKey:@"status"];
        
        if(responseObject != nil){
            [dictionary setObject:responseObject forKey:@"data"];
        }
        [dictionary setObject:[response allHeaderFields] forKey:@"headers"];
        
        //NSLog(@"%@", dictionary);
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
        
    } failure:^(NSURLSessionTask *operation, NSError *error) {
        NSLog(@"Error Localized desc: %@", error.localizedDescription);
        NSLog(@"Error domain: %@", [error domain]);
        NSLog(@"Error code: %ld", [error code]);
        NSLog(@"Error: %@", error);
        NSHTTPURLResponse *response = error.userInfo[AFNetworkingOperationFailingURLResponseErrorKey];
        NSLog(@"Error Response: %@", response);
        
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        [dictionary setObject:[error domain] forKey:@"errordomain"];
        [dictionary setObject:[NSNumber numberWithInt:[error code]] forKey:@"errorcode"];
        
        //for no internet
        if([error code] == -1009){
            [dictionary setObject:[NSNumber numberWithInt:-10] forKey:@"errorcode"];
        }
        
        if(response == nil){
            [dictionary setObject:[NSNumber numberWithInt:0] forKey:@"httperrorcode"];
            [dictionary setObject:@"" forKey:@"data"];
            // [dictionary setObject:nil forKey:@"headers"];
        }
        else{
            NSString* ErrorResponse = [[NSString alloc] initWithData:(NSData *)error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey] encoding:NSUTF8StringEncoding];
            NSLog(@"%@",ErrorResponse);
            
            [dictionary setObject:ErrorResponse forKey:@"data"];
            [dictionary setObject:[NSNumber numberWithInt:response.statusCode] forKey:@"httperrorcode"];
            [dictionary setObject:[response allHeaderFields] forKey:@"headers"];
        }
        [dictionary setObject:error.localizedDescription forKey:@"errorinfo"];
        
        [dictionary setObject:[self getErrStr:error.code] forKey:@"errordomain"];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:dictionary];
        [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
    }];
    
      // add the task to our arrayOfTasks
//    [arrayOfTasks addObject:task];
    [taskDictionary setObject:task forKey:URLkey];

}


- (void)get:(CDVInvokedUrlCommand*)command {
//    AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
//    manager.responseSerializer = [TextResponseSerializer serializer];
//    manager.securityPolicy = securityPolicy;
    
    NSString *URL = [command.arguments objectAtIndex:0];
    NSDictionary *parameters = [command.arguments objectAtIndex:1];
    NSDictionary *headers = [command.arguments objectAtIndex:2];
    NSString *URLkey = [command.arguments objectAtIndex:3];
    [self setRequestHeaders: headers forManager: manager];
    
    CordovaPluginSslSupport* __weak weakSelf = self;
    
//    NSLog(@"ARRlength: %@", [arrayOfTasks count]);
    
    if ([taskDictionary objectForKey:URLkey]) {
        // key exists.
        NSLog(@"ArrCancelled: %@", URLkey);
        [[taskDictionary objectForKey:URLkey] cancel];
        [taskDictionary removeObjectForKey:URLkey];
    }
    else
    {
        // ...
    }

    
    NSURLSessionDataTask *task = [manager GET:URL parameters:parameters progress:nil success:^(NSURLSessionTask *operation, id responseObject) {
//        NSLog(@"JSON: %@", responseObject);
//        NSLog(@"%@", operation.response);
        NSHTTPURLResponse *response = (NSHTTPURLResponse *) [operation response];
        
        NSArray *cookies = [NSHTTPCookie cookiesWithResponseHeaderFields:[response allHeaderFields] forURL:[response URL]];
        [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookies forURL:[response URL] mainDocumentURL:nil];
        
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        [dictionary setObject:[NSNumber numberWithInt:response.statusCode] forKey:@"status"];
        
        if(responseObject != nil){
            [dictionary setObject:responseObject forKey:@"data"];
        }
        [dictionary setObject:[response allHeaderFields] forKey:@"headers"];
        
//        NSLog(@"%@", dictionary);
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
    } failure:^(NSURLSessionTask *operation, NSError *error) {
        NSLog(@"Error Localized desc: %@", error.localizedDescription);
        NSLog(@"Error code: %ld", [error code]);
        NSHTTPURLResponse *response = error.userInfo[AFNetworkingOperationFailingURLResponseErrorKey];
        NSLog(@"Error Resp: %@", [self getErrStr:error.code]);
  
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        [dictionary setObject:[error domain] forKey:@"errordomain"];
        [dictionary setObject:[NSNumber numberWithInt:[error code]] forKey:@"errorcode"];
        //for no internet
        if([error code] == -1009){
            [dictionary setObject:[NSNumber numberWithInt:-10] forKey:@"errorcode"];
        }
        
        if(response == nil){
            [dictionary setObject:[NSNumber numberWithInt:0] forKey:@"httperrorcode"];
            [dictionary setObject:@"" forKey:@"data"];
        }
        else{
            NSString* ErrorResponse = [[NSString alloc] initWithData:(NSData *)error.userInfo[AFNetworkingOperationFailingURLResponseDataErrorKey] encoding:NSUTF8StringEncoding];
//            NSLog(@"%@",ErrorResponse);
            
            [dictionary setObject:ErrorResponse forKey:@"data"];
            [dictionary setObject:[NSNumber numberWithInt:response.statusCode] forKey:@"httperrorcode"];
            [dictionary setObject:[response allHeaderFields] forKey:@"headers"];
        }
        [dictionary setObject:error.localizedDescription forKey:@"errorinfo"];
        
        [dictionary setObject:[self getErrStr:error.code] forKey:@"errordomain"];
        
        
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:dictionary];
        [weakSelf.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
    }];
    
    // add the task to our arrayOfTasks
//    [arrayOfTasks addObject:task];
    [taskDictionary setObject:task forKey:URLkey];
    
    
    
}


- (void)cancelRequest:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    NSString *URLkey = [command.arguments objectAtIndex:0];
    
    if ([taskDictionary objectForKey:URLkey]) {
        // key exists.
        NSLog(@"ArrCancelledHERE: %@", URLkey);
        [[taskDictionary objectForKey:URLkey] cancel];
        [taskDictionary removeObjectForKey:URLkey];
    }
    else
    {
        NSLog(@"NoArr Found for: %@", URLkey);
    }
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


@end

