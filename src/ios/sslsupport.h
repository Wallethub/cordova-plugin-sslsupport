#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface CordovaPluginSslSupport  : CDVPlugin

- (void)enableSSLPinning:(CDVInvokedUrlCommand*)command;
- (void)acceptAllCerts:(CDVInvokedUrlCommand*)command;
- (void)validateDomainName:(CDVInvokedUrlCommand*)command;
- (void)setHeader:(CDVInvokedUrlCommand*)command;
- (void)post:(CDVInvokedUrlCommand*)command;
- (void)get:(CDVInvokedUrlCommand*)command;
- (void)download:(CDVInvokedUrlCommand*)command;
- (void)cancelRequest:(CDVInvokedUrlCommand*)command;
- (void)setUserAgent:(CDVInvokedUrlCommand*)command;

@end
